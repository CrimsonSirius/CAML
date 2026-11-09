package org.chs.camlauncher.backend.api;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionManifestCacheTest {
    @TempDir
    Path tempDirectory;

    @Test
    void reusesOneManifestForAllVersionCategories() throws Exception {
        String manifestJson = """
                {
                  "latest": {"release": "1.21.5", "snapshot": "25w14a"},
                  "versions": [{
                    "id": "1.21.5",
                    "type": "release",
                    "url": "http://127.0.0.1:1/1.21.5.json",
                    "releaseTime": "2026-01-01T00:00:00+00:00"
                  }]
                }
                """;
        byte[] body = manifestJson.getBytes(StandardCharsets.UTF_8);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mc/game/version_manifest_v2.json", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        try (CAMLBackend backend = new CAMLBackend(new BackendConfig(
                tempDirectory.resolve("data"), tempDirectory.resolve("game")))) {
            backend.start();
            backend.setDownloadSource(
                    DownloadSourceMode.CUSTOM,
                    "http://127.0.0.1:" + server.getAddress().getPort()
            );

            VersionManifest first = backend.fetchVersionManifest();
            VersionManifest second = backend.fetchVersionManifest();

            assertEquals("1.21.5", first.latest().release());
            assertEquals("1.21.5", second.latest().release());
            assertEquals(1, requests.get(), "分类列表应复用同一次 manifest 请求");
        } finally {
            server.stop(0);
        }
    }
}
