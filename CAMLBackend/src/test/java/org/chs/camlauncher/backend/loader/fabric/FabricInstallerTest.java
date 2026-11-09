package org.chs.camlauncher.backend.loader.fabric;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricInstallerTest {
    @TempDir
    Path gameDirectory;

    @Test
    void installsFabricProfileAndApiIntoIsolatedInstance() throws Exception {
        byte[] libraryContent = "fabric-library".getBytes(StandardCharsets.UTF_8);
        byte[] apiContent = "fabric-api".getBytes(StandardCharsets.UTF_8);
        String librarySha1 = sha1(libraryContent);
        String apiSha1 = sha1(apiContent);

        Path baseDirectory = gameDirectory.resolve("versions/1.21.4");
        Files.createDirectories(baseDirectory);
        Files.writeString(baseDirectory.resolve("1.21.4.json"), """
                {"id":"1.21.4","mainClass":"net.minecraft.client.main.Main"}
                """);
        Files.write(baseDirectory.resolve("1.21.4.jar"), new byte[]{1});

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port;
        String profileJson = """
                {
                  "id": "fabric-loader-test-1.21.4",
                  "inheritsFrom": "1.21.4",
                  "mainClass": "net.fabricmc.loader.impl.launch.knot.KnotClient",
                  "arguments": {"game": [], "jvm": ["-DFabricMcEmu= net.minecraft.client.main.Main "]},
                  "libraries": [{
                    "name": "org.example:fabric-lib:1.0",
                    "url": "%s/maven/",
                    "sha1": "%s",
                    "size": %d
                  }]
                }
                """.formatted(baseUrl, librarySha1, libraryContent.length);

        server.createContext("/v2/versions/loader/1.21.4/test-loader/profile/json",
                exchange -> respond(exchange, profileJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/maven/org/example/fabric-lib/1.0/fabric-lib-1.0.jar",
                exchange -> respond(exchange, libraryContent));
        server.createContext("/fabric-api.jar",
                exchange -> respond(exchange, apiContent));
        server.start();

        try {
            URI serverBase = URI.create(baseUrl + "/");
            FabricService fabricService = new FabricService(
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
            FabricInstaller installer = new FabricInstaller(
                    versionInstaller,
                    fabricService,
                    new FileDownloader(),
                    URI.create(baseUrl + "/version_manifest_v2.json"),
                    gameDirectory.resolve("cache/version_manifest_v2.json")
            );
            FabricApiVersion api = new FabricApiVersion(
                    "1.0.0",
                    "Fabric API",
                    "fabric-api.jar",
                    URI.create(baseUrl + "/fabric-api.jar"),
                    apiSha1,
                    apiContent.length,
                    "2025-01-01T00:00:00Z"
            );

            InstalledVersion installed = installer.install(
                    "1.21.4",
                    "fabric-test",
                    "test-loader",
                    api,
                    gameDirectory,
                    DownloadListener.NONE
            );

            assertEquals("fabric-test", installed.id());
            assertEquals("1.21.4", installed.manifest().clientJarId());
            assertTrue(Files.isRegularFile(
                    gameDirectory.resolve("versions/fabric-test/fabric-test.json")));
            assertArrayEquals(libraryContent, Files.readAllBytes(
                    gameDirectory.resolve(
                            "libraries/org/example/fabric-lib/1.0/fabric-lib-1.0.jar")));
            assertArrayEquals(apiContent, Files.readAllBytes(
                    gameDirectory.resolve("versions/fabric-test/mods/fabric-api.jar")));
            assertArrayEquals(apiContent, Files.readAllBytes(
                    gameDirectory.resolve("mods/fabric-api.jar")));

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
                    "net.fabricmc.loader.impl.launch.knot.KnotClient",
                    plan.manifest().mainClass()
            );
            assertTrue(classpath.contains(
                    gameDirectory.resolve(
                            "libraries/org/example/fabric-lib/1.0/fabric-lib-1.0.jar")
                            .toAbsolutePath().normalize()));
            assertTrue(classpath.contains(
                    gameDirectory.resolve("versions/1.21.4/1.21.4.jar")
                            .toAbsolutePath().normalize()));
        } finally {
            server.stop(0);
        }
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
