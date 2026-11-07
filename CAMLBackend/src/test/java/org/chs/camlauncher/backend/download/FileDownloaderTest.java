package org.chs.camlauncher.backend.download;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDownloaderTest {
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
