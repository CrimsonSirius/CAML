package org.chs.camlauncher.backend.mod.source;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.error.BackendException;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModApiHttpClientTest {
    @Test
    void retriesTransientServerErrors() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/retry", exchange -> {
            if (requests.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            respond(exchange, 200, "{\"ok\":true}");
        });
        server.start();
        try {
            ModApiHttpClient client = client();
            JsonElement result = client.get(uri(server, "/retry"), Map.of());

            assertTrue(result.getAsJsonObject().get("ok").getAsBoolean());
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotRetryClientErrors() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/missing", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 404, "{\"error\":\"missing\"}");
        });
        server.start();
        try {
            ModApiHttpClient client = client();
            BackendException error = assertThrows(BackendException.class,
                    () -> client.get(uri(server, "/missing"), Map.of()));

            assertEquals("network.httpError", error.error().code());
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }

    private static ModApiHttpClient client() {
        return new ModApiHttpClient(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build());
    }

    private static URI uri(HttpServer server, String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static void respond(HttpExchange exchange, int status, String body)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
