package org.chs.camlauncher.backend.modpack;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricService;
import org.chs.camlauncher.backend.loader.quilt.QuiltInstaller;
import org.chs.camlauncher.backend.loader.quilt.QuiltService;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    private ModpackInstaller installer() {
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
                cache
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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("modrinth.index.json"));
            zip.write(index.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static String serverRoot(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }
}
