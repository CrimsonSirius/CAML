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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
