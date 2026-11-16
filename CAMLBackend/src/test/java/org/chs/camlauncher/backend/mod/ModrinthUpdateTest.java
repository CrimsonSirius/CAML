package org.chs.camlauncher.backend.mod;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.chs.camlauncher.backend.mod.source.ModrinthClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthUpdateTest {
    @Test
    void queriesLatestCompatibleVersionByHash() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/version_files/update_individual", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("abc123"));
            byte[] response = ("""
                    {"abc123":{"id":"v2","project_id":"sodium","name":"Sodium 2",
                    "version_number":"2.0","game_versions":["1.20.1"],"loaders":["fabric"],
                    "date_published":"2026-02-01T00:00:00Z","files":[
                    {"url":"https://example.invalid/sodium.jar","filename":"sodium.jar",
                    "primary":true,"size":10,"hashes":{"sha1":"abc123"}}]}}
                    """).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            ModrinthClient client = new ModrinthClient(
                    HttpClient.newHttpClient(),
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v2/"));

            Map<String, ModFile> result = client.latestVersions(
                    List.of("abc123"), "1.20.1", ModLoader.FABRIC);

            assertEquals("2.0", result.get("abc123").versionNumber());
            assertEquals("sodium.jar", result.get("abc123").fileName());
        } finally {
            server.stop(0);
        }
    }
}
