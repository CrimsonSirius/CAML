package org.chs.camlauncher.backend.download.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** 使用有界线程池并发执行下载任务。 */
public final class DownloadExecutor {
    private static final Logger LOGGER = LogManager.getLogger(DownloadExecutor.class);
    private static final long WORKER_SHUTDOWN_SECONDS = 10;

    @FunctionalInterface
    public interface DownloadTask {
        void run(DownloadItem item, DownloadListener listener) throws IOException, InterruptedException;
    }

    private DownloadExecutor() {
    }

    public static void download(List<DownloadItem> items, int concurrency,
                                DownloadListener listener, DownloadTask task)
            throws IOException, InterruptedException {
        download(items, concurrency, listener, task, new DownloadCancellationToken());
    }

    public static void download(List<DownloadItem> items, int concurrency,
                                DownloadListener listener, DownloadTask task,
                                DownloadCancellationToken token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(token, "token");
        if (items.isEmpty()) {
            return;
        }

        int parallelism = Math.min(DownloadConcurrency.clamp(concurrency), items.size());
        ExecutorService executor = Executors.newFixedThreadPool(parallelism, runnable -> {
            Thread thread = new Thread(runnable, "caml-download-worker");
            thread.setDaemon(true);
            return thread;
        });
        CompletionService<Void> completion = new ExecutorCompletionService<>(executor);
        DownloadListener actualListener = listener == null ? DownloadListener.NONE : listener;
        long totalBytes = items.stream()
                .mapToLong(item -> Math.max(0L, item.size()))
                .sum();
        long[] itemDownloaded = new long[items.size()];
        long[] aggregateDownloaded = {0L};
        int[] completedItems = {0};
        Object summaryLock = new Object();
        List<Future<Void>> futures = new ArrayList<>();

        try {
            token.throwIfCancelled();
            actualListener.onSummary(new DownloadListener.DownloadSummary(
                    0L, totalBytes, 0, items.size()));

            int itemIndex = 0;
            for (DownloadItem item : items) {
                token.throwIfCancelled();
                int index = itemIndex++;
                futures.add(completion.submit(() -> {
                    token.throwIfCancelled();
                    task.run(item, progress -> {
                        actualListener.onProgress(progress);

                        synchronized (summaryLock) {
                            long previous = itemDownloaded[index];
                            itemDownloaded[index] = progress.downloaded();
                            aggregateDownloaded[0] += progress.downloaded() - previous;
                            actualListener.onSummary(new DownloadListener.DownloadSummary(
                                    aggregateDownloaded[0],
                                    totalBytes,
                                    completedItems[0],
                                    items.size()
                            ));
                        }
                    });

                    synchronized (summaryLock) {
                        completedItems[0]++;
                        actualListener.onSummary(new DownloadListener.DownloadSummary(
                                aggregateDownloaded[0],
                                totalBytes,
                                completedItems[0],
                                items.size()
                        ));
                    }
                    token.throwIfCancelled();
                    return null;
                }));
            }

            int remaining = items.size();
            while (remaining > 0) {
                token.throwIfCancelled();
                Future<Void> completed = completion.poll(100, TimeUnit.MILLISECONDS);
                if (completed == null) {
                    continue;
                }
                remaining--;
                try {
                    completed.get();
                } catch (ExecutionException e) {
                    rethrow(e.getCause());
                }
            }
            token.throwIfCancelled();
        } finally {
            shutdown(executor, futures);
        }
    }

    private static void shutdown(ExecutorService executor, List<Future<Void>> futures) {
        for (Future<Void> future : futures) {
            future.cancel(true);
        }
        executor.shutdownNow();
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WORKER_SHUTDOWN_SECONDS);
        while (true) {
            try {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    LOGGER.warn("下载线程未在 {} 秒内结束", WORKER_SHUTDOWN_SECONDS);
                    break;
                }
                if (executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void rethrow(Throwable cause) throws IOException, InterruptedException {
        if (cause instanceof IOException ioException) {
            throw ioException;
        }
        if (cause instanceof InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw interruptedException;
        }
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        throw new IOException("下载任务失败", cause);
    }
}
