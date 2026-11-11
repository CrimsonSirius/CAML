package org.chs.camlauncher.backend.download;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDownloaderTest {
    private static final String RANGE_ETAG = "\"caml-test-etag\"";
    private static final String RANGE_LAST_MODIFIED = "Wed, 01 Jan 2025 00:00:00 GMT";

    @TempDir
    Path tempDirectory;

    @Test
    void downloadsVerifiesAndAtomicallyMovesFile() throws Exception {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hello.txt", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path destination = tempDirectory.resolve("nested").resolve("hello.txt");
            DownloadItem item = new DownloadItem(
                    "hello",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/hello.txt")),
                    destination,
                    "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d",
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertTrue(item.existsAndValid());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsCancelledTokenBeforeNetwork() {
        DownloadCancellationToken token = new DownloadCancellationToken();
        token.cancel();
        Path destination = tempDirectory.resolve("cancelled.bin");
        DownloadItem item = new DownloadItem(
                "cancelled",
                List.of(URI.create("http://127.0.0.1:1/unused")),
                destination,
                null,
                -1
        );

        assertThrows(InterruptedException.class,
                () -> new FileDownloader().download(item, DownloadListener.NONE, null, token));
        assertFalse(Files.exists(destination));
    }

    @Test
    void cancellationDuringDownloadRemovesPartialFile() throws Exception {
        byte[] content = new byte[32 * 1024];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cancel.bin", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path destination = tempDirectory.resolve("cancel.bin");
            DownloadItem item = new DownloadItem(
                    "cancel",
                    List.of(URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/cancel.bin")),
                    destination,
                    null,
                    content.length
            );
            DownloadCancellationToken token = new DownloadCancellationToken();

            assertThrows(InterruptedException.class, () -> new FileDownloader().download(
                    item, progress -> token.cancel(), null, token));

            assertFalse(Files.exists(destination));
            assertFalse(Files.exists(destination.resolveSibling(destination.getFileName() + ".part")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesAfterTruncatedResponse() throws Exception {
        byte[] content = "hello retry".getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/retry.txt", exchange -> {
            int request = requests.incrementAndGet();
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                if (request == 1) {
                    output.write(content, 0, content.length / 2);
                    output.flush();
                    return;
                }
                output.write(content);
            }
        });
        server.start();

        try {
            Path destination = tempDirectory.resolve("retry.txt");
            DownloadItem item = new DownloadItem(
                    "retry",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/retry.txt")),
                    destination,
                    sha1(content),
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertTrue(requests.get() >= 2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadsWhenDestinationAlreadyExistsButIsEmpty() throws Exception {
        byte[] content = "installer".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/installer.jar", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path destination = tempDirectory.resolve("installer.jar");
            Files.createFile(destination);
            DownloadItem item = new DownloadItem(
                    "installer",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/installer.jar")),
                    destination,
                    null,
                    -1
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void attemptsEachCandidateUntilSixTotalAttempts() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/first.jar", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.createContext("/second.jar", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            DownloadItem item = new DownloadItem(
                    "retry-all",
                    List.of(URI.create(base + "/first.jar"), URI.create(base + "/second.jar")),
                    tempDirectory.resolve("retry-all.jar"),
                    null,
                    -1
            );

            assertThrows(IOException.class,
                    () -> new FileDownloader().download(item, DownloadListener.NONE));
            assertEquals(6, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackToNextCandidateWhenContentValidatorRejectsFirst() throws Exception {
        byte[] bad = "not an installer".getBytes(StandardCharsets.UTF_8);
        byte[] good = "valid installer".getBytes(StandardCharsets.UTF_8);
        AtomicInteger firstRequests = new AtomicInteger();
        AtomicInteger secondRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/first.jar", exchange -> {
            firstRequests.incrementAndGet();
            exchange.sendResponseHeaders(200, bad.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bad);
            }
        });
        server.createContext("/second.jar", exchange -> {
            secondRequests.incrementAndGet();
            exchange.sendResponseHeaders(200, good.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(good);
            }
        });
        server.start();

        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            Path destination = tempDirectory.resolve("installer.jar");
            DownloadItem item = new DownloadItem(
                    "installer",
                    List.of(URI.create(base + "/first.jar"), URI.create(base + "/second.jar")),
                    destination,
                    null,
                    -1
            );

            new FileDownloader().download(item, DownloadListener.NONE,
                    file -> {
                        if (!Files.readString(file).equals("valid installer")) {
                            throw new IOException("invalid installer content");
                        }
                    });

            assertArrayEquals(good, Files.readAllBytes(destination));
            assertEquals(1, firstRequests.get());
            assertEquals(1, secondRequests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadsLargeFileInSegments() throws Exception {
        byte[] content = new byte[5 * 1024 * 1024];
        for (int index = 0; index < content.length; index++) {
            content[index] = (byte) (index % 251);
        }
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = rangeServer(
                content, requests, new AtomicBoolean(false), new AtomicBoolean(false));

        try {
            Path destination = tempDirectory.resolve("segmented.bin");
            DownloadItem item = new DownloadItem(
                    "segmented",
                    List.of(URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/segmented.bin")),
                    destination,
                    sha1(content),
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertTrue(requests.get() > 2);
            assertTrue(Files.notExists(tempDirectory.resolve("segmented.bin.part")));
            assertTrue(Files.notExists(tempDirectory.resolve("segmented.bin.part.json")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resumesInterruptedSegmentFromCheckpoint() throws Exception {
        byte[] content = new byte[5 * 1024 * 1024];
        for (int index = 0; index < content.length; index++) {
            content[index] = (byte) (index % 241);
        }
        AtomicInteger requests = new AtomicInteger();
        AtomicBoolean resumedMidFirstSegment = new AtomicBoolean();
        AtomicReference<String> observedIfRange = new AtomicReference<>();
        HttpServer server = rangeServer(
                content,
                requests,
                new AtomicBoolean(true),
                resumedMidFirstSegment,
                RANGE_ETAG,
                RANGE_LAST_MODIFIED,
                observedIfRange
        );

        try {
            Path destination = tempDirectory.resolve("resumed.bin");
            DownloadItem item = new DownloadItem(
                    "resumed",
                    List.of(URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/resumed.bin")),
                    destination,
                    sha1(content),
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertTrue(resumedMidFirstSegment.get(), "应至少出现一次从分片中间位置继续的 Range 请求");
            assertEquals(RANGE_ETAG, observedIfRange.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resumesWithLastModifiedWhenEtagIsWeak() throws Exception {
        byte[] content = new byte[5 * 1024 * 1024];
        for (int index = 0; index < content.length; index++) {
            content[index] = (byte) (index % 239);
        }
        AtomicInteger requests = new AtomicInteger();
        AtomicBoolean resumedMidFirstSegment = new AtomicBoolean();
        AtomicReference<String> observedIfRange = new AtomicReference<>();
        HttpServer server = rangeServer(
                content,
                requests,
                new AtomicBoolean(true),
                resumedMidFirstSegment,
                "W/\"weak-etag\"",
                RANGE_LAST_MODIFIED,
                observedIfRange
        );

        try {
            Path destination = tempDirectory.resolve("last-modified.bin");
            DownloadItem item = new DownloadItem(
                    "last-modified",
                    List.of(URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/resumed.bin")),
                    destination,
                    sha1(content),
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertTrue(resumedMidFirstSegment.get());
            assertEquals(RANGE_LAST_MODIFIED, observedIfRange.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void discardsCheckpointWhenStoredEtagDoesNotMatch() throws Exception {
        byte[] content = new byte[5 * 1024 * 1024];
        for (int index = 0; index < content.length; index++) {
            content[index] = (byte) (index % 237);
        }
        AtomicInteger requests = new AtomicInteger();
        AtomicBoolean resumedMidFirstSegment = new AtomicBoolean();
        HttpServer server = rangeServer(
                content, requests, new AtomicBoolean(false), resumedMidFirstSegment);

        try {
            Path destination = tempDirectory.resolve("stale-etag.bin");
            Path part = destination.resolveSibling(destination.getFileName() + ".part");
            Path meta = destination.resolveSibling(destination.getFileName() + ".part.json");
            Files.write(part, new byte[content.length]);
            Files.writeString(meta, """
                    {
                      "total": %d,
                      "sha1": "%s",
                      "etag": "\\"stale-etag\\"",
                      "lastModified": "%s",
                      "segments": [
                        {"start": 0, "end": 2621439, "downloaded": 1},
                        {"start": 2621440, "end": 5242879, "downloaded": 0}
                      ]
                    }
                    """.formatted(content.length, sha1(content), RANGE_LAST_MODIFIED));
            DownloadItem item = new DownloadItem(
                    "stale-etag",
                    List.of(URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/resumed.bin")),
                    destination,
                    sha1(content),
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertFalse(resumedMidFirstSegment.get(), "ETag 变化后不能从旧检查点继续");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void abortsWhenResponseBodyReadStalls() throws Exception {
        byte[] content = new byte[1024];
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stalled.bin", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content, 0, 1);
                output.flush();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        server.start();

        try {
            DownloadItem item = new DownloadItem(
                    "stalled",
                    List.of(URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/stalled.bin")),
                    tempDirectory.resolve("stalled.bin"),
                    null,
                    content.length
            );
            FileDownloader downloader = new FileDownloader(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    1,
                    Duration.ofMillis(200)
            );

            long started = System.nanoTime();
            assertThrows(IOException.class,
                    () -> downloader.download(item, DownloadListener.NONE));
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
            assertTrue(elapsedMillis < 3000, "读超时应快速中止，而不是永久阻塞");
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    @Test
    void redownloadsExistingFileWithMismatchedSizeWithoutChecksum() throws Exception {
        byte[] content = "fresh".getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/existing.bin", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path destination = tempDirectory.resolve("existing.bin");
            Files.writeString(destination, "old");
            DownloadItem item = new DownloadItem(
                    "existing",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/existing.bin")),
                    destination,
                    null,
                    content.length
            );

            new FileDownloader().download(item, DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(destination));
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void validatesExistingFileBeforeReusingIt() throws Exception {
        byte[] replacement = "fresh".getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/validated.bin", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, replacement.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(replacement);
            }
        });
        server.start();

        try {
            Path destination = tempDirectory.resolve("validated.bin");
            Files.writeString(destination, "stale");
            DownloadItem item = new DownloadItem(
                    "validated",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/validated.bin")),
                    destination,
                    null,
                    replacement.length
            );

            new FileDownloader().download(item, DownloadListener.NONE, file -> {
                if (!Files.readString(file).equals("fresh")) {
                    throw new IOException("stale content");
                }
            });

            assertArrayEquals(replacement, Files.readAllBytes(destination));
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsResponseWithMismatchedContentLengthWithoutChecksum() throws Exception {
        byte[] content = "abc".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/wrong-length.bin", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            DownloadItem item = new DownloadItem(
                    "wrong-length",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/wrong-length.bin")),
                    tempDirectory.resolve("wrong-length.bin"),
                    null,
                    5
            );
            FileDownloader downloader = new FileDownloader(HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build(), 1);

            assertThrows(IOException.class,
                    () -> downloader.download(item, DownloadListener.NONE));
            assertFalse(Files.exists(item.destination()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsResponseShorterThanExpectedSizeWithoutChecksum() throws Exception {
        byte[] content = "abc".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/short.bin", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            DownloadItem item = new DownloadItem(
                    "short",
                    List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/short.bin")),
                    tempDirectory.resolve("short.bin"),
                    null,
                    5
            );
            FileDownloader downloader = new FileDownloader(HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build(), 1);

            assertThrows(IOException.class,
                    () -> downloader.download(item, DownloadListener.NONE));
            assertFalse(Files.exists(item.destination()));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer rangeServer(byte[] content, AtomicInteger requests,
                                          AtomicBoolean failFirstSegment,
                                          AtomicBoolean resumedMidFirstSegment) throws IOException {
        return rangeServer(
                content, requests, failFirstSegment, resumedMidFirstSegment,
                RANGE_ETAG, RANGE_LAST_MODIFIED, new AtomicReference<>());
    }

    private static HttpServer rangeServer(byte[] content, AtomicInteger requests,
                                          AtomicBoolean failFirstSegment,
                                          AtomicBoolean resumedMidFirstSegment,
                                          String etag, String lastModified,
                                          AtomicReference<String> observedIfRange) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/segmented.bin", exchange -> handleRange(
                exchange, content, requests, failFirstSegment, resumedMidFirstSegment,
                etag, lastModified, observedIfRange));
        server.createContext("/resumed.bin", exchange -> handleRange(
                exchange, content, requests, failFirstSegment, resumedMidFirstSegment,
                etag, lastModified, observedIfRange));
        server.start();
        return server;
    }

    private static void handleRange(com.sun.net.httpserver.HttpExchange exchange,
                                    byte[] content, AtomicInteger requests,
                                    AtomicBoolean failFirstSegment,
                                    AtomicBoolean resumedMidFirstSegment,
                                    String etag, String lastModified,
                                    AtomicReference<String> observedIfRange) throws IOException {
        String range = exchange.getRequestHeaders().getFirst("Range");
        if (range == null || !range.startsWith("bytes=")) {
            addValidators(exchange, etag, lastModified);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(content);
            }
            return;
        }

        String[] bounds = range.substring("bytes=".length()).split("-", -1);
        long start = Long.parseLong(bounds[0]);
        long end = bounds.length < 2 || bounds[1].isBlank()
                ? content.length - 1
                : Long.parseLong(bounds[1]);
        end = Math.min(end, content.length - 1L);
        long length = end - start + 1;
        requests.incrementAndGet();

        if (start > 0 && start < content.length / 2L) {
            resumedMidFirstSegment.set(true);
        }

        String ifRange = exchange.getRequestHeaders().getFirst("If-Range");
        if (ifRange != null) {
            observedIfRange.set(ifRange);
        }
        addValidators(exchange, etag, lastModified);
        exchange.getResponseHeaders().add(
                "Content-Range", "bytes " + start + "-" + end + "/" + content.length);
        exchange.sendResponseHeaders(206, length);
        try (OutputStream output = exchange.getResponseBody()) {
            if (start == 0 && end > 0 && failFirstSegment.compareAndSet(true, false)) {
                output.write(content, 0, (int) (length / 2));
                output.flush();
                return;
            }
            output.write(content, (int) start, (int) length);
        }
    }

    private static void addValidators(com.sun.net.httpserver.HttpExchange exchange,
                                      String etag, String lastModified) {
        if (etag != null) {
            exchange.getResponseHeaders().add("ETag", etag);
        }
        if (lastModified != null) {
            exchange.getResponseHeaders().add("Last-Modified", lastModified);
        }
    }

    private static String sha1(byte[] content) {
        try {
            Path temporary = Files.createTempFile("caml-test-", ".bin");
            try {
                Files.write(temporary, content);
                return HashVerifier.sha1(temporary);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
