package org.chs.camlauncher.backend.instance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * 共享 libraries 目录的事务式合并。
 *
 * <p>安装器生成的库先通过 {@link #merge(Path)} 写入目标目录；如果调用方在关闭前
 * 没有 {@link #commit()}，已替换的库会从备份恢复，新库会删除。事务同时持有
 * JVM 内锁和文件锁，避免多个安装器并发修改同一个 libraries 目录。</p>
 */
public final class SharedLibraryTransaction implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(SharedLibraryTransaction.class);
    private static final String LOCK_FILE = ".caml-library.lock";
    private static final Map<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Path librariesRoot;
    private final Path backupRoot;
    private final ReentrantLock processLock;
    private final FileChannel lockChannel;
    private final FileLock fileLock;
    private final Map<Path, Change> changes = new LinkedHashMap<>();
    private boolean committed;
    private boolean closed;

    private SharedLibraryTransaction(Path librariesRoot, ReentrantLock processLock,
                                     FileChannel lockChannel, FileLock fileLock) throws IOException {
        this.librariesRoot = librariesRoot;
        this.processLock = processLock;
        this.lockChannel = lockChannel;
        this.fileLock = fileLock;
        this.backupRoot = Files.createTempDirectory(librariesRoot, ".caml-library-backup-");
    }

    public static SharedLibraryTransaction begin(Path librariesRoot)
            throws IOException, InterruptedException {
        Objects.requireNonNull(librariesRoot, "librariesRoot");
        Path normalizedRoot = librariesRoot.toAbsolutePath().normalize();
        Files.createDirectories(normalizedRoot);

        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(
                normalizedRoot, ignored -> new ReentrantLock());
        processLock.lockInterruptibly();
        FileChannel channel = null;
        try {
            channel = FileChannel.open(
                    normalizedRoot.resolve(LOCK_FILE),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            FileLock lock = channel.lock();
            return new SharedLibraryTransaction(normalizedRoot, processLock, channel, lock);
        } catch (IOException | RuntimeException e) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            processLock.unlock();
            throw e;
        }
    }

    /** 把 stagedLibraries 中的文件合并到目标 libraries，并按原内容记录回滚信息。 */
    public void merge(Path stagedLibraries) throws IOException {
        ensureOpen();
        if (stagedLibraries == null || !Files.isDirectory(stagedLibraries)) {
            return;
        }

        Path sourceRoot = stagedLibraries.toAbsolutePath().normalize();
        List<Path> files;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            files = paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> sourceRoot.relativize(path).toString()))
                    .toList();
        }

        for (Path source : files) {
            Path relative = sourceRoot.relativize(source);
            Path destination = librariesRoot.resolve(relative).normalize();
            if (!destination.startsWith(librariesRoot)) {
                throw new IOException("安装器库路径越界: " + source);
            }
            if (destination.equals(librariesRoot.resolve(LOCK_FILE))
                    || destination.startsWith(backupRoot)) {
                throw new IOException("安装器库路径占用事务内部文件: " + source);
            }
            if (Files.isDirectory(destination)) {
                throw new IOException("安装器库目标不是文件: " + destination);
            }

            boolean exists = Files.isRegularFile(destination);
            if (exists && Files.mismatch(source, destination) == -1) {
                continue;
            }

            Change change = changes.get(destination);
            if (change == null) {
                Path backup = exists
                        ? backupRoot.resolve(relative).normalize()
                        : null;
                if (backup != null) {
                    ensureWithinBackup(backup);
                    Files.createDirectories(backup.getParent());
                    Files.copy(destination, backup, StandardCopyOption.REPLACE_EXISTING);
                }
                change = new Change(destination, backup);
                changes.put(destination, change);
            }

            Files.createDirectories(destination.getParent());
            Path temporary = Files.createTempFile(
                    destination.getParent(), ".caml-library-", ".part");
            try {
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                move(temporary, destination);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    /** 标记事务成功；实际清理和锁释放发生在 {@link #close()}。 */
    public void commit() {
        ensureOpen();
        committed = true;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        IOException failure = null;
        boolean rolledBack = false;
        if (!committed) {
            try {
                rollbackChanges();
                rolledBack = true;
            } catch (IOException e) {
                failure = e;
            }
        }
        if (committed || rolledBack) {
            try {
                deleteRecursively(backupRoot);
            } catch (IOException e) {
                LOGGER.warn("无法清理共享库事务备份: {}", backupRoot, e);
            }
        } else {
            LOGGER.warn("共享库回滚未完成，保留备份用于恢复: {}", backupRoot);
        }
        try {
            fileLock.release();
        } catch (IOException e) {
            failure = recordReleaseFailure(failure, e);
        }
        try {
            lockChannel.close();
        } catch (IOException e) {
            failure = recordReleaseFailure(failure, e);
        }
        processLock.unlock();
        if (failure != null) {
            throw failure;
        }
    }

    private IOException recordReleaseFailure(IOException failure, IOException releaseFailure) {
        if (!committed && failure == null) {
            return releaseFailure;
        }
        LOGGER.warn("释放共享库事务锁失败: {}", librariesRoot, releaseFailure);
        return failure;
    }

    private void rollbackChanges() throws IOException {
        IOException failure = null;
        List<Change> ordered = new ArrayList<>(changes.values());
        for (int index = ordered.size() - 1; index >= 0; index--) {
            Change change = ordered.get(index);
            try {
                if (change.backup() == null) {
                    Files.deleteIfExists(change.destination());
                } else {
                    Path temporary = Files.createTempFile(
                            change.destination().getParent(), ".caml-library-restore-", ".part");
                    try {
                        Files.copy(change.backup(), temporary, StandardCopyOption.REPLACE_EXISTING);
                        move(temporary, change.destination());
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("回滚共享库失败: {}", change.destination(), e);
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void ensureOpen() {
        if (closed || committed) {
            throw new IllegalStateException("共享库事务已提交或关闭");
        }
    }

    private void ensureWithinBackup(Path path) throws IOException {
        if (!path.startsWith(backupRoot)) {
            throw new IOException("共享库备份路径越界: " + path);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record Change(Path destination, Path backup) {
    }
}
