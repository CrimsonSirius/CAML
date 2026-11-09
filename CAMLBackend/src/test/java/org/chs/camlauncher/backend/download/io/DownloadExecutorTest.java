package org.chs.camlauncher.backend.download.io;

import org.chs.camlauncher.backend.download.DownloadListener;
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
}
