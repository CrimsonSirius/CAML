package org.chs.camlauncher.backend.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/** UI 日志的 latest.log：每次关闭保留，发生错误时额外保存压缩历史。 */
public final class LatestLogService implements AutoCloseable {
    private static final DateTimeFormatter ARCHIVE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    private final Path logFile;
    private final Object lock = new Object();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-log-writer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean started;
    private volatile boolean closed;
    private volatile boolean failed;
    private BufferedWriter output;

    public LatestLogService(Path logFile) {
        this.logFile = Objects.requireNonNull(logFile, "logFile");
    }

    public void start() throws IOException {
        if (started) {
            return;
        }
        Path parent = logFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        synchronized (lock) {
            output = Files.newBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            started = true;
        }
    }

    public void append(String line) {
        if (line == null || line.isEmpty() || !started || closed) {
            return;
        }
        if (containsFailure(line)) {
            failed = true;
        }
        writer.execute(() -> write(line));
    }

    public void markFailure() {
        failed = true;
    }

    private void write(String line) {
        synchronized (lock) {
            if (output == null) {
                return;
            }
            try {
                output.write(line);
                output.newLine();
                output.flush();
            } catch (IOException ignored) {
                // 日志写盘失败不能影响启动器主流程。
            }
        }
    }

    private static boolean containsFailure(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return line.contains("[FATAL]")
                || line.contains("[ERR]")
                || line.contains("启动失败")
                || line.contains("下载失败")
                || line.contains("安装失败")
                || line.contains("游戏崩溃")
                || line.contains("异常退出")
                || lower.contains("exception");
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        writer.execute(() -> {
            synchronized (lock) {
                if (output != null) {
                    try {
                        output.flush();
                        output.close();
                    } catch (IOException ignored) {
                        // 关闭阶段的日志错误不再向上抛出。
                    }
                    output = null;
                }
            }
            if (failed) {
                persistCompressedCopy();
            }
        });
        writer.shutdown();
        try {
            if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        }
    }

    public Path logFile() {
        return logFile;
    }

    private void persistCompressedCopy() {
        if (!Files.isRegularFile(logFile)) {
            return;
        }
        Path parent = logFile.toAbsolutePath().getParent();
        String base = "latest-" + LocalDateTime.now().format(ARCHIVE_TIME);
        Path target = parent.resolve(base + ".log.gz");
        int index = 1;
        while (Files.exists(target)) {
            target = parent.resolve(base + "-" + index++ + ".log.gz");
        }
        try (var input = Files.newInputStream(logFile);
             var output = new GZIPOutputStream(Files.newOutputStream(target))) {
            input.transferTo(output);
        } catch (IOException ignored) {
            // 归档失败不能影响退出。
        }
    }
}
