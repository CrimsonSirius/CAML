package org.chs.camlauncher.backend.loader;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.source.MirrorDownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.loader.forge.ForgeService;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeService;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgeAndNeoForgeServiceTest {

    @Test
    void filtersAndOrdersForgeAndNeoForgeVersions() throws Exception {
        String forgeXml = """
                <metadata><versioning><versions>
                  <version>1.20.1-47.3.0</version>
                  <version>1.19.2-43.0.0</version>
                  <version>1.20.1-47.4.0</version>
                </versions></versioning></metadata>
                """;
        String neoForgeXml = """
                <metadata><versioning><versions>
                  <version>20.2.85-beta</version>
                  <version>20.1.0</version>
                  <version>20.2.86</version>
                  <version>20.0.1</version>
                  <version>21.1.0</version>
                </versions></versioning></metadata>
                """;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Map<String, byte[]> installerJars = new HashMap<>();
        installerJars.put("20.2.85-beta", installerJar("20.2.85-beta", "1.20.2"));
        installerJars.put("20.2.86", installerJar("20.2.86", "1.20.2"));
        installerJars.put("20.1.0", installerJar("20.1.0", "1.20.1"));
        installerJars.put("20.0.1", installerJar("20.0.1", "1.20"));
        installerJars.put("21.1.0", installerJar("21.1.0", "1.21.1"));

        server.createContext("/forge.xml", exchange -> respond(exchange, forgeXml));
        server.createContext("/neoforge.xml", exchange -> respond(exchange, neoForgeXml));
        server.createContext("/neoforge/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            String version = filename.substring("neoforge-".length(),
                    filename.length() - "-installer.jar".length());
            byte[] body = installerJars.get(version);
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            respondRange(exchange, body);
        });
        server.start();

        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            ForgeService forgeService = new ForgeService(
                    new MavenMetadataClient(),
                    URI.create(base + "forge.xml"),
                    base + "forge/"
            );
            NeoForgeService neoForgeService = new NeoForgeService(
                    new MavenMetadataClient(),
                    URI.create(base + "neoforge.xml"),
                    base + "neoforge/"
            );

            List<ForgeVersion> forge = forgeService.fetchVersions("1.20.1");
            assertEquals(2, forge.size());
            assertEquals("47.4.0", forge.getFirst().version());
            assertEquals("1.20.1-forge-47.4.0", forge.getFirst().versionId());
            assertEquals(
                    base + "forge/1.20.1-47.4.0/forge-1.20.1-47.4.0-installer.jar",
                    forge.getFirst().installerUri().toString()
            );

            List<NeoForgeVersion> neoForge = neoForgeService.fetchVersions("1.20.2");
            assertEquals(2, neoForge.size());
            assertEquals("20.2.86", neoForge.getFirst().version());
            assertEquals("neoforge-20.2.86", neoForge.getFirst().versionId());
            assertEquals(
                    base + "neoforge/20.2.86/neoforge-20.2.86-installer.jar",
                    neoForge.getFirst().installerUri().toString()
            );

            List<NeoForgeVersion> baseVersion = neoForgeService.fetchVersions("1.20");
            assertEquals(1, baseVersion.size());
            assertEquals("20.0.1", baseVersion.getFirst().version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchesForgeMetadataFromMirror() throws Exception {
        String forgeXml = """
                <metadata><versioning><versions>
                  <version>1.20.1-47.4.0</version>
                </versions></versioning></metadata>
                """;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/maven/net/minecraftforge/forge/maven-metadata.xml",
                exchange -> respond(exchange, forgeXml));
        server.start();

        try {
            ForgeService service = new ForgeService();
            service.setSources(List.of(new MirrorDownloadSource(
                    "http://127.0.0.1:" + server.getAddress().getPort())));

            List<ForgeVersion> versions = service.fetchVersions("1.20.1");

            assertEquals(1, versions.size());
            assertEquals("47.4.0", versions.getFirst().version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchesLoaderListsFromBmclJsonApi() throws Exception {
        String forgeJson = """
                [
                  {"version":"52.0.0","files":[{"category":"installer","format":"jar"}]},
                  {"version":"52.1.21","files":[{"category":"installer","format":"jar"}]}
                ]
                """;
        String neoForgeJson = """
                [
                  {"version":"21.1.1","mcversion":"1.21.1"},
                  {"version":"21.1.265","mcversion":"1.21.1"},
                  {"version":"21.2.0","mcversion":"1.21.2"}
                ]
                """;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/forge/minecraft/1.21.1", exchange -> respond(exchange, forgeJson));
        server.createContext("/neoforge/list/1.21.1", exchange -> respond(exchange, neoForgeJson));
        server.start();

        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            ForgeService forgeService = new ForgeService();
            forgeService.setSourceMode(DownloadSourceMode.CUSTOM, base);
            NeoForgeService neoForgeService = new NeoForgeService();
            neoForgeService.setSourceMode(DownloadSourceMode.CUSTOM, base);

            List<ForgeVersion> forge = forgeService.fetchVersions("1.21.1");
            assertEquals(2, forge.size());
            assertEquals("52.1.21", forge.getFirst().version());

            List<NeoForgeVersion> neoForge = neoForgeService.fetchVersions("1.21.1");
            assertEquals(2, neoForge.size());
            assertEquals("21.1.265", neoForge.getFirst().version());
            assertEquals(
                    "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
                            + "21.1.265/neoforge-21.1.265-installer.jar",
                    neoForge.getFirst().installerUri().toString()
            );
        } finally {
            server.stop(0);
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String content)
            throws java.io.IOException {
        respondRange(exchange, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void respondRange(com.sun.net.httpserver.HttpExchange exchange, byte[] content)
            throws java.io.IOException {
        String range = exchange.getRequestHeaders().getFirst("Range");
        if (range == null || !range.startsWith("bytes=")) {
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
            return;
        }

        String[] bounds = range.substring("bytes=".length()).split("-", -1);
        int start = Integer.parseInt(bounds[0]);
        int end = bounds.length < 2 || bounds[1].isBlank()
                ? content.length - 1
                : Integer.parseInt(bounds[1]);
        int length = end - start + 1;
        exchange.getResponseHeaders().add("Content-Range",
                "bytes " + start + "-" + end + "/" + content.length);
        exchange.sendResponseHeaders(206, length);
        try (var output = exchange.getResponseBody()) {
            output.write(content, start, length);
        }
    }

    private static byte[] installerJar(String version, String inheritsFrom) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("version.json"));
            zip.write(("{\"id\":\"neoforge-" + version
                    + "\",\"inheritsFrom\":\"" + inheritsFrom + "\"}")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            return bytes.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
