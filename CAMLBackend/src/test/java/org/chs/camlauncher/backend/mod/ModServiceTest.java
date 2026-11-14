package org.chs.camlauncher.backend.mod;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.mod.model.ModDependencyType;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.mod.service.ModService;
import org.chs.camlauncher.backend.mod.source.CurseForgeClient;
import org.chs.camlauncher.backend.mod.source.ModrinthClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void searchesModrinthAndLoadsDependencies() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/search", exchange -> respond(exchange, """
                {"hits":[{"project_id":"sodium","slug":"sodium","author":"jellysquid3",
                "title":"Sodium","description":"Fast renderer","categories":["fabric","optimization"],
                "versions":["1.20.1"],"downloads":1000,"date_modified":"2026-01-01T00:00:00Z"}],
                "offset":0,"limit":20,"total_hits":1}
                """));
        server.createContext("/v2/project/sodium", exchange -> respond(exchange, """
                {"id":"sodium","slug":"sodium","title":"Sodium","description":"Fast renderer",
                "body":"Body","icon_url":"","downloads":1000,"date_modified":"2026-01-01T00:00:00Z",
                "categories":["fabric"],"game_versions":["1.20.1"],"loaders":["fabric"]}
                """));
        server.createContext("/v2/project/sodium/version", exchange -> respond(exchange, """
                [{"id":"ver1","project_id":"sodium","name":"Sodium 1","version_number":"1.0",
                "game_versions":["1.20.1"],"loaders":["fabric"],"date_published":"2026-01-01T00:00:00Z",
                "downloads":10,"dependencies":[{"project_id":"fabric-api","dependency_type":"required"}],
                "files":[{"url":"http://example.invalid/sodium.jar","filename":"sodium.jar","primary":true,
                "size":123,"hashes":{"sha1":"abc"}}]}]
                """));
        server.createContext("/v2/project/fabric-api", exchange -> respond(exchange, """
                {"id":"fabric-api","slug":"fabric-api","title":"Fabric API","description":"",
                "downloads":1,"categories":[]}
                """));
        server.start();

        try {
            String base = serverRoot(server) + "v2/";
            ModService service = service(base, base, "");
            ModSearchPage page = service.search(new ModSearchQuery(
                    "sodium", ModSource.MODRINTH, "1.20.1", null, 0, 20));
            assertEquals(1, page.items().size());
            assertEquals("Sodium", page.items().getFirst().name());

            ModProjectDetail detail = service.project(ModSource.MODRINTH, "sodium");
            assertEquals(1, detail.files().size());
            assertEquals("Fabric API", detail.files().getFirst().dependencies().getFirst().name());
            assertEquals(ModDependencyType.REQUIRED,
                    detail.files().getFirst().dependencies().getFirst().type());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void parsesCurseForgeSearchAndDependencies() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/mods/search", exchange -> respond(exchange, """
                {"data":[{"id":123,"name":"JEI","slug":"jei","summary":"Items",
                "downloadCount":20,"dateModified":"2026-01-01T00:00:00Z",
                "links":{"websiteUrl":"https://example.invalid/jei"},
                "logo":{"thumbnailUrl":""},"authors":[{"name":"mezz"}],
                "categories":[{"name":"API"}],"latestFilesIndexes":[
                {"gameVersion":"1.20.1","modLoader":4}]}],
                "pagination":{"index":0,"pageSize":20,"resultCount":1,"totalCount":1}}
                """));
        server.createContext("/v1/mods/123", exchange -> respond(exchange, """
                {"data":{"id":123,"name":"JEI","slug":"jei","summary":"Items",
                "downloadCount":20,"dateModified":"2026-01-01T00:00:00Z",
                "links":{"websiteUrl":"https://example.invalid/jei"},
                "logo":{"thumbnailUrl":""},"authors":[{"name":"mezz"}],
                "categories":[{"name":"API"}],"latestFilesIndexes":[]}}
                """));
        server.createContext("/v1/mods/123/files", exchange -> respond(exchange, """
                {"data":[{"id":9,"modId":123,"displayName":"JEI 1","fileName":"jei.jar",
                "fileDate":"2026-01-01T00:00:00Z","fileLength":100,"downloadCount":5,
                "downloadUrl":"http://example.invalid/jei.jar","gameVersions":["1.20.1","Fabric"],
                "hashes":[{"value":"abc","algo":1}],"dependencies":[{"modId":456,"relationType":3}]}],
                "pagination":{"index":0,"pageSize":50,"resultCount":1,"totalCount":1}}
                """));
        server.createContext("/v1/mods", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            respond(exchange, "{\"data\":[{\"id\":456,\"name\":\"Dependency Mod\"}]}");
        });
        server.start();

        try {
            String base = serverRoot(server) + "v1/";
            ModService service = service(base, base, "test-key");
            ModSearchPage page = service.search(new ModSearchQuery(
                    "jei", ModSource.CURSEFORGE, "1.20.1", null, 0, 20));
            assertEquals(1, page.items().size());
            assertEquals("JEI", page.items().getFirst().name());

            ModProjectDetail detail = service.project(ModSource.CURSEFORGE, "123");
            assertEquals(1, detail.files().size());
            ModFile file = detail.files().getFirst();
            assertEquals("fabric", file.loaders().getFirst());
            assertEquals("Dependency Mod", file.dependencies().getFirst().name());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generatesCurseForgeFallbackUrlsAndDeduplicatesFiles() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/mods/789", exchange -> respond(exchange, """
                {"data":{"id":789,"name":"Fallback","slug":"fallback","summary":"",
                "downloadCount":0,"dateModified":"2026-01-01T00:00:00Z",
                "links":{},"authors":[],"categories":[],"latestFilesIndexes":[]}}
                """));
        server.createContext("/v1/mods/789/files", exchange -> respond(exchange, """
                {"data":[
                {"id":1234567,"modId":789,"displayName":"Fallback 1","fileName":"fallback mod.jar",
                "fileDate":"2026-01-01T00:00:00Z","fileLength":0,"downloadCount":0,"downloadUrl":"",
                "gameVersions":["1.20.1","Fabric"],"dependencies":[],
                "hashes":[{"value":"sha1-value","algo":1},{"value":"md5-value","algo":2}]},
                {"id":1234567,"modId":789,"displayName":"Fallback 1","fileName":"fallback mod.jar",
                "fileDate":"2026-01-01T00:00:00Z","fileLength":0,"downloadCount":0,"downloadUrl":"",
                "gameVersions":["1.20.1","Fabric"],"dependencies":[],
                "hashes":[{"value":"sha1-value","algo":1},{"value":"md5-value","algo":2}]},
                {"id":7654321,"modId":789,"displayName":"Legacy","fileName":"legacy.jar",
                "fileDate":"2026-01-01T00:00:00Z","fileLength":123,"downloadCount":0,
                "downloadUrl":"https://edge-service.overwolf.wtf/files/7654/321/legacy.jar",
                "gameVersions":["1.20.1","Fabric"],"dependencies":[],
                "hashes":[{"value":"sha1-legacy","algo":1}]}
                ],"pagination":{"index":0,"pageSize":50,"resultCount":3,"totalCount":3}}
                """));
        server.start();

        try {
            String base = serverRoot(server) + "v1/";
            ModProjectDetail detail = service(base, base, "test-key")
                    .project(ModSource.CURSEFORGE, "789");

            assertEquals(2, detail.files().size(), "重复文件 ID 应只保留一项");
            ModFile fallback = detail.files().get(0);
            assertEquals("https://edge.forgecdn.net/files/1234/567/fallback%20mod.jar",
                    fallback.downloadUris().getFirst().toString());
            assertTrue(fallback.downloadUris().stream().anyMatch(uri ->
                    uri.toString().contains("mediafilez.forgecdn.net")));
            assertEquals("sha1-value", fallback.sha1());
            assertEquals("md5-value", fallback.md5());
            assertEquals(-1, fallback.size(), "fileLength=0 应视为未知大小");

            ModFile legacy = detail.files().get(1);
            assertTrue(legacy.downloadUris().stream().anyMatch(uri -> uri.toString().equals(
                    "https://mediafilez.forgecdn.net/files/7654/321/legacy.jar")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadsCurseForgeFileWithApiKeyAndMd5() throws Exception {
        byte[] content = "curse-forge-file".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/secured.jar", exchange -> {
            assertEquals("test-key", exchange.getRequestHeaders().getFirst("x-api-key"));
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path gameDirectory = tempDirectory.resolve("secure-game");
            ModFile file = new ModFile(
                    ModSource.CURSEFORGE, "project", "version", "Secure Mod", "1.0",
                    "secured.jar", List.of(URI.create(serverRoot(server) + "secured.jar")),
                    null, HashVerifier.md5(content), content.length,
                    List.of("1.20.1"), List.of("fabric"), List.of(), "", 1
            );
            ModService service = service("https://example.invalid/v2/",
                    "https://example.invalid/v1/", "test-key");

            var result = service.download(
                    file, gameDirectory, "instance", VersionIsolationMode.NONE,
                    DownloadListener.NONE);

            assertArrayEquals(content, Files.readAllBytes(result.path()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsCurseForgeFileWithWrongMd5() throws Exception {
        byte[] content = "bad-md5".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bad-md5.jar", exchange -> {
            assertEquals("test-key", exchange.getRequestHeaders().getFirst("x-api-key"));
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path gameDirectory = tempDirectory.resolve("bad-md5-game");
            ModFile file = new ModFile(
                    ModSource.CURSEFORGE, "project", "version", "Bad MD5", "1.0",
                    "bad-md5.jar", List.of(URI.create(serverRoot(server) + "bad-md5.jar")),
                    null, HashVerifier.md5("different".getBytes(StandardCharsets.UTF_8)),
                    content.length, List.of("1.20.1"), List.of("fabric"), List.of(), "", 1
            );
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            ModService service = new ModService(
                    new ModrinthClient(client, URI.create("https://example.invalid/v2/")),
                    new CurseForgeClient(client, URI.create("https://example.invalid/v1/"),
                            "test-key"),
                    new FileDownloader(client, 1)
            );

            assertThrows(BackendException.class, () -> service.download(
                    file, gameDirectory, "instance", VersionIsolationMode.NONE,
                    DownloadListener.NONE));
            assertFalse(Files.exists(gameDirectory.resolve("mods/bad-md5.jar")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void curseForgeRequiresApiKey() throws Exception {
        ModService service = service("https://example.invalid/v2/",
                "https://example.invalid/v1/", "");

        BackendException exception = assertThrows(BackendException.class,
                () -> service.search(new ModSearchQuery(
                        "jei", ModSource.CURSEFORGE, "", null, 0, 20)));
        assertEquals("mod.error.curseforgeKeyMissing", exception.error().code());
    }

    @Test
    void downloadsModIntoInstanceModsDirectory() throws Exception {
        byte[] content = "mod-content".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mods/test.jar", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path gameDirectory = tempDirectory.resolve("game");
            String sha1 = HashVerifier.sha1(content);
            ModFile file = new ModFile(
                    ModSource.MODRINTH, "project", "version", "Test Mod", "1.0",
                    "test.jar", List.of(URI.create(serverRoot(server) + "mods/test.jar")),
                    sha1, null, content.length, List.of("1.20.1"), List.of("fabric"),
                    List.of(), "", 1
            );
            ModService service = service("https://example.invalid/v2/",
                    "https://example.invalid/v1/", "");

            var result = service.download(
                    file, gameDirectory, "instance", VersionIsolationMode.NONE,
                    DownloadListener.NONE);

            assertEquals(gameDirectory.resolve("mods/test.jar"), result.path());
            assertArrayEquals(content, Files.readAllBytes(result.path()));
        } finally {
            server.stop(0);
        }
    }

    private ModService service(String modrinthBase, String curseBase, String curseKey) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new ModService(
                new ModrinthClient(client, URI.create(modrinthBase)),
                new CurseForgeClient(client, URI.create(curseBase), curseKey),
                new FileDownloader()
        );
    }

    private static void respond(HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String serverRoot(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }
}
