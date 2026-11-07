package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * 控制器共享的后端、I/O 线程和 UI 回调。
 *
 * <p>页面控制器只通过这里访问后端，保证所有阻塞操作都离开 JavaFX 线程，
 * 并把结果统一切回 UI 线程。</p>
 */
final class ControllerContext {
    private final CAMLBackend backend;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-io-worker");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Consumer<String> logSink;

    ControllerContext(CAMLBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    CAMLBackend backend() {
        return backend;
    }

    void setLogSink(Consumer<String> logSink) {
        this.logSink = logSink;
    }

    void sendLog(String log) {
        Consumer<String> current = logSink;
        if (current != null) {
            Platform.runLater(() -> current.accept(log));
        }
    }

    <T> CompletableFuture<T> supplyAsync(IoSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new CompletionException(e);
            }
        }, ioExecutor);
    }

    void executeIo(Runnable task) {
        try {
            ioExecutor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // 应用正在退出，不再接受新的持久化任务。
        }
    }

    /** 目录扫描统一走后端，结果始终在 JavaFX 线程回调。 */
    void inspectGameDirectory(Path directory, Consumer<GameDirectoryInfo> callback) {
        if (directory == null) {
            return;
        }

        supplyAsync(() -> backend.inspectGameDirectory(directory))
                .thenAccept(info -> Platform.runLater(() -> {
                    if (callback != null) {
                        callback.accept(info);
                    }
                }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    sendLog("[目录] 检查失败: " + cause.getMessage());
                    return null;
                });
    }

    static String messageOf(Throwable cause) {
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    void shutdown() {
        ioExecutor.shutdownNow();
    }

    @FunctionalInterface
    interface IoSupplier<T> {
        T get() throws IOException, InterruptedException;
    }
}
