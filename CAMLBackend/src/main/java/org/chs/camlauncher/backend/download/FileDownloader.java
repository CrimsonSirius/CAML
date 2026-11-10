package org.chs.camlauncher.backend.download;

import com.google.gson.Gson;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileDownloader {
    private static final int DEFAULT_MAX_ATTEMPTS = 6;
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final long MIN_SEGMENT_SIZE = 4L * 1024 * 1024;
    private static final int MAX_SEGMENTS = 4;
    private static final int SEGMENT_CONCURRENCY = 8;
    private static final long CHECKPOINT_BYTES = 1024 * 1024;
    private static final long CHECKPOINT_NANOS = 1_000_000_000L;
    private static final Gson GSON = new Gson();
    private static final Semaphore SEGMENT_SLOTS = new Semaphore(SEGMENT_CONCURRENCY, true);
    private static final Pattern CONTENT_RANGE = Pattern.compile(
            "bytes\\s+(\\d+)-(\\d+)/(\\d+)"
    );

    private final HttpClient httpClient;
    private final int maxAttempts;
    private final Duration readTimeout;

    public FileDownloader() {
        this(defaultClient(), DEFAULT_MAX_ATTEMPTS, DEFAULT_READ_TIMEOUT);
    }

    public FileDownloader(HttpClient httpClient, int maxAttempts) {
        this(httpClient, maxAttempts, DEFAULT_READ_TIMEOUT);
    }

    public FileDownloader(HttpClient httpClient, int maxAttempts, Duration readTimeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("readTimeout must be positive");
        }
    }

    public void download(DownloadItem item, DownloadListener listener)
            throws IOException, InterruptedException {
        download(item, listener, null);
    }

    public void download(DownloadItem item, DownloadListener listener,
                         DownloadValidator validator)
            throws IOException, InterruptedException {
        download(item, listener, validator, new DownloadCancellationToken());
    }

    public void download(DownloadItem item, DownloadListener listener,
                         DownloadValidator validator, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        DownloadListener progress = listener == null ? DownloadListener.NONE : listener;
        if (item.existsAndValid()) {
            progress.onProgress(new DownloadListener.DownloadProgress(
                    item.label(), item.size(), item.size()));
            return;
        }

        Path parent = item.destination().toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        IOException lastFailure = null;
        URI lastUri = null;
        int attempted = 0;
        int round = 0;
        while (attempted < maxAttempts) {
            token.throwIfCancelled();
            for (URI uri : item.candidates()) {
                if (attempted >= maxAttempts) {
                    break;
                }
                token.throwIfCancelled();
                attempted++;
                lastUri = uri;
                try {
                    downloadOnce(uri, item, progress, validator, token);
                    return;
                } catch (IOException e) {
                    lastFailure = e;
                }
            }
            round++;
            if (attempted < maxAttempts) {
                Thread.sleep(200L * round);
                token.throwIfCancelled();
            }
        }

        throw new BackendException(BackendError.of(
                "download.error.failed",
                item.label(),
                attempted,
                lastUri == null ? "" : lastUri
        ), lastFailure);
    }

    private void downloadOnce(URI uri, DownloadItem item, DownloadListener listener,
                              DownloadValidator validator, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        long expectedSize = item.size();
        if (expectedSize >= MIN_SEGMENT_SIZE) {
            RangeProbe probe = probeRange(uri, token);
            if (probe.ranged()) {
                if (probe.totalSize() <= 0
                        || (expectedSize > 0 && expectedSize != probe.totalSize())) {
                    throw new IOException("服务器返回的文件大小与元数据不一致: " + uri);
                }
                if (probe.totalSize() >= MIN_SEGMENT_SIZE) {
                    try {
                        downloadSegmented(uri, item, probe, listener, validator, token);
                        return;
                    } catch (InterruptedException e) {
                        clearPartialQuietly(item, e);
                        throw e;
                    } catch (RangeUnsupportedException e) {
                        clearPartial(item);
                    }
                }
            }
        }
        downloadSingleStream(uri, item, listener, validator, token);
    }

    private void downloadSingleStream(URI uri, DownloadItem item,
                                      DownloadListener listener, DownloadValidator validator,
                                      DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        clearPartial(item);
        Path destination = item.destination();
        Path part = partialPath(destination);
        boolean moved = false;
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            try (InputStream input = new ReadTimeoutInputStream(response.body(), readTimeout)) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP " + response.statusCode() + " for " + uri);
                }

                long total = item.size() > 0
                        ? item.size()
                        : response.headers().firstValueAsLong("Content-Length").orElse(-1);
                long downloaded = 0;
                byte[] buffer = new byte[8192];
                int read;

                try (OutputStream output = Files.newOutputStream(
                        part,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                )) {
                    while ((read = input.read(buffer)) != -1) {
                        token.throwIfCancelled();
                        output.write(buffer, 0, read);
                        downloaded += read;
                        listener.onProgress(new DownloadListener.DownloadProgress(
                                item.label(), downloaded, total));
                    }
                }

                token.throwIfCancelled();
                verify(part, item, validator);
                move(part, destination);
                moved = true;
            }
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(part);
                } catch (IOException ignored) {
                    // 保留原始下载异常；取消时尤其不能因为清理失败而掩盖 InterruptedException。
                }
            }
        }
    }

    private void downloadSegmented(URI uri, DownloadItem item, RangeProbe probe,
                                   DownloadListener listener, DownloadValidator validator,
                                   DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        Path destination = item.destination();
        Path part = partialPath(destination);
        Path metaPath = metaPath(part);
        ResumeMeta meta = loadResumeMeta(item, part, metaPath, probe.totalSize());
        if (meta == null) {
            clearPartial(item);
            meta = ResumeMeta.create(probe.totalSize(), item.sha1());
            preallocate(part, probe.totalSize());
            persistMeta(metaPath, meta);
        } else {
            preallocate(part, probe.totalSize());
        }

        ResumeMeta activeMeta = meta;
        AtomicLong completed = new AtomicLong(activeMeta.downloadedBytes());
        ProgressReporter reporter = new ProgressReporter(
                listener, item.label(), probe.totalSize(), completed);
        reporter.reportCurrent();

        List<Future<Void>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(MAX_SEGMENTS, Math.max(1, activeMeta.segments.size())),
                runnable -> {
                    Thread thread = new Thread(runnable, "caml-segment-worker");
                    thread.setDaemon(true);
                    return thread;
                }
        );
        try {
            for (SegmentState segment : activeMeta.segments) {
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
                        downloadSegment(uri, part, metaPath, activeMeta, segment, reporter, token);
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

        persistMeta(metaPath, activeMeta);
        reporter.report(probe.totalSize());
        try {
            token.throwIfCancelled();
            verify(part, item, validator);
            move(part, destination);
            Files.deleteIfExists(metaPath);
        } catch (IOException | RuntimeException | InterruptedException e) {
            clearPartialQuietly(item, e);
            throw e;
        }
    }

    private void downloadSegment(URI uri, Path part, Path metaPath,
                                 ResumeMeta meta, SegmentState segment,
                                 ProgressReporter reporter,
                                 DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        long start = segment.start() + segment.downloaded();
        long end = segment.end();
        if (start > end) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(2))
                    .header("Range", "bytes=" + start + "-" + end)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() == 200) {
                throw new RangeUnsupportedException("服务器忽略 Range: " + uri);
            }
            if (response.statusCode() != 206) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }

            String contentRange = response.headers().firstValue("Content-Range").orElse("");
            Matcher matcher = CONTENT_RANGE.matcher(contentRange);
            if (!matcher.matches()
                    || Long.parseLong(matcher.group(1)) != start
                    || Long.parseLong(matcher.group(3)) != meta.total()) {
                throw new RangeUnsupportedException("无效的 Content-Range: " + contentRange);
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
                        if (now - segment.lastPersistNanos() >= CHECKPOINT_NANOS
                                || segment.downloaded() - segment.lastPersistBytes()
                                >= CHECKPOINT_BYTES) {
                            persistMeta(metaPath, meta);
                            segment.markPersisted(now);
                        }
                    }
                    reporter.add(read);
                }
            }
            synchronized (meta) {
                segment.complete();
                persistMeta(metaPath, meta);
                segment.markPersisted(System.nanoTime());
            }
        } catch (IOException | RuntimeException | InterruptedException e) {
            synchronized (meta) {
                persistMeta(metaPath, meta);
            }
            throw e;
        }
    }

    private RangeProbe probeRange(URI uri, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .header("Range", "bytes=0-0")
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );
        try (InputStream ignored = response.body()) {
            token.throwIfCancelled();
            int status = response.statusCode();
            if (status == 206) {
                String contentRange = response.headers().firstValue("Content-Range").orElse("");
                Matcher matcher = CONTENT_RANGE.matcher(contentRange);
                if (matcher.matches()) {
                    return new RangeProbe(true, Long.parseLong(matcher.group(3)));
                }
                return new RangeProbe(false, -1);
            }
            if (status == 200 || status == 416) {
                return new RangeProbe(false, response.headers()
                        .firstValueAsLong("Content-Length").orElse(-1));
            }
            throw new IOException("HTTP " + status + " for " + uri);
        }
    }

    private ResumeMeta loadResumeMeta(DownloadItem item, Path part, Path metaPathForRead,
                                      long total) throws IOException {
        if (!Files.isRegularFile(part) || !Files.isRegularFile(metaPathForRead)) {
            return null;
        }
        try {
            ResumeMeta meta = GSON.fromJson(
                    Files.readString(metaPathForRead), ResumeMeta.class);
            if (meta == null || meta.total() != total || meta.segments() == null) {
                return null;
            }
            if (item.sha1() != null && !item.sha1().isBlank()
                    && !item.sha1().equalsIgnoreCase(meta.sha1())) {
                return null;
            }
            if (Files.size(part) != total || !meta.matchesPlan()) {
                return null;
            }
            meta.markPersisted(System.nanoTime());
            return meta;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void persistMeta(Path metaPath, ResumeMeta meta) throws IOException {
        synchronized (meta) {
            Path parent = metaPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = Files.createTempFile(parent, "caml-resume-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(meta));
                try {
                    Files.move(temporary, metaPath,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, metaPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void preallocate(Path part, long total) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(part.toFile(), "rw")) {
            if (file.length() != total) {
                file.setLength(total);
            }
        }
    }

    private static void verify(Path file, DownloadItem item, DownloadValidator validator)
            throws IOException {
        if (item.hasSha1() && !HashVerifier.matches(file, item.sha1())) {
            throw new IOException("SHA-1 mismatch for " + item.label());
        }
        if (validator != null) {
            validator.validate(file);
        }
    }

    private static void clearPartial(DownloadItem item) throws IOException {
        Path part = partialPath(item.destination());
        Files.deleteIfExists(part);
        Files.deleteIfExists(metaPath(part));
    }

    private static void clearPartialQuietly(DownloadItem item, Throwable original) {
        try {
            clearPartial(item);
        } catch (IOException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
        }
    }

    private static Path partialPath(Path destination) {
        return destination.resolveSibling(destination.getFileName() + ".part");
    }

    private static Path metaPath(Path part) {
        return part.resolveSibling(part.getFileName() + ".json");
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
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

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private record RangeProbe(boolean ranged, long totalSize) {
    }

    private static final class RangeUnsupportedException extends IOException {
        private RangeUnsupportedException(String message) {
            super(message);
        }
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

    private static final class ResumeMeta {
        private long total;
        private String sha1;
        private List<SegmentState> segments;

        private ResumeMeta() {
        }

        static ResumeMeta create(long total, String sha1) {
            ResumeMeta meta = new ResumeMeta();
            meta.total = total;
            meta.sha1 = sha1;
            meta.segments = createSegments(total);
            return meta;
        }

        long total() {
            return total;
        }

        String sha1() {
            return sha1;
        }

        List<SegmentState> segments() {
            return segments;
        }

        long downloadedBytes() {
            return segments.stream().mapToLong(SegmentState::downloaded).sum();
        }

        boolean matchesPlan() {
            List<SegmentState> expected = createSegments(total);
            if (segments.size() != expected.size()) {
                return false;
            }
            for (int index = 0; index < segments.size(); index++) {
                SegmentState actual = segments.get(index);
                SegmentState wanted = expected.get(index);
                if (actual.start() != wanted.start()
                        || actual.end() != wanted.end()
                        || actual.downloaded() < 0
                        || actual.downloaded() > actual.length()) {
                    return false;
                }
            }
            return true;
        }

        void markPersisted(long now) {
            for (SegmentState segment : segments) {
                segment.markPersisted(now);
            }
        }

        private static List<SegmentState> createSegments(long total) {
            int count = (int) Math.min(MAX_SEGMENTS,
                    Math.max(2, (total + MIN_SEGMENT_SIZE - 1) / MIN_SEGMENT_SIZE));
            long length = (total + count - 1) / count;
            List<SegmentState> result = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                long start = index * length;
                if (start >= total) {
                    break;
                }
                long end = Math.min(total - 1, start + length - 1);
                result.add(new SegmentState(start, end));
            }
            return result;
        }
    }

    private static final class SegmentState {
        private long start;
        private long end;
        private long downloaded;
        private transient long lastPersistNanos;
        private transient long lastPersistBytes;

        private SegmentState() {
        }

        SegmentState(long start, long end) {
            this.start = start;
            this.end = end;
            markPersisted(System.nanoTime());
        }

        long start() {
            return start;
        }

        long end() {
            return end;
        }

        long downloaded() {
            return downloaded;
        }

        long length() {
            return end - start + 1;
        }

        long lastPersistNanos() {
            return lastPersistNanos;
        }

        long lastPersistBytes() {
            return lastPersistBytes;
        }

        void addDownloaded(long bytes) {
            downloaded += bytes;
        }

        void complete() {
            downloaded = length();
        }

        void markPersisted(long now) {
            lastPersistNanos = now;
            lastPersistBytes = downloaded;
        }
    }
}
