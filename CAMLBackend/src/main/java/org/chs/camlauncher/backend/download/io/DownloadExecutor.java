package org.chs.camlauncher.backend.download.io;

import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** 使用有界线程池并发执行下载任务。 */
public final class DownloadExecutor {
    @FunctionalInterface
    public interface DownloadTask {
        void run(DownloadItem item, DownloadListener listener) throws IOException, InterruptedException;
    }

    private DownloadExecutor() {
    }

    public static void download(List<DownloadItem> items, int concurrency,
                                DownloadListener listener, DownloadTask task)
            throws IOException, InterruptedException {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(task, "task");
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

        try {
            actualListener.onSummary(new DownloadListener.DownloadSummary(
                    0L, totalBytes, 0, items.size()));

            int itemIndex = 0;
            for (DownloadItem item : items) {
                int index = itemIndex++;
                completion.submit(() -> {
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
                    return null;
                });
            }

            for (int index = 0; index < items.size(); index++) {
                Future<Void> completed = completion.take();
                try {
                    completed.get();
                } catch (ExecutionException e) {
                    rethrow(e.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
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
