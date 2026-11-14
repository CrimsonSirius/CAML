package org.chs.camlauncher.backend.mod;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.mod.service.ModIconService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModIconServiceTest {
    private static final String WEBP_LOSSLESS =
            "UklGRhwAAABXRUJQVlA4TA8AAAAvAUAAEAcQ/Y/+BSKi/wEA";

    @TempDir
    Path dataDirectory;

    @Test
    void downloadsAndReusesCachedIcon() throws Exception {
        byte[] content = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/icon.png", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            ModIconService service = new ModIconService(dataDirectory);
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/icon.png";

            byte[] first = service.load(url);
            byte[] second = service.load(url);

            assertArrayEquals(content, first);
            assertArrayEquals(content, second);
            assertEquals(1, requests.get());
            try (var files = Files.list(dataDirectory.resolve("cache/mod-icons"))) {
                assertTrue(files.findAny().isPresent());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotRetryInsideSingleAttempt() throws Exception {
        byte[] content = "retry-image".getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/icon.png", exchange -> {
            if (requests.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            ModIconService service = new ModIconService(dataDirectory);
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/icon.png";

            assertThrows(IOException.class, () -> service.load(url));
            assertEquals(1, requests.get());

            assertArrayEquals(content, service.load(url));
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void convertsWebpToPngAndNormalizesExistingCache() throws Exception {
        byte[] webp = Base64.getDecoder().decode(WEBP_LOSSLESS);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/icon.webp", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "image/webp");
            exchange.sendResponseHeaders(200, webp.length);
            try (var output = exchange.getResponseBody()) {
                output.write(webp);
            }
        });
        server.start();

        try {
            ModIconService service = new ModIconService(dataDirectory);
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/icon.webp";

            byte[] downloaded = service.load(url);
            assertPng(downloaded);
            assertEquals(1, requests.get());

            Path cached = dataDirectory.resolve("cache/mod-icons")
                    .resolve(HashVerifier.sha1(url.getBytes(StandardCharsets.UTF_8)) + ".img");
            Files.write(cached, webp);

            byte[] normalized = service.load(url);
            assertPng(normalized);
            assertPng(Files.readAllBytes(cached));
            assertEquals(1, requests.get(), "旧 WebP 缓存应原地转换，不应重新请求网络");
        } finally {
            server.stop(0);
        }
    }

    private static void assertPng(byte[] bytes) throws IOException {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        assertArrayEquals(signature, Arrays.copyOf(bytes, signature.length));
        assertTrue(ImageIO.read(new ByteArrayInputStream(bytes)) != null);
    }
}
