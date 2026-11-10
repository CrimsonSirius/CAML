package org.chs.camlauncher.backend.loader.fabric;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.source.MirrorDownloadSource;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricServiceTest {

    @Test
    void fetchesLoadersProfileAndApiVersions() throws Exception {
        String loaderJson = """
                [
                  {"loader":{"version":"0.20.1","stable":true}},
                  {"loader":{"version":"0.19.0","stable":true}}
                ]
                """;
        String profileJson = """
                {"id":"fabric-loader-0.20.1-1.21.4","inheritsFrom":"1.21.4",
                 "mainClass":"net.fabricmc.loader.impl.launch.knot.KnotClient"}
                """;
        String apiJson = """
                [
                  {
                    "name":"Fabric API 0.119.4+1.21.4",
                    "version_number":"0.119.4+1.21.4",
                    "version_type":"release",
                    "date_published":"2025-08-08T13:22:14Z",
                    "status":"listed",
                    "files":[{
                      "url":"https://cdn.example/fabric-api.jar",
                      "filename":"fabric-api.jar",
                      "hashes":{"sha1":"api-sha1"},
                      "size":123,
                      "primary":true
                    }]
                  }
                ]
                """;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/versions/loader/1.21.4",
                exchange -> respond(exchange, loaderJson));
        server.createContext("/v2/versions/loader/1.21.4/0.20.1/profile/json",
                exchange -> respond(exchange, profileJson));
        server.createContext("/v2/project/fabric-api/version",
                exchange -> respond(exchange, apiJson));
        server.start();

        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            FabricService service = new FabricService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    base,
                    base
            );

            List<FabricLoaderVersion> loaders = service.fetchLoaderVersions("1.21.4");
            assertEquals(2, loaders.size());
            assertEquals("0.20.1", loaders.getFirst().version());
            assertTrue(loaders.getFirst().stable());

            assertTrue(service.fetchLoaderProfile("1.21.4", "0.20.1").contains("KnotClient"));

            List<FabricApiVersion> apis = service.fetchApiVersions("1.21.4");
            assertEquals(1, apis.size());
            assertEquals("0.119.4+1.21.4", apis.getFirst().versionNumber());
            assertEquals("api-sha1", apis.getFirst().sha1());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchesLoaderVersionsFromMirror() throws Exception {
        String loaderJson = """
                [
                  {"loader":{"version":"0.20.1","stable":true}}
                ]
                """;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fabric-meta/v2/versions/loader/1.21.4",
                exchange -> respond(exchange, loaderJson));
        server.start();

        try {
            FabricService service = new FabricService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    URI.create("https://meta.fabricmc.net/"),
                    URI.create("https://api.modrinth.com/")
            );
            service.setSources(List.of(new MirrorDownloadSource(
                    "http://127.0.0.1:" + server.getAddress().getPort())));

            List<FabricLoaderVersion> loaders = service.fetchLoaderVersions("1.21.4");

            assertEquals(1, loaders.size());
            assertEquals("0.20.1", loaders.getFirst().version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackToNextSourceWhenMirrorReturnsEmptyLoaderList() throws Exception {
        String loaderJson = """
                [
                  {"loader":{"version":"0.20.1","stable":true}}
                ]
                """;

        HttpServer emptyServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        emptyServer.createContext("/fabric-meta/v2/versions/loader/1.21.4",
                exchange -> respond(exchange, "[]"));
        emptyServer.start();
        HttpServer dataServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        dataServer.createContext("/fabric-meta/v2/versions/loader/1.21.4",
                exchange -> respond(exchange, loaderJson));
        dataServer.start();

        try {
            FabricService service = new FabricService(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                    new Gson(),
                    URI.create("https://meta.fabricmc.net/"),
                    URI.create("https://api.modrinth.com/")
            );
            service.setSources(List.of(
                    new MirrorDownloadSource(serverRoot(emptyServer)),
                    new MirrorDownloadSource(serverRoot(dataServer))
            ));

            List<FabricLoaderVersion> loaders = service.fetchLoaderVersions("1.21.4");

            assertEquals(1, loaders.size());
            assertEquals("0.20.1", loaders.getFirst().version());
        } finally {
            emptyServer.stop(0);
            dataServer.stop(0);
        }
    }

    private static String serverRoot(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
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
