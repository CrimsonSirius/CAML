package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.download.SegmentResumeState.Segment;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/** 支持 Range 的大文件分片下载与断点续传。 */
final class SegmentedDownload {
    private static final int SEGMENT_CONCURRENCY = 8;
    private static final Semaphore SEGMENT_SLOTS = new Semaphore(SEGMENT_CONCURRENCY, true);

    private final HttpClient httpClient;
    private final Duration readTimeout;

    SegmentedDownload(HttpClient httpClient, Duration readTimeout) {
        this.httpClient = httpClient;
        this.readTimeout = readTimeout;
    }

    void download(
            URI uri,
            DownloadItem item,
            DownloadAttempt.RangeProbe probe,
            DownloadListener listener,
            DownloadValidator validator,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        token.throwIfCancelled();
        Path destination = item.destination();
        Path part = DownloadIo.partialPath(destination);
        Path metaPath = DownloadIo.metaPath(part);
        SegmentResumeState meta = SegmentResumeState.load(item, part, metaPath, probe);
        if (meta == null) {
            DownloadIo.clearPartial(item);
            meta = SegmentResumeState.create(
                    probe.totalSize(), item.sha1(), probe.etag(), probe.lastModified());
            meta.preallocate(part);
            meta.persist(metaPath);
        } else {
            meta.preallocate(part);
        }

        SegmentResumeState activeMeta = meta;
        AtomicLong completed = new AtomicLong(activeMeta.downloadedBytes());
        ProgressReporter reporter = new ProgressReporter(
                listener, item.label(), probe.totalSize(), completed);
        reporter.reportCurrent();

        List<Future<Void>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(SegmentResumeState.MAX_SEGMENTS,
                        Math.max(1, activeMeta.segments().size())),
                runnable -> {
                    Thread thread = new Thread(runnable, "caml-segment-worker");
                    thread.setDaemon(true);
                    return thread;
                }
        );
        try {
            for (Segment segment : activeMeta.segments()) {
                token.throwIfCancelled();
                if (segment.downloaded() >= segment.length()) {
                    continue;
                }
                futures.add(executor.submit((Callable<Void>) () -> {
                    token.throwIfCancelled();
                    while (!SEGMENT_SLOTS.tryAcquire(250, TimeUnit.MILLISECONDS)) {
                        token.throwIfCancelled();
                    }
                    try {
                        downloadSegment(
                                uri, item, part, metaPath, activeMeta, segment, reporter, token);
                    } finally {
                        SEGMENT_SLOTS.release();
                    }
                    return null;
                }));
            }
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    cancel(futures);
                    if (e.getCause() instanceof InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw interrupted;
                    }
                    throw unwrap(e);
                }
            }
        } catch (InterruptedException e) {
            cancel(futures);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            executor.shutdownNow();
            awaitExecutor(executor);
        }

        activeMeta.persist(metaPath);
        reporter.report(probe.totalSize());
        try {
            token.throwIfCancelled();
            DownloadIo.verify(part, item, validator);
            DownloadIo.move(part, destination);
            Files.deleteIfExists(metaPath);
        } catch (IOException | RuntimeException | InterruptedException e) {
            DownloadIo.clearPartialQuietly(item, e);
            throw e;
        }
    }

    private void downloadSegment(
            URI uri,
            DownloadItem item,
            Path part,
            Path metaPath,
            SegmentResumeState meta,
            Segment segment,
            ProgressReporter reporter,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        token.throwIfCancelled();
        long start = segment.start() + segment.downloaded();
        long end = segment.end();
        if (start > end) {
            return;
        }

        try {
            HttpRequest.Builder request = DownloadIo.requestBuilder(uri, item)
                    .timeout(Duration.ofMinutes(2))
                    .header("Range", "bytes=" + start + "-" + end);
            if (meta.ifRange() != null) {
                request.header("If-Range", meta.ifRange());
            }
            HttpResponse<InputStream> response = httpClient.send(
                    request.GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() == 200) {
                throw new DownloadAttempt.RangeUnsupportedException(
                        "服务器忽略 Range: " + uri);
            }
            if (response.statusCode() != 206) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }
            verifySegmentValidators(response, meta);

            String contentRange = response.headers().firstValue("Content-Range").orElse("");
            Matcher matcher = DownloadIo.CONTENT_RANGE.matcher(contentRange);
            if (!matcher.matches()
                    || Long.parseLong(matcher.group(1)) != start
                    || Long.parseLong(matcher.group(3)) != meta.total()) {
                throw new DownloadAttempt.RangeUnsupportedException(
                        "无效的 Content-Range: " + contentRange);
            }

            try (InputStream input = new ReadTimeoutInputStream(response.body(), readTimeout);
                 FileChannel channel = FileChannel.open(part, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                long position = start;
                while (position <= end) {
                    token.throwIfCancelled();
                    int want = (int) Math.min(buffer.length, end - position + 1);
                    int read = input.read(buffer, 0, want);
                    if (read < 0) {
                        throw new IOException("分片响应提前结束: " + uri);
                    }
                    int written = 0;
                    while (written < read) {
                        int count = channel.write(
                                ByteBuffer.wrap(buffer, written, read - written), position + written);
                        if (count <= 0) {
                            throw new IOException("分片写入没有进展: " + part);
                        }
                        written += count;
                    }
                    position += read;
                    synchronized (meta) {
                        segment.addDownloaded(read);
                        long now = System.nanoTime();
                        if (meta.checkpointDue(segment, now)) {
                            meta.persist(metaPath);
                            segment.markPersisted(now);
                        }
                    }
                    reporter.add(read);
                }
            }
            synchronized (meta) {
                segment.complete();
                meta.persist(metaPath);
                segment.markPersisted(System.nanoTime());
            }
        } catch (IOException | RuntimeException | InterruptedException e) {
            synchronized (meta) {
                meta.persist(metaPath);
            }
            throw e;
        }
    }

    private static void verifySegmentValidators(
            HttpResponse<?> response,
            SegmentResumeState meta
    ) throws DownloadAttempt.RangeUnsupportedException {
        if (meta.etag() != null) {
            if (!meta.etag().equals(DownloadIo.strongEtag(response))) {
                throw new DownloadAttempt.RangeUnsupportedException(
                        "ETag changed during resume");
            }
            return;
        }
        if (meta.lastModified() != null
                && !meta.lastModified().equals(DownloadIo.lastModified(response))) {
            throw new DownloadAttempt.RangeUnsupportedException(
                    "Last-Modified changed during resume");
        }
    }

    private static void cancel(List<Future<Void>> futures) {
        for (Future<Void> future : futures) {
            future.cancel(true);
        }
    }

    private static void awaitExecutor(ExecutorService executor) {
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (true) {
            try {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0 || executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
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

    private static IOException unwrap(ExecutionException exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        if (cause instanceof IOException ioException) {
            return ioException;
        }
        if (cause instanceof RuntimeException runtimeException) {
            return new IOException(runtimeException.getMessage(), runtimeException);
        }
        return new IOException("分片下载失败", cause);
    }

    private static final class ProgressReporter {
        private final DownloadListener listener;
        private final String label;
        private final long total;
        private final AtomicLong completed;
        private long lastReported;

        private ProgressReporter(DownloadListener listener, String label, long total,
                                 AtomicLong completed) {
            this.listener = listener;
            this.label = label;
            this.total = total;
            this.completed = completed;
        }

        void reportCurrent() {
            report(completed.get());
        }

        void add(long bytes) {
            report(completed.addAndGet(bytes));
        }

        synchronized void report(long downloaded) {
            if (downloaded <= lastReported) {
                return;
            }
            lastReported = downloaded;
            listener.onProgress(new DownloadListener.DownloadProgress(label, downloaded, total));
        }
    }
}
