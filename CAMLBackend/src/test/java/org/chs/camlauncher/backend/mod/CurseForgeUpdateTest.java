package org.chs.camlauncher.backend.mod;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.chs.camlauncher.backend.mod.source.CurseForgeClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurseForgeUpdateTest {
    @Test
    void resolvesFilesAndClassIdsInBulk() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/mods/files", exchange -> respond(exchange, """
                {"data":[{"id":456,"modId":123,"displayName":"Test Mod",
                "fileName":"test.jar","gameVersions":["1.20.1","Forge"],
                "fileLength":10,"fileDate":"2026-01-01T00:00:00Z","downloadCount":1,
                "downloadUrl":"https://example.invalid/test.jar",
                "hashes":[{"algo":1,"value":"sha1-value"},{"algo":2,"value":"md5-value"}]}]}
                """));
        server.createContext("/v1/mods", exchange -> respond(exchange, """
                {"data":[{"id":123,"classId":6}]}
                """));
        server.start();
        try {
            CurseForgeClient client = new CurseForgeClient(
                    HttpClient.newHttpClient(),
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/"),
                    "test-key");

            ModFile file = client.filesByIds(List.of(456L, 456L)).get(456L);
            assertEquals("test.jar", file.fileName());
            assertEquals("sha1-value", file.sha1());
            assertEquals("md5-value", file.md5());
            assertEquals(6, client.classIdsByModIds(List.of(123L)).get(123L));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolvesFingerprintAndLatestFile() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/fingerprints/432", exchange -> respond(exchange, """
                {"data":{"exactMatches":[{"id":123,"file":{"fileFingerprint":999}}]}}
                """));
        server.createContext("/v1/mods", exchange -> respond(exchange, """
                {"data":[{"id":123,"latestFilesIndexes":[
                {"gameVersion":"1.20.1","modLoader":4,"fileId":456}]}]}
                """));
        server.createContext("/v1/mods/files", exchange -> respond(exchange, """
                {"data":[{"id":456,"modId":123,"displayName":"Test Mod 2",
                "fileName":"test-2.jar","gameVersions":["1.20.1","Fabric"],
                "fileLength":10,"fileDate":"2026-01-01T00:00:00Z","downloadCount":1}]}
                """));
        server.start();
        try {
            CurseForgeClient client = new CurseForgeClient(
                    HttpClient.newHttpClient(),
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/"),
                    "test-key");
            Map<Long, String> modIds = client.fingerprintModIds(List.of(999L, 999L));
            assertEquals("123", modIds.get(999L));

            Map<Long, ModFile> latest = client.latestVersions(modIds, "1.20.1", ModLoader.FABRIC);

            assertEquals("test-2.jar", latest.get(999L).fileName());
            assertEquals("Test Mod 2", latest.get(999L).name());
        } finally {
            server.stop(0);
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
