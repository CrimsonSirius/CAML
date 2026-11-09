package org.chs.camlauncher.backend.loader.optifine;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

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
}
