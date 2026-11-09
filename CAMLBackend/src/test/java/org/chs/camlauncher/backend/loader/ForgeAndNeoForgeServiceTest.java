package org.chs.camlauncher.backend.loader;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.source.MirrorDownloadSource;
import org.chs.camlauncher.backend.loader.forge.ForgeService;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeService;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
                  <version>21.1.0</version>
                </versions></versioning></metadata>
                """;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/forge.xml", exchange -> respond(exchange, forgeXml));
        server.createContext("/neoforge.xml", exchange -> respond(exchange, neoForgeXml));
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

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String content)
            throws java.io.IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
