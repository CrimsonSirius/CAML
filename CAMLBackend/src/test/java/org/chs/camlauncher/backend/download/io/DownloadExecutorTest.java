package org.chs.camlauncher.backend.download.io;

import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadExecutorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void limitsConcurrentDownloads() throws Exception {
        List<DownloadItem> items = IntStream.range(0, 12)
                .mapToObj(index -> new DownloadItem(
                        "item-" + index,
                        List.of(URI.create("https://example.invalid/" + index)),
                        tempDirectory.resolve("item-" + index),
                        null,
                        -1
                ))
                .toList();

        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(3);
        CountDownLatch finished = new CountDownLatch(items.size());

        try {
            DownloadExecutor.download(items, 3, DownloadListener.NONE, (item, listener) -> {
                int current = active.incrementAndGet();
                maximum.accumulateAndGet(current, Math::max);
                try {
                    barrier.await(2, TimeUnit.SECONDS);
                    active.decrementAndGet();
                    finished.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } catch (Exception e) {
                    throw new IOException("并发任务同步失败", e);
                }
            });
        } finally {
            finished.await(1, TimeUnit.SECONDS);
        }

        assertEquals(3, maximum.get());
    }

    @Test
    void reportsAggregateSummary() throws Exception {
        List<DownloadItem> items = List.of(
                new DownloadItem(
                        "first",
                        List.of(URI.create("https://example.invalid/first")),
                        tempDirectory.resolve("first"),
                        null,
                        100
                ),
                new DownloadItem(
                        "second",
                        List.of(URI.create("https://example.invalid/second")),
                        tempDirectory.resolve("second"),
                        null,
                        200
                )
        );
        AtomicReference<DownloadListener.DownloadSummary> lastSummary = new AtomicReference<>();

        DownloadExecutor.download(items, 2, new DownloadListener() {
            @Override
            public void onProgress(DownloadListener.DownloadProgress progress) {
            }

            @Override
            public void onSummary(DownloadListener.DownloadSummary summary) {
                lastSummary.set(summary);
            }
        }, (item, listener) -> {
            listener.onProgress(new DownloadListener.DownloadProgress(
                    item.label(), item.size() / 2, item.size()));
            listener.onProgress(new DownloadListener.DownloadProgress(
                    item.label(), item.size(), item.size()));
        });

        DownloadListener.DownloadSummary summary = lastSummary.get();
        assertNotNull(summary);
        assertEquals(300, summary.downloaded());
        assertEquals(300, summary.total());
        assertEquals(2, summary.completedItems());
        assertEquals(2, summary.totalItems());
    }

    @Test
    void failureWaitsForSiblingWorkerToStop() throws Exception {
        List<DownloadItem> items = List.of(
                new DownloadItem("fail",
                        List.of(URI.create("https://example.invalid/fail")),
                        tempDirectory.resolve("fail"), null, -1),
                new DownloadItem("slow",
                        List.of(URI.create("https://example.invalid/slow")),
                        tempDirectory.resolve("slow"), null, -1)
        );
        CyclicBarrier started = new CyclicBarrier(2);
        CountDownLatch slowStopped = new CountDownLatch(1);

        assertThrows(IOException.class, () -> DownloadExecutor.download(
                items, 2, DownloadListener.NONE, (item, listener) -> {
                    try {
                        started.await(2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IOException("并发任务同步失败", e);
                    }
                    if ("fail".equals(item.label())) {
                        throw new IOException("boom");
                    }
                    try {
                        new CountDownLatch(1).await();
                    } finally {
                        slowStopped.countDown();
                    }
                }));

        assertEquals(0, slowStopped.getCount(), "失败返回前必须等待其他 worker 结束");
    }

    @Test
    void cancellationStopsWorkersBeforeReturning() throws Exception {
        List<DownloadItem> items = List.of(
                new DownloadItem("first",
                        List.of(URI.create("https://example.invalid/first")),
                        tempDirectory.resolve("first"), null, -1),
                new DownloadItem("second",
                        List.of(URI.create("https://example.invalid/second")),
                        tempDirectory.resolve("second"), null, -1)
        );
        DownloadCancellationToken token = new DownloadCancellationToken();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch stopped = new CountDownLatch(2);
        Thread canceller = new Thread(() -> {
            try {
                started.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                token.cancel();
            }
        }, "caml-test-canceller");
        canceller.start();

        try {
            assertThrows(InterruptedException.class, () -> DownloadExecutor.download(
                    items, 2, DownloadListener.NONE, (item, listener) -> {
                        started.countDown();
                        try {
                            new CountDownLatch(1).await();
                        } finally {
                            stopped.countDown();
                        }
                    }, token));
        } finally {
            canceller.join(2000);
        }

        assertEquals(0, stopped.getCount(), "取消返回前必须等待 worker 结束");
    }
}
