package org.chs.camlauncher.backend.loader;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalLoaderInstallerTest {
    @TempDir
    Path gameDirectory;

    @ParameterizedTest
    @ValueSource(strings = {"my-forge", "test-forge-id"})
    void installsThroughExternalInstallerAndRenamesInstance(String instanceId) throws Exception {
        Path baseDirectory = gameDirectory.resolve("versions/1.20.1");
        Files.createDirectories(baseDirectory);
        // 假安装器用 Java 25 编译，必须显式要求 Java 25，避免 CI 上选中旧 JDK。
        Files.writeString(baseDirectory.resolve("1.20.1.json"), """
                {
                  "id": "1.20.1",
                  "mainClass": "base.Main",
                  "javaVersion": {"component": "test", "majorVersion": 25}
                }
                """, StandardCharsets.UTF_8);
        Files.write(baseDirectory.resolve("1.20.1.jar"), new byte[]{1});

        Path installerJar = gameDirectory.resolve("fake-installer.jar");
        createFakeInstallerJar(installerJar);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/installer.jar", exchange -> {
            byte[] bytes = Files.readAllBytes(installerJar);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();

        try {
            ExternalLoaderInstaller installer = new ExternalLoaderInstaller(
                    new VersionInstaller(
                            new MetadataService(),
                            new VersionManifestService(),
                            new DownloadPlanner(),
                            new AssetDownloadPlanner(),
                            new FileDownloader()
                    ),
                    new JavaLocator(),
                    new FileDownloader(),
                    new MetadataService(),
                    URI.create("https://example.invalid/version_manifest_v2.json"),
                    gameDirectory.resolve("cache/version_manifest_v2.json")
            );

            InstalledVersion installed = installer.install(
                    "Forge",
                    "1.20.1",
                    instanceId,
                    URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/installer.jar"),
                    gameDirectory,
                    DownloadListener.NONE
            );

            assertEquals(instanceId, installed.id());
            assertEquals("test.ForgeMain", installed.manifest().mainClass());
            assertEquals("1.20.1", installed.manifest().clientJarId());
            assertTrue(Files.isRegularFile(
                    gameDirectory.resolve("versions").resolve(instanceId).resolve(instanceId + ".json")));
            assertTrue(Files.isRegularFile(gameDirectory.resolve(
                    "libraries/org/example/loader-lib/1.0/loader-lib-1.0.jar")));
            try (var entries = Files.list(gameDirectory)) {
                assertFalse(entries.anyMatch(path ->
                        path.getFileName().toString().startsWith(".caml-forge-install-")));
            }
        } finally {
            server.stop(0);
        }
    }

    @org.junit.jupiter.api.Test
    void rejectsInstalledResultWithMissingRequiredLibrary() throws Exception {
        VersionInfo manifest = new MetadataService().parseVersionInfo("""
                {
                  "id": "forge-instance",
                  "mainClass": "example.Main",
                  "libraries": [{
                    "name": "org.example:missing:1.0",
                    "downloads": {
                      "artifact": {
                        "path": "org/example/missing/1.0/missing-1.0.jar"
                      }
                    }
                  }]
                }
                """);
        InstalledVersion installed = new InstalledVersion(
                "forge-instance",
                manifest,
                gameDirectory,
                gameDirectory.resolve("versions/forge-instance"),
                gameDirectory.resolve("versions/forge-instance/forge-instance.json"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar"),
                gameDirectory.resolve("libraries"),
                gameDirectory.resolve("assets"),
                null
        );

        assertThrows(IOException.class,
                () -> ExternalLoaderInstaller.verifyInstalledLibraries("Forge", installed));
    }

    @org.junit.jupiter.api.Test
    void predownloadsInstallerLibrariesFromConfiguredSources() throws Exception {
        Path baseDirectory = gameDirectory.resolve("versions/1.20.1");
        Files.createDirectories(baseDirectory);
        // 假安装器用 Java 25 编译，必须显式要求 Java 25，避免 CI 上选中旧 JDK。
        Files.writeString(baseDirectory.resolve("1.20.1.json"), """
                {
                  "id": "1.20.1",
                  "mainClass": "base.Main",
                  "javaVersion": {"component": "test", "majorVersion": 25}
                }
                """, StandardCharsets.UTF_8);
        Files.write(baseDirectory.resolve("1.20.1.jar"), new byte[]{1});

        byte[] libraryBytes = "pre-downloaded-library".getBytes(StandardCharsets.UTF_8);
        Path installerJar = gameDirectory.resolve("fake-installer.jar");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/installer.jar", exchange -> {
            byte[] bytes = Files.readAllBytes(installerJar);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.createContext("/lib.jar", exchange -> {
            exchange.sendResponseHeaders(200, libraryBytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(libraryBytes);
            }
        });
        server.start();

        try {
            String libraryUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/lib.jar";
            createFakeInstallerJar(installerJar, """
                    [{
                      "name": "org.example:pre:1.0",
                      "downloads": {
                        "artifact": {
                          "url": "%s",
                          "path": "org/example/pre/1.0/pre-1.0.jar",
                          "size": %d
                        }
                      }
                    }]
                    """.formatted(libraryUrl, libraryBytes.length));

            ExternalLoaderInstaller installer = new ExternalLoaderInstaller(
                    new VersionInstaller(
                            new MetadataService(),
                            new VersionManifestService(),
                            new DownloadPlanner(),
                            new AssetDownloadPlanner(),
                            new FileDownloader()
                    ),
                    new JavaLocator(),
                    new FileDownloader(),
                    new MetadataService(),
                    URI.create("https://example.invalid/version_manifest_v2.json"),
                    gameDirectory.resolve("cache/version_manifest_v2.json")
            );
            installer.install(
                    "Forge",
                    "1.20.1",
                    "pre-download-instance",
                    URI.create("http://127.0.0.1:"
                            + server.getAddress().getPort() + "/installer.jar"),
                    gameDirectory,
                    DownloadListener.NONE
            );

            Path library = gameDirectory.resolve(
                    "libraries/org/example/pre/1.0/pre-1.0.jar");
            assertTrue(Files.isRegularFile(library));
            assertTrue(java.util.Arrays.equals(libraryBytes, Files.readAllBytes(library)));
        } finally {
            server.stop(0);
        }
    }

    private static void createFakeInstallerJar(Path jar) throws Exception {
        createFakeInstallerJar(jar, null);
    }

    private static void createFakeInstallerJar(Path jar, String librariesJson) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,
                FakeForgeInstallerMain.class.getName());

        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            String classPath = FakeForgeInstallerMain.class.getName().replace('.', '/') + ".class";
            output.putNextEntry(new JarEntry(classPath));
            try (InputStream input = FakeForgeInstallerMain.class.getClassLoader()
                    .getResourceAsStream(classPath)) {
                if (input == null) {
                    throw new IllegalStateException("Missing " + classPath);
                }
                input.transferTo(output);
            }
            output.closeEntry();

            output.putNextEntry(new JarEntry("version.json"));
            output.write(("""
                    {
                      "id": "test-forge-id",
                      "inheritsFrom": "1.20.1",
                      "mainClass": "test.ForgeMain",
                      "type": "release"%s
                    }
                    """.formatted(librariesJson == null
                    ? ""
                    : ",\n  \"libraries\": " + librariesJson))
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
