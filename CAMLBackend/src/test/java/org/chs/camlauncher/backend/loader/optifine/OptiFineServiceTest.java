package org.chs.camlauncher.backend.loader.optifine;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptiFineServiceTest {
    @Test
    void fetchesVersionsAndBuildsInstallerUrls() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String body = """
                [
                  {"mcversion":"1.20.1","type":"HD_U","patch":"I5","filename":"OptiFine_1.20.1_HD_U_I5.jar","forge":"Forge 47.0.35"},
                  {"mcversion":"1.20.1","type":"HD_U","patch":"I6","filename":"OptiFine_1.20.1_HD_U_I6.jar","forge":"Forge 47.2.18"},
                  {"mcversion":"1.20.1","type":"HD_U_I6","patch":"pre4","filename":"preview_OptiFine.jar","forge":"Forge 47.1.0"},
                  {"mcversion":"1.20.1","type":"","patch":"","filename":"bad.jar"}
                ]
                """;
        server.createContext("/optifine/1.20.1", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/optifine/");
            OptiFineService service = new OptiFineService(HttpClient.newHttpClient(), new Gson(), base);

            var versions = service.fetchVersions("1.20.1");

            assertEquals(3, versions.size());
            assertEquals("HD_U_I6", versions.get(0).version());
            assertEquals("HD_U_I5", versions.get(1).version());
            assertEquals("HD_U_I6_pre4", versions.get(2).version());
            assertFalse(versions.get(0).preview());
            assertTrue(versions.get(2).preview());
            assertEquals(
                    "http://127.0.0.1:" + server.getAddress().getPort()
                            + "/optifine/1.20.1/HD_U/I6",
                    versions.get(0).downloadUri().toString()
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void parsesOfficialDownloadsPageInOfficialMode() throws Exception {
        String html = """
                <html><body>
                <h2>Minecraft 1.20.1</h2>
                <table>
                  <tr class='downloadLine downloadLineMain'>
                    <td class='colFile'>OptiFine HD U I6</td>
                    <td class='colMirror'><a href="http://optifine.net/adloadx?f=OptiFine_1.20.1_HD_U_I6.jar">(Mirror)</a></td>
                    <td class='colForge'>Forge 47.2.18</td>
                  </tr>
                  <tr class='downloadLine downloadLinePreview'>
                    <td class='colFile'>OptiFine HD U I6 pre4</td>
                    <td class='colMirror'><a href="http://optifine.net/adloadx?f=preview_OptiFine_1.20.1_HD_U_I6_pre4.jar">(Mirror)</a></td>
                    <td class='colForge'>Forge 47.1.0</td>
                  </tr>
                </table>
                <h2>Minecraft 1.20.2</h2>
                <table>
                  <tr class='downloadLine downloadLineMain'>
                    <td class='colMirror'><a href="http://optifine.net/adloadx?f=OptiFine_1.20.2_HD_U_I7.jar">(Mirror)</a></td>
                  </tr>
                </table>
                </body></html>
                """;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/downloads", exchange -> {
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            URI downloads = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/downloads");
            OptiFineService service = new OptiFineService(
                    HttpClient.newHttpClient(), new Gson(), URI.create("https://example.invalid/"), downloads);
            service.setSourceMode(DownloadSourceMode.OFFICIAL, "");

            var versions = service.fetchVersions("1.20.1");

            assertEquals(2, versions.size());
            assertEquals("HD_U_I6", versions.get(0).version());
            assertEquals("Forge 47.2.18", versions.get(0).forgeVersion());
            assertEquals("HD_U_I6_pre4", versions.get(1).version());
            assertTrue(versions.get(1).preview());
            assertEquals(
                    "https://optifine.net/adloadx?f=OptiFine_1.20.1_HD_U_I6.jar",
                    versions.get(0).downloadUri().toString()
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackWhenMirrorJsonIsEmptyOrInvalid() throws Exception {
        String body = """
                [
                  {"mcversion":"1.20.1","type":"HD_U","patch":"I5","filename":"OptiFine_1.20.1_HD_U_I5.jar"}
                ]
                """;
        HttpServer emptyServer = mirrorServer("[]");
        HttpServer invalidServer = mirrorServer("{");
        HttpServer dataServer = mirrorServer(body);

        try {
            OptiFineService service = new OptiFineService(
                    HttpClient.newHttpClient(), new Gson(),
                    URI.create("http://127.0.0.1:1/optifine/"));
            service.setSources(List.of(
                    rewriteTo(emptyServer),
                    rewriteTo(invalidServer),
                    rewriteTo(dataServer)
            ));

            var versions = service.fetchVersions("1.20.1");

            assertEquals(1, versions.size());
            assertEquals("HD_U_I5", versions.getFirst().version());
        } finally {
            emptyServer.stop(0);
            invalidServer.stop(0);
            dataServer.stop(0);
        }
    }

    private static HttpServer mirrorServer(String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/optifine/1.20.1", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static DownloadSource rewriteTo(HttpServer server) {
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        return new DownloadSource() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public List<URI> candidates(URI original) {
                return List.of(URI.create(base + original.getRawPath()));
            }
        };
    }
}
