package org.chs.camlauncher.backend.instance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** 同一实例的写操作互斥锁；安装、内容下载与启停共用，同一线程可重入。 */
public final class InstanceInstallLock implements AutoCloseable {
    private static final Map<String, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Integer>> HOLD_COUNTS =
            ThreadLocal.withInitial(HashMap::new);

    private final String key;
    private final ReentrantLock processLock;
    private final FileChannel channel;
    private final FileLock fileLock;
    private boolean closed;

    private InstanceInstallLock(String key, ReentrantLock processLock,
                                FileChannel channel, FileLock fileLock) {
        this.key = key;
        this.processLock = processLock;
        this.channel = channel;
        this.fileLock = fileLock;
    }

    public static InstanceInstallLock acquire(Path gameDirectory, String instanceId)
            throws IOException, InterruptedException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String instance = Objects.requireNonNull(instanceId, "instanceId").trim();
        Path lockDirectory = gameDirectory.toAbsolutePath().normalize()
                .resolve("versions").resolve(".caml-locks");
        Files.createDirectories(lockDirectory);

        Path identity = gameDirectory.toAbsolutePath().normalize().resolve(instance);
        String key = sha256(identity.toString());
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(
                key, ignored -> new ReentrantLock());
        processLock.lockInterruptibly();

        Map<String, Integer> holds = HOLD_COUNTS.get();
        int holdCount = holds.getOrDefault(key, 0);
        if (holdCount > 0) {
            holds.put(key, holdCount + 1);
            return new InstanceInstallLock(key, processLock, null, null);
        }

        FileChannel channel = null;
        try {
            channel = FileChannel.open(
                    lockDirectory.resolve(key + ".lock"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            FileLock fileLock = channel.lock();
            holds.put(key, 1);
            return new InstanceInstallLock(key, processLock, channel, fileLock);
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

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        Map<String, Integer> holds = HOLD_COUNTS.get();
        int holdCount = holds.getOrDefault(key, 1);
        if (holdCount > 1) {
            holds.put(key, holdCount - 1);
            processLock.unlock();
            return;
        }

        holds.remove(key);
        if (holds.isEmpty()) {
            HOLD_COUNTS.remove();
        }
        IOException failure = null;
        try {
            if (fileLock != null) {
                fileLock.release();
            }
        } catch (IOException e) {
            failure = e;
        }
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        } finally {
            processLock.unlock();
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
