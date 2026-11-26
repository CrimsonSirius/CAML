package org.chs.camlauncher.backend.modpack;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.ArchiveExtractionGuard;
import org.chs.camlauncher.backend.loader.FakeForgeInstallerMain;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricService;
import org.chs.camlauncher.backend.loader.quilt.QuiltInstaller;
import org.chs.camlauncher.backend.loader.quilt.QuiltService;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModpackInstallerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsInvalidArchiveAndCleansTemporaryInstance() throws Exception {
        byte[] content = "not a zip".getBytes(StandardCharsets.UTF_8);
        HttpServer server = serve(content);
        try {
            Path gameDirectory = temporaryDirectory.resolve("game");
            ModFile file = modFile(server, "bad.mrpack", content);

            BackendException error = assertThrows(BackendException.class,
                    () -> installer().install(file, gameDirectory, "Bad Pack",
                            DownloadListener.NONE, new DownloadCancellationToken()));

            assertEquals("modpack.error.invalidArchive", error.error().code());
            assertFalse(Files.exists(gameDirectory.resolve("versions/Bad Pack")));
            try (var files = Files.list(gameDirectory)) {
                assertFalse(files.anyMatch(path -> path.getFileName().toString()
                        .startsWith(".caml-modpack-")));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsModpackWithoutMinecraftDependency() throws Exception {
        byte[] content = zip("""
                {"name":"Missing MC","versionId":"1","dependencies":{},"files":[]}
                """);
        HttpServer server = serve(content);
        try {
            Path gameDirectory = temporaryDirectory.resolve("game-missing-minecraft");
            ModFile file = modFile(server, "missing.mrpack", content);

            BackendException error = assertThrows(BackendException.class,
                    () -> installer().install(file, gameDirectory, "Missing MC",
                            DownloadListener.NONE, new DownloadCancellationToken()));

            assertEquals("modpack.error.missingMinecraft", error.error().code());
            assertFalse(Files.exists(gameDirectory.resolve("versions/Missing MC")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rollsBackLoaderLibrariesWhenModpackContentFails() throws Exception {
        Path gameDirectory = temporaryDirectory.resolve("game-forge-rollback");
        Path baseDirectory = gameDirectory.resolve("versions/1.20.1");
        Files.createDirectories(baseDirectory);
        Files.writeString(baseDirectory.resolve("1.20.1.json"), """
                {
                  "id": "1.20.1",
                  "mainClass": "base.Main",
                  "javaVersion": {"component": "test", "majorVersion": 25}
                }
                """, StandardCharsets.UTF_8);

        Path installerJar = temporaryDirectory.resolve("fake-forge-installer.jar");
        createFakeInstallerJar(installerJar);
        byte[] installerSha1 = HashVerifier.sha1(installerJar)
                .getBytes(StandardCharsets.UTF_8);

        byte[] modpack = zip("""
                {
                  "name":"Rollback Pack",
                  "versionId":"1",
                  "dependencies":{"minecraft":"1.20.1","forge":"47.2.0"},
                  "files":[{
                    "path":"../evil.jar",
                    "downloads":["http://127.0.0.1/evil.jar"],
                    "hashes":{"sha1":"abc"},
                    "fileSize":1,
                    "env":{"client":"required"}
                  }]
                }
                """);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/installer.jar", exchange -> respond(exchange,
                Files.readAllBytes(installerJar)));
        server.createContext("/installer.jar.sha1", exchange -> respond(exchange, installerSha1));
        server.createContext("/rollback.mrpack", exchange -> respond(exchange, modpack));
        server.start();

        try {
            VersionInstaller versionInstaller = new VersionInstaller(
                    new MetadataService(),
                    new VersionManifestService(),
                    new DownloadPlanner(),
                    new AssetDownloadPlanner(),
                    new FileDownloader()
            );
            FileDownloader downloader = new FileDownloader();
            URI manifest = URI.create("https://example.invalid/version_manifest_v2.json");
            Path cache = temporaryDirectory.resolve("rollback-manifest.json");
            ExternalLoaderInstaller external = new ExternalLoaderInstaller(
                    versionInstaller, new JavaLocator(), downloader,
                    new MetadataService(), manifest, cache);
            String forgeInstaller = "https://maven.minecraftforge.net/net/minecraftforge/forge/"
                    + "1.20.1-47.2.0/forge-1.20.1-47.2.0-installer.jar";
            String localInstaller = "http://127.0.0.1:"
                    + server.getAddress().getPort() + "/installer.jar";
            DownloadSource localSource = new DownloadSource() {
                @Override
                public String name() {
                    return "test";
                }

                @Override
                public List<URI> candidates(URI original) {
                    String value = original.toString();
                    if (value.equals(forgeInstaller) || value.equals(forgeInstaller + ".sha1")) {
                        return List.of(URI.create(localInstaller + (value.endsWith(".sha1")
                                ? ".sha1" : "")));
                    }
                    return List.of();
                }
            };
            external.setSources(List.of(localSource));

            ModpackInstaller modpackInstaller = new ModpackInstaller(
                    versionInstaller,
                    new FabricInstaller(versionInstaller, new FabricService(), downloader, manifest, cache),
                    new QuiltInstaller(versionInstaller, new QuiltService(), downloader, manifest, cache),
                    external,
                    downloader,
                    manifest,
                    cache
            );
            ModFile file = modFile(server, "rollback.mrpack", modpack);

            assertThrows(BackendException.class, () -> modpackInstaller.install(
                    file, gameDirectory, "Rollback Pack",
                    DownloadListener.NONE, new DownloadCancellationToken()));

            assertFalse(Files.exists(gameDirectory.resolve(
                    "libraries/org/example/loader-lib/1.0/loader-lib-1.0.jar")));
            assertFalse(Files.exists(gameDirectory.resolve("versions/Rollback Pack")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsModpackFileWithoutSha1OrSize() throws Exception {
        ModrinthModpackIndex index = new ModrinthModpackIndex(
                "Invalid", "1", "", Map.of("minecraft", "1.20.1"),
                List.of(new ModrinthModpackIndex.File(
                        "mods/example.jar",
                        List.of(URI.create("http://127.0.0.1/example.jar")),
                        "", 0, "required")));

        assertThrows(BackendException.class, () -> installer().downloadFiles(
                index, temporaryDirectory.resolve("content"), "Invalid",
                DownloadListener.NONE, new DownloadCancellationToken()));
    }

    @Test
    void rejectsDuplicateModpackFilePaths() throws Exception {
        ModrinthModpackIndex.File file = new ModrinthModpackIndex.File(
                "mods/example.jar",
                List.of(URI.create("http://127.0.0.1/example.jar")),
                "abc", 1, "required");
        ModrinthModpackIndex index = new ModrinthModpackIndex(
                "Duplicate", "1", "", Map.of("minecraft", "1.20.1"),
                List.of(file, file));

        assertThrows(BackendException.class, () -> installer().downloadFiles(
                index, temporaryDirectory.resolve("content-duplicate"), "Duplicate",
                DownloadListener.NONE, new DownloadCancellationToken()));
    }

    @Test
    void rejectsModpackFileThatConflictsWithOverride() throws Exception {
        Path archive = writeZip("override-conflict.mrpack", zipEntries(ordered(
                "modrinth.index.json", "{}".getBytes(StandardCharsets.UTF_8),
                "overrides/mods/example.jar", "override".getBytes(StandardCharsets.UTF_8)
        )));
        Path instance = temporaryDirectory.resolve("content-override");
        ModpackInstaller installer = installer();
        installer.extractOverrides(archive, instance, "Conflict.mrpack");
        ModrinthModpackIndex index = new ModrinthModpackIndex(
                "Conflict", "1", "", Map.of("minecraft", "1.20.1"),
                List.of(new ModrinthModpackIndex.File(
                        "mods/example.jar",
                        List.of(URI.create("http://127.0.0.1/example.jar")),
                        "abc", 1, "required")));

        assertThrows(BackendException.class, () -> installer.downloadFiles(
                index, instance, "Conflict.mrpack",
                DownloadListener.NONE, new DownloadCancellationToken()));
    }

    @Test
    void rejectsDuplicateOverridePaths() throws Exception {
        Path archive = writeZip("duplicate-override.mrpack", zipEntriesWithDuplicates(
                "overrides/config/value.txt", "one".getBytes(StandardCharsets.UTF_8),
                "client-overrides/config/value.txt", "two".getBytes(StandardCharsets.UTF_8)));

        assertThrows(BackendException.class, () -> installer().extractOverrides(
                archive, temporaryDirectory.resolve("duplicate-override"), "Duplicate.mrpack"));
    }

    @Test
    void rejectsModpackArchiveEntryCountOverLimit() throws Exception {
        Path archive = writeZip("many-overrides.mrpack", zipEntries(ordered(
                "overrides/mods/a.jar", "a".getBytes(StandardCharsets.UTF_8),
                "overrides/mods/b.jar", "b".getBytes(StandardCharsets.UTF_8)
        )));

        assertThrows(BackendException.class, () -> installer(
                new ArchiveExtractionGuard(1, 1024, 1024))
                .extractOverrides(archive, temporaryDirectory.resolve("many"), "Many.mrpack"));
    }

    @Test
    void rejectsModpackOverrideOverSingleEntryLimit() throws Exception {
        Path archive = writeZip("large-override.mrpack", zipEntries(Map.of(
                "overrides/config/value.txt", "toolarge".getBytes(StandardCharsets.UTF_8)
        )));

        assertThrows(BackendException.class, () -> installer(
                new ArchiveExtractionGuard(10, 4, 1024))
                .extractOverrides(archive, temporaryDirectory.resolve("large"), "Large.mrpack"));
    }

    private ModpackInstaller installer() {
        return installer(new ArchiveExtractionGuard());
    }

    private ModpackInstaller installer(ArchiveExtractionGuard guard) {
        VersionInstaller versionInstaller = new VersionInstaller();
        FileDownloader downloader = new FileDownloader();
        URI manifest = URI.create("https://example.invalid/version_manifest_v2.json");
        Path cache = temporaryDirectory.resolve("manifest.json");
        return new ModpackInstaller(
                versionInstaller,
                new FabricInstaller(versionInstaller, new FabricService(), downloader, manifest, cache),
                new QuiltInstaller(versionInstaller, new QuiltService(), downloader, manifest, cache),
                new ExternalLoaderInstaller(versionInstaller, new JavaLocator(), downloader,
                        new MetadataService(), manifest, cache),
                downloader,
                manifest,
                cache,
                guard
        );
    }

    private static ModFile modFile(HttpServer server, String name, byte[] content) {
        return new ModFile(
                ModSource.MODRINTH, "project", "version", "Test Pack", "1.0",
                name, List.of(URI.create(serverRoot(server) + name)),
                HashVerifier.sha1(content), null, content.length,
                List.of("1.20.1"), List.of(), List.of(), "", 0
        );
    }

    private static HttpServer serve(byte[] content) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bad.mrpack", exchange -> respond(exchange, content));
        server.createContext("/missing.mrpack", exchange -> respond(exchange, content));
        server.start();
        return server;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] content)
            throws java.io.IOException {
        exchange.sendResponseHeaders(200, content.length);
        try (var output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static byte[] zip(String index) throws Exception {
        return zipEntries(Map.of(
                "modrinth.index.json", index.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] zipEntries(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private static Map<String, byte[]> ordered(Object... values) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            entries.put((String) values[index], (byte[]) values[index + 1]);
        }
        return entries;
    }

    private static byte[] zipEntriesWithDuplicates(Object... values) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int index = 0; index < values.length; index += 2) {
                zip.putNextEntry(new ZipEntry((String) values[index]));
                zip.write((byte[]) values[index + 1]);
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private Path writeZip(String name, byte[] content) throws Exception {
        Path archive = temporaryDirectory.resolve(name);
        Files.write(archive, content);
        return archive;
    }

    private static void createFakeInstallerJar(Path jar) throws Exception {
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
            output.write("""
                    {
                      "id":"test-forge-id",
                      "inheritsFrom":"1.20.1",
                      "mainClass":"test.ForgeMain",
                      "type":"release"
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    private static String serverRoot(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }
}
