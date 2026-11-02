package org.chs.camlauncher.backend.metadata;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionManifestServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void fetchesManifestAndVersionInfo() throws Exception {
        String manifestJson = """
                {
                  "latest": {"release": "1.21.5", "snapshot": "25w14a"},
                  "versions": [
                    {
                      "id": "1.21.5",
                      "type": "release",
                      "url": "http://127.0.0.1:PORT/1.21.5.json",
                      "time": "2026-01-01T00:00:00+00:00",
                      "releaseTime": "2026-01-01T00:00:00+00:00",
                      "sha1": "manifest-sha1",
                      "complianceLevel": 1
                    }
                  ]
                }
                """;
        String versionJson = """
                {
                  "id": "1.21.5",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main"
                }
                """;

        try (TestHttpServer server = new TestHttpServer()) {
            server.handle("/version_manifest_v2.json", manifestJson.replace("PORT", Integer.toString(server.port())));
            server.handle("/1.21.5.json", versionJson);

            VersionManifestService service = new VersionManifestService();
            VersionManifest manifest = service.fetchManifest(server.uri("/version_manifest_v2.json"));
            VersionInfo version = service.fetchVersionInfo(manifest.versions().getFirst());

            assertEquals("1.21.5", manifest.latest().release());
            assertEquals("1.21.5", version.id());
            assertEquals(1, manifest.versions().size());
        }
    }

    @Test
    void fallsBackToCachedManifestWhenFetchFails() throws Exception {
        String manifestJson = """
                {
                  "latest": {"release": "1.21.5", "snapshot": "25w14a"},
                  "versions": []
                }
                """;
        Path cacheFile = tempDirectory.resolve("cache/version_manifest_v2.json");

        try (TestHttpServer server = new TestHttpServer()) {
            server.handle("/version_manifest_v2.json", manifestJson);
            VersionManifestService service = new VersionManifestService(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                    new MetadataService()
            );

            service.fetchManifestOrCache(server.uri("/version_manifest_v2.json"), cacheFile);
            assertTrue(Files.isRegularFile(cacheFile));

            VersionManifest cached = service.fetchManifestOrCache(
                    URI.create("http://127.0.0.1:1/version_manifest_v2.json"),
                    cacheFile
            );
            assertEquals("1.21.5", cached.latest().release());
        }
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;

        private TestHttpServer() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.start();
        }

        private void handle(String path, String body) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            server.createContext(path, exchange -> {
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
        }

        private int port() {
            return server.getAddress().getPort();
        }

        private URI uri(String path) {
            return URI.create("http://127.0.0.1:" + port() + path);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
