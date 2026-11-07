package org.chs.camlauncher.backend.instance;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.OfficialDownloadSource;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionInstallerTest {
    @TempDir
    Path gameDirectory;

    @ParameterizedTest
    @CsvSource(value = {
            "1.21.5|1.21.5",
            "1.21.5|我的 1.21.5"
    }, delimiter = '|')
    void installsVanillaVersionFromRemoteMetadataAndFiles(String officialVersionId, String instanceId)
            throws Exception {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        String contentSha1 = sha1(content);
        String assetIndexJson = """
                {
                  "objects": {
                    "test": {
                      "hash": "%s",
                      "size": %d
                    }
                  }
                }
                """.formatted(contentSha1, content.length);
        byte[] assetIndexBytes = assetIndexJson.getBytes(StandardCharsets.UTF_8);
        String assetIndexSha1 = sha1(assetIndexBytes);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();

        String versionJson = """
                {
                  "id": "1.21.5",
                  "downloads": {
                    "client": {
                      "sha1": "%s",
                      "size": %d,
                      "url": "http://127.0.0.1:%d/client.jar"
                    }
                  },
                  "assetIndex": {
                    "id": "5",
                    "sha1": "%s",
                    "size": %d,
                    "totalSize": %d,
                    "url": "http://127.0.0.1:%d/assets.json"
                  },
                  "libraries": [
                    {
                      "name": "com.example:lib:1.0",
                      "downloads": {
                        "artifact": {
                          "path": "com/example/lib/1.0/lib-1.0.jar",
                          "sha1": "%s",
                          "size": %d,
                          "url": "http://127.0.0.1:%d/lib.jar"
                        }
                      }
                    }
                  ]
                }
                """.formatted(
                contentSha1, content.length, port,
                assetIndexSha1, assetIndexBytes.length, assetIndexBytes.length, port,
                contentSha1, content.length, port
        );

        String manifestJson = """
                {
                  "latest": {
                    "release": "1.21.5",
                    "snapshot": "1.21.5"
                  },
                  "versions": [
                    {
                      "id": "1.21.5",
                      "type": "release",
                      "url": "http://127.0.0.1:%d/1.21.5.json"
                    }
                  ]
                }
                """.formatted(port);

        server.createContext("/version_manifest_v2.json",
                exchange -> respond(exchange, manifestJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/1.21.5.json",
                exchange -> respond(exchange, versionJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/client.jar", exchange -> respond(exchange, content));
        server.createContext("/lib.jar", exchange -> respond(exchange, content));
        server.createContext("/assets.json", exchange -> respond(exchange, assetIndexBytes));
        server.createContext("/assets/aa/" + contentSha1, exchange -> respond(exchange, content));
        server.start();

        try {
            VersionInstaller installer = new VersionInstaller(
                    new MetadataService(),
                    new VersionManifestService(),
                    new DownloadPlanner(
                            List.of(OfficialDownloadSource.INSTANCE),
                            new RuleEvaluator("linux", "x86_64", "6.0", Map.of()),
                            "linux",
                            64
                    ),
                    new AssetDownloadPlanner(
                            List.of(OfficialDownloadSource.INSTANCE),
                            "http://127.0.0.1:" + port + "/assets/"
                    ),
                    new FileDownloader()
            );

            InstalledVersion installed = installer.install(
                    officialVersionId,
                    instanceId,
                    gameDirectory,
                    URI.create("http://127.0.0.1:" + port + "/version_manifest_v2.json"),
                    gameDirectory.resolve("cache/version_manifest_v2.json"),
                    DownloadListener.NONE
            );

            assertEquals(instanceId, installed.id());
            assertEquals(instanceId, installed.manifest().id());
            assertEquals(
                    gameDirectory.resolve("versions").resolve(instanceId).resolve(instanceId + ".json"),
                    installed.versionJson()
            );
            assertTrue(Files.isRegularFile(installed.versionJson()));
            assertArrayEquals(content, Files.readAllBytes(installed.clientJar()));
            assertArrayEquals(content, Files.readAllBytes(
                    gameDirectory.resolve("libraries/com/example/lib/1.0/lib-1.0.jar")));
            assertArrayEquals(assetIndexBytes, Files.readAllBytes(installed.assetIndexFile()));
            assertArrayEquals(content, Files.readAllBytes(
                    gameDirectory.resolve("assets/objects/aa/" + contentSha1)));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void localLoaderVersionUsesJarFieldForClientJarPath() throws Exception {
        Path parentDirectory = gameDirectory.resolve("versions/1.20.1");
        Files.createDirectories(parentDirectory);
        Files.writeString(parentDirectory.resolve("1.20.1.json"), """
                {
                  "id": "1.20.1",
                  "mainClass": "parent.Main"
                }
                """);

        Path loaderDirectory = gameDirectory.resolve("versions/forge-instance");
        Files.createDirectories(loaderDirectory);
        Files.writeString(loaderDirectory.resolve("forge-instance.json"), """
                {
                  "id": "forge-instance",
                  "inheritsFrom": "1.20.1",
                  "jar": "1.20.1",
                  "mainClass": "loader.Main"
                }
                """);

        InstalledVersion installed = new VersionInstaller().install(
                "forge-instance",
                gameDirectory,
                URI.create("https://example.invalid/version_manifest_v2.json"),
                gameDirectory.resolve("cache/version_manifest_v2.json"),
                DownloadListener.NONE
        );

        assertEquals("forge-instance", installed.id());
        assertEquals("1.20.1", installed.manifest().clientJarId());
        assertEquals(
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar").toAbsolutePath().normalize(),
                installed.clientJar().toAbsolutePath().normalize()
        );
    }

    private static void respond(HttpExchange exchange, byte[] content) throws IOException {
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static String sha1(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return HexFormat.of().formatHex(digest.digest(content));
    }
}
