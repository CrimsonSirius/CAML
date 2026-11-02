package org.chs.camlauncher.backend.download;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
}
