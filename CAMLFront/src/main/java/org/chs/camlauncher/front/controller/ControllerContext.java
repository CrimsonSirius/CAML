package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.language.Language;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private static final Logger LOGGER = LogManager.getLogger(ControllerContext.class);

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
    private final ExecutorService launchExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-launch-worker");
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

    <T> FrontTask<T> runDownload(String operation, IoSupplier<T> supplier,
                                 Consumer<T> onSuccess, Consumer<String> onError) {
        return submit(operation, downloadExecutor, supplier, onSuccess, onError, false);
    }

    <T> FrontTask<T> runCheck(String operation, IoSupplier<T> supplier,
                              Consumer<T> onSuccess, Consumer<String> onError) {
        return submit(operation, checkExecutor, supplier, onSuccess, onError, false);
    }

    <T> FrontTask<T> runMetadata(String operation, IoSupplier<T> supplier,
                                 Consumer<T> onSuccess, Consumer<String> onError) {
        return submit(operation, metadataExecutor, supplier, onSuccess, onError, false);
    }

    <T> FrontTask<T> runJava(String operation, IoSupplier<T> supplier,
                             Consumer<T> onSuccess, Consumer<String> onError) {
        return submit(operation, javaExecutor, supplier, onSuccess, onError, false);
    }

    <T> FrontTask<T> runLaunch(String operation, IoSupplier<T> supplier,
                               Consumer<T> onSuccess, Consumer<String> onError) {
        return submit(operation, launchExecutor, supplier, onSuccess, onError, true);
    }

    private <T> FrontTask<T> submit(String operation, ExecutorService executor,
                                    IoSupplier<T> supplier, Consumer<T> onSuccess,
                                    Consumer<String> onError, boolean fatal) {
        Objects.requireNonNull(supplier, "supplier");
        CompletableFuture<T> completion = new CompletableFuture<>();
        Future<?> running = executor.submit(() -> {
            try {
                completion.complete(supplier.get());
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                completion.completeExceptionally(e);
            } catch (RuntimeException | Error e) {
                completion.completeExceptionally(e);
            }
        });
        FrontTask<T> task = new FrontTask<>(completion, running);
        completion.whenComplete((value, failure) -> {
            if (task.isCancelled()) {
                return;
            }
            if (failure != null) {
                Consumer<String> guardedError = onError == null
                        ? null
                        : message -> {
                            if (!task.isCancelled()) {
                                onError.accept(message);
                            }
                        };
                reportError(operation, failure, fatal, guardedError);
            } else if (onSuccess != null) {
                Platform.runLater(() -> {
                    if (!task.isCancelled()) {
                        onSuccess.accept(value);
                    }
                });
            }
        });
        return task;
    }

    void executeSettings(String operation, IoAction task) {
        try {
            settingsExecutor.execute(() -> {
                try {
                    task.run();
                } catch (IOException | RuntimeException e) {
                    reportError(operation, e, false, null);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // 应用正在退出，不再接受新的持久化任务。
        }
    }

    /** 目录扫描统一走后端，结果始终在 JavaFX 线程回调。 */
    FrontTask<GameDirectoryInfo> inspectGameDirectory(Path directory,
                                                      Consumer<GameDirectoryInfo> callback) {
        return inspectGameDirectory(directory, callback, null);
    }

    FrontTask<GameDirectoryInfo> inspectGameDirectory(Path directory,
                                                      Consumer<GameDirectoryInfo> callback,
                                                      Consumer<String> onError) {
        if (directory == null) {
            return null;
        }

        return runDownload(Language.tr("operation.inspectGameDirectory"),
                () -> backend.inspectGameDirectory(directory), callback, onError);
    }

    /** 统一错误动作：记录日志、写入 UI 日志，并把可读消息切回 JavaFX 线程。 */
    void reportError(String operation, Throwable cause, Consumer<String> onError) {
        reportError(operation, cause, false, onError);
    }

    void reportError(String operation, Throwable cause, boolean fatal, Consumer<String> onError) {
        Throwable root = unwrap(cause);
        if (root instanceof CancellationException) {
            return;
        }
        String message = messageOf(root);
        LOGGER.warn("{}失败", operation, root);
        if (fatal) {
            markFailure();
        }
        sendLog(Language.tr("log.error.operationFailed", operation, message));
        if (onError != null) {
            Platform.runLater(() -> onError.accept(message));
        }
    }

    private static Throwable unwrap(Throwable cause) {
        Throwable current = cause;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String messageOf(Throwable cause) {
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    void shutdown() {
        downloadExecutor.shutdownNow();
        checkExecutor.shutdownNow();
        metadataExecutor.shutdownNow();
        javaExecutor.shutdownNow();
        launchExecutor.shutdownNow();
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

    @FunctionalInterface
    interface IoAction {
        void run() throws IOException;
    }
}
