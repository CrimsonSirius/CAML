package org.chs.camlauncher.backend.download.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAwareHttpClientTest {
    @Test
    void fallsBackWhenParsedJsonIsEmptyOrInvalid() throws Exception {
        HttpServer emptyServer = server("[]");
        HttpServer invalidServer = server("{");
        HttpServer dataServer = server("[\"ok\"]");

        try {
            SourceAwareHttpClient client = new SourceAwareHttpClient(HttpClient.newHttpClient());
            client.setSources(List.of(
                    rewriteTo(emptyServer),
                    rewriteTo(invalidServer),
                    rewriteTo(dataServer)
            ));

            Optional<List<String>> result = client.getParsedText(
                    URI.create("https://example.invalid/list"),
                    Duration.ofSeconds(5),
                    "application/json",
                    SourceAwareHttpClientTest::parseStrings
            );

            assertTrue(result.isPresent());
            assertEquals(List.of("ok"), result.orElseThrow());
        } finally {
            emptyServer.stop(0);
            invalidServer.stop(0);
            dataServer.stop(0);
        }
    }

    private static Optional<List<String>> parseStrings(String text) {
        JsonArray array = JsonParser.parseString(text).getAsJsonArray();
        List<String> values = new ArrayList<>();
        for (var element : array) {
            values.add(element.getAsString());
        }
        return values.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(values));
    }

    private static HttpServer server(String body) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/list", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
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
