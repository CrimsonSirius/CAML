package org.chs.camlauncher.backend.loader.quilt;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.launch.classpath.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.launch.LaunchPlanner;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuiltInstallerTest {
    @TempDir
    Path gameDirectory;

    @Test
    void installsQuiltProfileAndApiIntoIsolatedInstance() throws Exception {
        byte[] libraryContent = "quilt-library".getBytes(StandardCharsets.UTF_8);
        byte[] apiContent = "quilted-fabric-api".getBytes(StandardCharsets.UTF_8);
        String librarySha1 = sha1(libraryContent);
        String apiSha1 = sha1(apiContent);

        Path baseDirectory = gameDirectory.resolve("versions/1.21.4");
        Files.createDirectories(baseDirectory);
        Files.writeString(baseDirectory.resolve("1.21.4.json"), """
                {"id":"1.21.4","mainClass":"net.minecraft.client.main.Main",
                 "javaVersion":{"component":"java-runtime-gamma","majorVersion":21}}
                """);
        Files.write(baseDirectory.resolve("1.21.4.jar"), new byte[]{1});

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port;
        String profileJson = """
                {
                  "id": "quilt-loader-test-1.21.4",
                  "inheritsFrom": "1.21.4",
                  "mainClass": "org.quiltmc.loader.impl.launch.knot.KnotClient",
                  "arguments": {"game": [], "jvm": ["-Dquiltmc.emulate.fabric=true"]},
                  "libraries": [{
                    "name": "org.example:quilt-lib:1.0",
                    "url": "%s/maven/",
                    "sha1": "%s",
                    "size": %d
                  }]
                }
                """.formatted(baseUrl, librarySha1, libraryContent.length);

        server.createContext("/v3/versions/loader/1.21.4/test-loader/profile/json",
                exchange -> respond(exchange, profileJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/maven/org/example/quilt-lib/1.0/quilt-lib-1.0.jar",
                exchange -> respond(exchange, libraryContent));
        server.createContext("/quilted-fabric-api.jar",
                exchange -> respond(exchange, apiContent));
        server.start();

        try {
            URI serverBase = URI.create(baseUrl + "/");
            QuiltService quiltService = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    serverBase,
                    serverBase
            );
            VersionInstaller versionInstaller = new VersionInstaller(
                    new MetadataService(),
                    new VersionManifestService(),
                    new DownloadPlanner(),
                    new AssetDownloadPlanner(),
                    new FileDownloader()
            );
            QuiltInstaller installer = new QuiltInstaller(
                    versionInstaller,
                    quiltService,
                    new FileDownloader(),
                    URI.create(baseUrl + "/version_manifest_v2.json"),
                    gameDirectory.resolve("cache/version_manifest_v2.json")
            );
            QuiltApiVersion api = new QuiltApiVersion(
                    "1.0.0",
                    "Quilted Fabric API",
                    "quilted-fabric-api.jar",
                    URI.create(baseUrl + "/quilted-fabric-api.jar"),
                    apiSha1,
                    apiContent.length,
                    "2025-01-01T00:00:00Z"
            );

            InstalledVersion installed = installer.install(
                    "1.21.4",
                    "quilt-test",
                    "test-loader",
                    api,
                    gameDirectory,
                    DownloadListener.NONE
            );

            assertEquals("quilt-test", installed.id());
            assertEquals("1.21.4", installed.manifest().clientJarId());
            assertTrue(Files.isRegularFile(
                    gameDirectory.resolve("versions/quilt-test/quilt-test.json")));
            assertArrayEquals(libraryContent, Files.readAllBytes(
                    gameDirectory.resolve(
                            "libraries/org/example/quilt-lib/1.0/quilt-lib-1.0.jar")));
            assertArrayEquals(apiContent, Files.readAllBytes(
                    gameDirectory.resolve("versions/quilt-test/mods/quilted-fabric-api.jar")));
            assertArrayEquals(apiContent, Files.readAllBytes(
                    gameDirectory.resolve("mods/quilted-fabric-api.jar")));

            LaunchPlan plan = new LaunchPlanner().create(
                    installed,
                    new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}"),
                    new JavaRuntime(Path.of("/tmp/java"), "21.0", 21, "test", "x86_64", 64, true),
                    LaunchOptions.defaults(),
                    gameDirectory
            );
            var classpath = new ClasspathBuilder(
                    new RuleEvaluator("linux", "x86_64", "6.0", Map.of())
            ).build(plan);

            assertEquals(
                    "org.quiltmc.loader.impl.launch.knot.KnotClient",
                    plan.manifest().mainClass()
            );
            assertTrue(classpath.contains(
                    gameDirectory.resolve(
                            "libraries/org/example/quilt-lib/1.0/quilt-lib-1.0.jar")
                            .toAbsolutePath().normalize()));
            assertTrue(classpath.contains(
                    gameDirectory.resolve("versions/1.21.4/1.21.4.jar")
                            .toAbsolutePath().normalize()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rollsBackProfileLibrariesAndVersionStageWhenApiDownloadFails() throws Exception {
        byte[] libraryContent = "quilt-library".getBytes(StandardCharsets.UTF_8);
        String librarySha1 = sha1(libraryContent);

        Path baseDirectory = gameDirectory.resolve("versions/1.21.4");
        Files.createDirectories(baseDirectory);
        Files.writeString(baseDirectory.resolve("1.21.4.json"), """
                {"id":"1.21.4","mainClass":"net.minecraft.client.main.Main",
                 "javaVersion":{"component":"java-runtime-gamma","majorVersion":21}}
                """);
        Files.write(baseDirectory.resolve("1.21.4.jar"), new byte[]{1});
        Path existingLibrary = gameDirectory.resolve(
                "libraries/org/example/quilt-lib/1.0/quilt-lib-1.0.jar");
        Files.createDirectories(existingLibrary.getParent());
        Files.writeString(existingLibrary, "old", StandardCharsets.UTF_8);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port;
        String profileJson = """
                {
                  "id": "quilt-loader-test-1.21.4",
                  "inheritsFrom": "1.21.4",
                  "mainClass": "org.quiltmc.loader.impl.launch.knot.KnotClient",
                  "libraries": [{
                    "name": "org.example:quilt-lib:1.0",
                    "url": "%s/maven/",
                    "sha1": "%s",
                    "size": %d
                  }]
                }
                """.formatted(baseUrl, librarySha1, libraryContent.length);

        server.createContext("/v3/versions/loader/1.21.4/test-loader/profile/json",
                exchange -> respond(exchange, profileJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/maven/org/example/quilt-lib/1.0/quilt-lib-1.0.jar",
                exchange -> respond(exchange, libraryContent));
        server.createContext("/quilted-fabric-api.jar", exchange -> {
            byte[] invalid = "invalid-quilt-api".getBytes(StandardCharsets.UTF_8);
            respond(exchange, invalid);
        });
        server.start();

        try {
            URI serverBase = URI.create(baseUrl + "/");
            QuiltService quiltService = new QuiltService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    serverBase,
                    serverBase
            );
            VersionInstaller versionInstaller = new VersionInstaller(
                    new MetadataService(),
                    new VersionManifestService(),
                    new DownloadPlanner(),
                    new AssetDownloadPlanner(),
                    new FileDownloader()
            );
            QuiltInstaller installer = new QuiltInstaller(
                    versionInstaller,
                    quiltService,
                    new FileDownloader(),
                    URI.create(baseUrl + "/version_manifest_v2.json"),
                    gameDirectory.resolve("cache/version_manifest_v2.json")
            );
            QuiltApiVersion api = new QuiltApiVersion(
                    "1.0.0",
                    "Quilted Fabric API",
                    "quilted-fabric-api.jar",
                    URI.create(baseUrl + "/quilted-fabric-api.jar"),
                    "0000000000000000000000000000000000000000",
                    16,
                    "2025-01-01T00:00:00Z"
            );

            assertThrows(IOException.class, () -> installer.install(
                    "1.21.4",
                    "quilt-test",
                    "test-loader",
                    api,
                    gameDirectory,
                    DownloadListener.NONE
            ));

            assertFalse(Files.exists(gameDirectory.resolve("versions/quilt-test")));
            assertEquals("old", Files.readString(existingLibrary));
            assertFalse(Files.exists(gameDirectory.resolve("mods/quilted-fabric-api.jar")));
            try (var versions = Files.list(gameDirectory.resolve("versions"))) {
                assertFalse(versions.anyMatch(path -> path.getFileName().toString()
                        .startsWith(".caml-version-install-")));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsInstanceNameEqualToGameVersion() {
        QuiltInstaller installer = new QuiltInstaller(
                new VersionInstaller(
                        new MetadataService(),
                        new VersionManifestService(),
                        new DownloadPlanner(),
                        new AssetDownloadPlanner(),
                        new FileDownloader()
                ),
                new QuiltService(),
                new FileDownloader(),
                URI.create("http://127.0.0.1/version_manifest_v2.json"),
                gameDirectory.resolve("cache/version_manifest_v2.json")
        );

        var error = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> installer.install("1.21.4", "1.21.4", "test-loader", null,
                        gameDirectory, DownloadListener.NONE));

        assertTrue(error.getMessage().contains("Quilt"));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] content)
            throws java.io.IOException {
        exchange.sendResponseHeaders(200, content.length);
        try (var output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static String sha1(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(content));
    }
}

