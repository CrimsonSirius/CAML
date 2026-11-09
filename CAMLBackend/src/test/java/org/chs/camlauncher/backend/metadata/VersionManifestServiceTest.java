package org.chs.camlauncher.backend.metadata;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.source.MirrorDownloadSource;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.download.source.DownloadSource;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
                  "versions": [{
                    "id": "1.21.5",
                    "type": "release",
                    "url": "http://127.0.0.1:1/1.21.5.json",
                    "releaseTime": "2026-01-01T00:00:00+00:00"
                  }]
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

    @Test
    void usesConfiguredMirrorForManifest() throws Exception {
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
        Path cacheFile = tempDirectory.resolve("cache/version_manifest_v2.json");

        try (TestHttpServer server = new TestHttpServer()) {
            server.handle("/mc/game/version_manifest_v2.json", manifestJson);
            VersionManifestService service = new VersionManifestService();
            service.setSources(List.of(new MirrorDownloadSource(
                    "http://127.0.0.1:" + server.port())));

            VersionManifest manifest = service.fetchManifestOrCache(
                    VersionManifestService.DEFAULT_MANIFEST_URI,
                    cacheFile
            );

            assertEquals("1.21.5", manifest.latest().release());
            assertTrue(Files.isRegularFile(cacheFile));
        }
    }

    @Test
    void switchesToNextSourceWhenFirstSourceReturnsInvalidJson() throws Exception {
        String goodManifest = """
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
        Path cacheFile = tempDirectory.resolve("cache/version_manifest_v2.json");

        try (TestHttpServer server = new TestHttpServer()) {
            server.handle("/bad.json", "<html>not json</html>");
            server.handle("/good.json", goodManifest);
            VersionManifestService service = new VersionManifestService();
            service.setSources(List.of(
                    fixedSource(server.uri("/bad.json")),
                    fixedSource(server.uri("/good.json"))
            ));

            VersionManifest manifest = service.fetchManifestOrCache(
                    server.uri("/manifest.json"), cacheFile);

            assertEquals("1.21.5", manifest.latest().release());
            assertTrue(Files.readString(cacheFile).contains("1.21.5"));
        }
    }

    @Test
    void doesNotOverwriteCacheWhenAllResponsesAreInvalid() throws Exception {
        String cachedManifest = """
                {
                  "latest": {"release": "cached", "snapshot": "cached"},
                  "versions": [{
                    "id": "cached",
                    "type": "release",
                    "url": "http://127.0.0.1:1/cached.json",
                    "releaseTime": "2020-01-01T00:00:00+00:00"
                  }]
                }
                """;
        Path cacheFile = tempDirectory.resolve("cache/version_manifest_v2.json");
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, cachedManifest);

        try (TestHttpServer server = new TestHttpServer()) {
            server.handle("/bad.json", "<html>bad</html>");
            VersionManifestService service = new VersionManifestService();
            service.setSources(List.of(fixedSource(server.uri("/bad.json"))));

            VersionManifest manifest = service.fetchManifestOrCache(
                    server.uri("/bad.json"), cacheFile);

            assertEquals("cached", manifest.latest().release());
            assertEquals(cachedManifest, Files.readString(cacheFile));
        }
    }

    @Test
    void retriesWhenRequestTimesOut() throws Exception {
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
        Path cacheFile = tempDirectory.resolve("cache/version_manifest_v2.json");

        try (TestHttpServer server = new TestHttpServer()) {
            server.server.createContext("/slow", exchange -> {
                if (requests.incrementAndGet() == 1) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                try {
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                } catch (IOException ignored) {
                    // 第一次请求可能已被客户端取消。
                }
            });

            VersionManifestService service = new VersionManifestService(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                    new MetadataService(),
                    Duration.ofMillis(200),
                    2
            );
            VersionManifest manifest = service.fetchManifestOrCache(
                    server.uri("/slow"), cacheFile);

            assertEquals("1.21.5", manifest.latest().release());
            assertTrue(requests.get() >= 2);
        }
    }

    @Test
    void followsRedirectsManually() throws Exception {
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
        Path cacheFile = tempDirectory.resolve("cache/version_manifest_v2.json");

        try (TestHttpServer server = new TestHttpServer()) {
            server.redirect("/redirect", "/manifest.json");
            server.handle("/manifest.json", manifestJson);

            VersionManifest manifest = new VersionManifestService().fetchManifestOrCache(
                    server.uri("/redirect"), cacheFile);

            assertEquals("1.21.5", manifest.latest().release());
        }
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "version-manifest-test-http");
            thread.setDaemon(true);
            return thread;
        });

        private TestHttpServer() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor);
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

        private void redirect(String path, String location) {
            server.createContext(path, exchange -> {
                exchange.getResponseHeaders().add("Location", location);
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
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
            executor.shutdownNow();
        }
    }

    private static DownloadSource fixedSource(URI uri) {
        return new DownloadSource() {
            @Override
            public String name() {
                return "fixed";
            }

            @Override
            public List<URI> candidates(URI original) {
                return List.of(uri);
            }
        };
    }
}
