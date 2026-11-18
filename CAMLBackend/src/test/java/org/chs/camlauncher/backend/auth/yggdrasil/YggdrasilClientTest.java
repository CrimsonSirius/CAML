package org.chs.camlauncher.backend.auth.yggdrasil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用本地假认证服务器覆盖“token 未绑定角色”的两种修复路径：
 * authenticate 后补一次绑定，以及每次 refresh 都携带 selectedProfile。
 */
class YggdrasilClientTest {
    private static final String PROFILE_ID = "2c1f0b1d4c364f6c8c19ad0b9f77f0a1";
    private static final String REFRESH_PATH = "/api/yggdrasil/authserver/refresh";

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void authenticateBindsUnboundProfileThroughRefresh() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = start(
                exchange -> respond(exchange, 200, """
                        {"accessToken":"token-1","clientToken":"client-1",
                         "availableProfiles":[{"id":"%s","name":"Steve"}]}
                        """.formatted(PROFILE_ID)),
                exchange -> {
                    requests.add(readBody(exchange));
                    respond(exchange, 200, """
                            {"accessToken":"token-2","clientToken":"client-1",
                             "selectedProfile":{"id":"%s","name":"Steve"}}
                            """.formatted(PROFILE_ID));
                });

        YggdrasilSession session = new YggdrasilClient().authenticate(
                apiRoot(server), "steve@example.com", "pw", "client-1");

        assertEquals("token-2", session.accessToken(), "未绑定角色的 token 应通过 refresh 补绑定");
        assertEquals(PROFILE_ID, session.profileId());
        assertEquals("Steve", session.username());
        assertEquals(1, requests.size());
        assertTrue(requests.get(0).contains("\"selectedProfile\""), requests.get(0));
        assertTrue(requests.get(0).contains(PROFILE_ID), requests.get(0));
    }

    @Test
    void refreshSendsSelectedProfileAndFallsBackToKnownProfile() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = start(null, exchange -> {
            requests.add(readBody(exchange));
            respond(exchange, 200, "{\"accessToken\":\"token-2\"}");
        });

        Optional<YggdrasilSession> refreshed = new YggdrasilClient().refresh(
                apiRoot(server), "token-1", "client-1", PROFILE_ID, "Steve");

        YggdrasilSession session = refreshed.orElseThrow();
        assertEquals("token-2", session.accessToken());
        assertEquals(PROFILE_ID, session.profileId(), "响应缺少角色时应沿用调用方已知的角色");
        assertEquals("Steve", session.username());
        assertTrue(requests.get(0).contains("\"selectedProfile\""), requests.get(0));
        assertTrue(requests.get(0).contains("\"name\":\"Steve\""), requests.get(0));
    }

    @Test
    void refreshRejectionMeansLoginExpired() throws Exception {
        HttpServer server = start(null, exchange -> respond(exchange, 400, """
                {"error":"IllegalArgumentException",
                 "errorMessage":"The access token has no profile assigned, please select a profile."}
                """));

        Optional<YggdrasilSession> refreshed = new YggdrasilClient().refresh(
                apiRoot(server), "token-1", "client-1", PROFILE_ID, "Steve");

        assertTrue(refreshed.isEmpty(), "400 应被当作需要重新登录，而不是把服务器原文抛给界面");
    }

    @Test
    void refreshWithoutKnownProfileLeavesRequestUnbound() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = start(null, exchange -> {
            requests.add(readBody(exchange));
            respond(exchange, 200, """
                    {"accessToken":"token-2",
                     "selectedProfile":{"id":"%s","name":"Steve"}}
                    """.formatted(PROFILE_ID));
        });

        Optional<YggdrasilSession> refreshed = new YggdrasilClient().refresh(
                apiRoot(server), "token-1", "client-1", null, null);

        assertEquals(PROFILE_ID, refreshed.orElseThrow().profileId());
        assertFalse(requests.get(0).contains("selectedProfile"), requests.get(0));
    }

    private HttpServer start(Handler authenticate, Handler refresh) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        if (authenticate != null) {
            created.createContext("/api/yggdrasil/authserver/authenticate", exchange -> {
                try (exchange) {
                    authenticate.handle(exchange);
                }
            });
        }
        created.createContext(REFRESH_PATH, exchange -> {
            try (exchange) {
                refresh.handle(exchange);
            }
        });
        created.start();
        server = created;
        return created;
    }

    private static String apiRoot(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/yggdrasil";
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
