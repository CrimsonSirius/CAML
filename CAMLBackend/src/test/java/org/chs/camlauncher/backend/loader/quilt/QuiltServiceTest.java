package org.chs.camlauncher.backend.loader.quilt;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.MirrorDownloadSource;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuiltServiceTest {

    @Test
    void fetchesLoadersProfileAndApiVersions() throws Exception {
        // 官方 /v3/versions/loader/<game> 把版本信息嵌在 loader 对象里，并附带 hashed/intermediary/launcherMeta。
        String loaderJson = """
                [
                  {
                    "loader": {"maven":"org.quiltmc:quilt-loader:0.27.1-beta.4",
                               "version":"0.27.1-beta.4","build":32,"separator":"."},
                    "hashed": {"maven":"org.quiltmc:hashed:1.21.4","version":"1.21.4"},
                    "intermediary": {"maven":"net.fabricmc:intermediary:1.21.4","version":"1.21.4"}
                  },
                  {
                    "loader": {"maven":"org.quiltmc:quilt-loader:0.27.0",
                               "version":"0.27.0","build":0,"separator":"."}
                  },
                  {
                    "hashed": {"maven":"org.quiltmc:hashed:1.21.4","version":"1.21.4"}
                  }
                ]
                """;
        String profileJson = """
                {"id":"quilt-loader-0.27.1-beta.4-1.21.4","inheritsFrom":"1.21.4",
                 "mainClass":"org.quiltmc.loader.impl.launch.knot.KnotClient"}
                """;
        String apiJson = """
                [
                  {
                    "name":"Quilted Fabric API 0.92.5",
                    "version_number":"0.92.5+1.21.4",
                    "version_type":"release",
                    "date_published":"2025-08-08T13:22:14Z",
                    "status":"listed",
                    "files":[{
                      "url":"https://cdn.example/quilted-fabric-api.jar",
                      "filename":"quilted-fabric-api.jar",
                      "hashes":{"sha1":"api-sha1"},
                      "size":321,
                      "primary":true
                    }]
                  }
                ]
                """;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/versions/loader/1.21.4",
                exchange -> respond(exchange, loaderJson));
        server.createContext("/v3/versions/loader/1.21.4/0.27.1-beta.4/profile/json",
                exchange -> respond(exchange, profileJson));
        server.createContext("/v2/project/qsl/version",
                exchange -> respond(exchange, apiJson));
        server.start();

        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            QuiltService service = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    base,
                    base
            );

            List<QuiltLoaderVersion> loaders = service.fetchLoaderVersions("1.21.4");
            assertEquals(2, loaders.size());
            assertEquals("0.27.1-beta.4", loaders.getFirst().version());
            assertEquals(32, loaders.getFirst().build());
            assertEquals("0.27.0", loaders.get(1).version());

            assertTrue(service.fetchLoaderProfile("1.21.4", "0.27.1-beta.4").contains("KnotClient"));

            List<QuiltApiVersion> apis = service.fetchApiVersions("1.21.4");
            assertEquals(1, apis.size());
            assertEquals("0.92.5+1.21.4", apis.getFirst().versionNumber());
            assertEquals("api-sha1", apis.getFirst().sha1());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sortsLoaderVersionsSemanticallyNewestFirst() throws Exception {
        // Quilt Meta 的返回顺序无规律，build 是各版本自身的构建号，只能按语义化版本排序。
        String loaderJson = """
                [
                  {"loader":{"version":"0.20.0-beta.9","build":9}},
                  {"loader":{"version":"0.30.0-beta.8","build":8}},
                  {"loader":{"version":"0.25.0","build":0}},
                  {"loader":{"version":"0.29.2-beta.5","build":5}},
                  {"loader":{"version":"0.25.0-beta.10","build":10}}
                ]
                """;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/versions/loader/1.21.4", exchange -> respond(exchange, loaderJson));
        server.start();

        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            QuiltService service = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    base,
                    base
            );

            List<String> versions = service.fetchLoaderVersions("1.21.4").stream()
                    .map(QuiltLoaderVersion::version)
                    .toList();

            assertEquals(
                    List.of("0.30.0-beta.8", "0.29.2-beta.5", "0.25.0",
                            "0.25.0-beta.10", "0.20.0-beta.9"),
                    versions);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void returnsEmptyLoaderListWhenQuiltDoesNotKnowGameVersion() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/versions/loader/1.9.9", exchange -> {
            byte[] bytes = "{\"code\":\"not_found\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();

        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            QuiltService service = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    base,
                    base
            );

            assertTrue(service.fetchLoaderVersions("1.9.9").isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void comparesLoaderVersionsSemantically() {
        assertTrue(QuiltService.compareVersions("0.29.2", "0.29.2-beta.5") > 0);
        assertTrue(QuiltService.compareVersions("0.20.0-beta.9", "0.20.0-beta.10") < 0);
        assertTrue(QuiltService.compareVersions("0.30.0", "0.9.0") > 0);
        assertEquals(0, QuiltService.compareVersions("0.27.1", "0.27.1"));
    }

    @Test
    void fetchesLoaderVersionsFromMirror() throws Exception {
        String loaderJson = """
                [
                  {"loader":{"maven":"org.quiltmc:quilt-loader:0.27.1-beta.4",
                             "version":"0.27.1-beta.4","build":32}}
                ]
                """;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/quilt-meta/v3/versions/loader/1.21.4",
                exchange -> respond(exchange, loaderJson));
        server.start();

        try {
            QuiltService service = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    URI.create("https://meta.quiltmc.org/"),
                    URI.create("https://api.modrinth.com/")
            );
            service.setSources(List.of(new MirrorDownloadSource(serverRoot(server))));

            List<QuiltLoaderVersion> loaders = service.fetchLoaderVersions("1.21.4");

            assertEquals(1, loaders.size());
            assertEquals("0.27.1-beta.4", loaders.getFirst().version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackToNextSourceWhenMirrorReturnsEmptyLoaderList() throws Exception {
        String loaderJson = """
                [
                  {"loader":{"version":"0.27.1-beta.4","build":32}}
                ]
                """;

        HttpServer emptyServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        emptyServer.createContext("/quilt-meta/v3/versions/loader/1.21.4",
                exchange -> respond(exchange, "[]"));
        emptyServer.start();
        HttpServer dataServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        dataServer.createContext("/quilt-meta/v3/versions/loader/1.21.4",
                exchange -> respond(exchange, loaderJson));
        dataServer.start();

        try {
            QuiltService service = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    URI.create("https://meta.quiltmc.org/"),
                    URI.create("https://api.modrinth.com/")
            );
            service.setSources(List.of(
                    new MirrorDownloadSource(serverRoot(emptyServer)),
                    new MirrorDownloadSource(serverRoot(dataServer))
            ));

            List<QuiltLoaderVersion> loaders = service.fetchLoaderVersions("1.21.4");

            assertEquals(1, loaders.size());
            assertEquals("0.27.1-beta.4", loaders.getFirst().version());
        } finally {
            emptyServer.stop(0);
            dataServer.stop(0);
        }
    }

    @Test
    void fallsBackWhenLoaderProfileIsInvalid() throws Exception {
        String profileJson = """
                {"id":"quilt-loader-0.27.1-beta.4-1.21.4","inheritsFrom":"1.21.4",
                 "mainClass":"org.quiltmc.loader.impl.launch.knot.KnotClient"}
                """;
        HttpServer emptyServer = profileServer("{}");
        HttpServer dataServer = profileServer(profileJson);

        try {
            QuiltService service = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    URI.create("https://meta.quiltmc.org/"),
                    URI.create("https://api.modrinth.com/")
            );
            service.setSources(List.of(rewriteTo(emptyServer), rewriteTo(dataServer)));

            String profile = service.fetchLoaderProfile("1.21.4", "0.27.1-beta.4");

            assertTrue(profile.contains("KnotClient"));
        } finally {
            emptyServer.stop(0);
            dataServer.stop(0);
        }
    }

    private static HttpServer profileServer(String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/versions/loader/1.21.4/0.27.1-beta.4/profile/json",
                exchange -> respond(exchange, body));
        server.start();
        return server;
    }

    private static DownloadSource rewriteTo(HttpServer server) {
        String root = serverRoot(server);
        return new DownloadSource() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public List<URI> candidates(URI original) {
                String base = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
                String query = original.getRawQuery() == null ? "" : "?" + original.getRawQuery();
                return List.of(URI.create(base + original.getRawPath() + query));
            }
        };
    }

    private static String serverRoot(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String content)
            throws java.io.IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}