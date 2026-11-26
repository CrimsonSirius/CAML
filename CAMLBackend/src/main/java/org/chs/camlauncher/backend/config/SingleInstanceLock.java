package org.chs.camlauncher.backend.config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/** 启动器单实例锁；第二个实例会立即得到空结果。 */
public final class SingleInstanceLock implements AutoCloseable {
    private static final String LOCK_FILE = ".instance.lock";
    private static final Map<Path, Semaphore> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Semaphore processLock;
    private final FileChannel channel;
    private final FileLock fileLock;
    private boolean closed;

    private SingleInstanceLock(Semaphore processLock, FileChannel channel, FileLock fileLock) {
        this.processLock = processLock;
        this.channel = channel;
        this.fileLock = fileLock;
    }

    public static Optional<SingleInstanceLock> tryAcquire(Path dataDirectory) throws IOException {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Path root = dataDirectory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Semaphore processLock = PROCESS_LOCKS.computeIfAbsent(root, ignored -> new Semaphore(1));
        if (!processLock.tryAcquire()) {
            return Optional.empty();
        }

        FileChannel channel = null;
        try {
            channel = FileChannel.open(
                    root.resolve(LOCK_FILE),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                processLock.release();
                return Optional.empty();
            }
            return Optional.of(new SingleInstanceLock(processLock, channel, lock));
        } catch (IOException | RuntimeException e) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            processLock.release();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            fileLock.release();
        } catch (IOException e) {
            failure = e;
        }
        try {
            channel.close();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        } finally {
            processLock.release();
        }
        if (failure != null) {
            throw failure;
        }
    }
}
