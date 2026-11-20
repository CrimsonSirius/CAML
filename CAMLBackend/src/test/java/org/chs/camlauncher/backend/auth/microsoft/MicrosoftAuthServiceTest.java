package org.chs.camlauncher.backend.auth.microsoft;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.error.BackendException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrosoftAuthServiceTest {
    private static final String PROFILE_ID = "2c1f0b1d-4c36-4f6c-8c19-ad0b9f77f0a1";

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void deviceCodeLoginCompletesXboxAndMinecraftChain() throws Exception {
        AtomicReference<String> deviceRequest = new AtomicReference<>();
        AtomicReference<String> xstsRequest = new AtomicReference<>();
        AtomicReference<String> minecraftRequest = new AtomicReference<>();
        startServer(deviceRequest, xstsRequest, minecraftRequest,
                "{\"items\":[{\"name\":\"game_minecraft\"}]}", 200);

        MicrosoftAuthService service = service();
        AtomicReference<MicrosoftDeviceCode> shownCode = new AtomicReference<>();
        MicrosoftSession session = service.login(shownCode::set);

        assertNotNull(shownCode.get());
        assertEquals("ABCD-EFGH", shownCode.get().userCode());
        assertEquals("https://example.test/device", shownCode.get().verificationUri());
        assertEquals("Steve", session.profileName());
        assertEquals(PROFILE_ID, session.profileId().toString());
        assertEquals("minecraft-access-token", session.accessToken());
        assertEquals("refresh-token", session.refreshToken());
        assertTrue(session.expiresAt() > System.currentTimeMillis());

        assertTrue(deviceRequest.get().contains("client_id=client-id"), deviceRequest.get());
        assertTrue(xstsRequest.get().contains("rp://api.minecraftservices.com/"), xstsRequest.get());
        assertTrue(minecraftRequest.get().contains("XBL3.0 x=uhs-1;xsts-token"),
                minecraftRequest.get());
    }

    @Test
    void refreshRotatesTokensAndReturnsMinecraftSession() throws Exception {
        AtomicReference<String> refreshRequest = new AtomicReference<>();
        startServer(new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(),
                "{\"items\":[{\"name\":\"game_minecraft\"}]}", 200, refreshRequest);

        MicrosoftSession session = service().refresh("old-refresh-token");

        assertEquals("minecraft-access-token", session.accessToken());
        assertEquals("new-refresh-token", session.refreshToken());
        assertTrue(refreshRequest.get().contains("refresh_token=old-refresh-token"),
                refreshRequest.get());
        assertTrue(refreshRequest.get().contains("grant_type=refresh_token"), refreshRequest.get());
    }

    @Test
    void rejectsAccountWithoutMinecraftLicense() throws Exception {
        startServer(new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(),
                "{\"items\":[]}", 200);

        BackendException error = assertThrows(BackendException.class,
                () -> service().login(code -> { }));

        assertEquals("auth.microsoft.noLicense", error.error().code());
    }

    @Test
    void reportsMinecraftProfileCreationRequired() throws Exception {
        startServer(new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(),
                "{\"items\":[{\"name\":\"game_minecraft\"}]}", 404);

        BackendException error = assertThrows(BackendException.class,
                () -> service().login(code -> { }));

        assertEquals("auth.microsoft.noProfile", error.error().code());
    }

    private MicrosoftAuthService service() {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        MicrosoftAuthService.MicrosoftEndpoints endpoints =
                new MicrosoftAuthService.MicrosoftEndpoints(
                        base.resolve("/device-code"),
                        base.resolve("/device-token"),
                        base.resolve("/refresh"),
                        base.resolve("/xbox"),
                        base.resolve("/xsts"),
                        base.resolve("/minecraft-login"),
                        base.resolve("/entitlements"),
                        base.resolve("/profile"));
        return new MicrosoftAuthService("client-id", HttpClient.newHttpClient(), endpoints);
    }

    private void startServer(AtomicReference<String> deviceRequest,
                             AtomicReference<String> xstsRequest,
                             AtomicReference<String> minecraftRequest,
                             String entitlementsJson,
                             int profileStatus) throws IOException {
        startServer(deviceRequest, xstsRequest, minecraftRequest,
                entitlementsJson, profileStatus, new AtomicReference<>());
    }

    private void startServer(AtomicReference<String> deviceRequest,
                             AtomicReference<String> xstsRequest,
                             AtomicReference<String> minecraftRequest,
                             String entitlementsJson,
                             int profileStatus,
                             AtomicReference<String> refreshRequest) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/device-code", exchange -> {
            deviceRequest.set(readBody(exchange));
            respond(exchange, 200, """
                    {"device_code":"device-code","user_code":"ABCD-EFGH",
                     "verification_uri":"https://example.test/device",
                     "expires_in":900,"interval":0}
                    """);
        });
        server.createContext("/device-token", exchange -> respond(exchange, 200, """
                {"access_token":"microsoft-access-token","refresh_token":"refresh-token"}
                """));
        server.createContext("/refresh", exchange -> {
            refreshRequest.set(readBody(exchange));
            respond(exchange, 200, """
                    {"access_token":"microsoft-access-token","refresh_token":"new-refresh-token"}
                    """);
        });
        server.createContext("/xbox", exchange -> respond(exchange, 200, """
                {"Token":"xbox-token","DisplayClaims":{"xui":[{"uhs":"uhs-1"}]}}
                """));
        server.createContext("/xsts", exchange -> {
            xstsRequest.set(readBody(exchange));
            respond(exchange, 200, """
                    {"Token":"xsts-token","DisplayClaims":{"xui":[{"uhs":"uhs-1"}]}}
                    """);
        });
        server.createContext("/minecraft-login", exchange -> {
            minecraftRequest.set(readBody(exchange));
            respond(exchange, 200, """
                    {"access_token":"minecraft-access-token","token_type":"Bearer",
                     "expires_in":86400}
                    """);
        });
        server.createContext("/entitlements", exchange -> respond(exchange, 200, entitlementsJson));
        server.createContext("/profile", exchange -> {
            if (profileStatus != 200) {
                respond(exchange, profileStatus, "{}");
            } else {
                respond(exchange, 200, """
                        {"id":"%s","name":"Steve"}
                        """.formatted(PROFILE_ID));
            }
        });
        server.start();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
