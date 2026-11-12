package org.chs.camlauncher.backend.mod;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.mod.service.ModIconService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModIconServiceTest {
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
}
