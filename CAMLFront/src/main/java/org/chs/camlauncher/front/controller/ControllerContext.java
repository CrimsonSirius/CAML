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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 控制器共享的后端、I/O 线程和 UI 回调。
 *
 * <p>页面控制器只通过这里访问后端，保证所有阻塞操作都离开 JavaFX 线程，
 * 并把结果统一切回 UI 线程。</p>
 */
final class ControllerContext {
    private final CAMLBackend backend;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-download-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService settingsExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-settings-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService checkExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-check-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService metadataExecutor = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "caml-metadata-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService javaExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-java-worker");
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
        backend.appendUiLog(log);
        Consumer<String> current = logSink;
        if (current != null) {
            Platform.runLater(() -> current.accept(log));
        }
    }

    void markFailure() {
        backend.markUiFailure();
    }

    <T> CompletableFuture<T> supplyAsync(IoSupplier<T> supplier) {
        return supplyAsync(supplier, downloadExecutor);
    }

    <T> CompletableFuture<T> supplyCheckAsync(IoSupplier<T> supplier) {
        return supplyAsync(supplier, checkExecutor);
    }

    <T> CompletableFuture<T> supplyMetadataAsync(IoSupplier<T> supplier) {
        return supplyAsync(supplier, metadataExecutor);
    }

    <T> CompletableFuture<T> supplyJavaAsync(IoSupplier<T> supplier) {
        return supplyAsync(supplier, javaExecutor);
    }

    private <T> CompletableFuture<T> supplyAsync(IoSupplier<T> supplier, ExecutorService executor) {
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
        }, executor);
    }

    void executeSettings(Runnable task) {
        try {
            settingsExecutor.execute(task);
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
        downloadExecutor.shutdownNow();
        checkExecutor.shutdownNow();
        metadataExecutor.shutdownNow();
        javaExecutor.shutdownNow();
        settingsExecutor.shutdown();
        try {
            if (!settingsExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                settingsExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            settingsExecutor.shutdownNow();
        }
    }

    @FunctionalInterface
    interface IoSupplier<T> {
        T get() throws IOException, InterruptedException;
    }
}
