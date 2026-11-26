package org.chs.camlauncher.backend.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjectorService;
import org.chs.camlauncher.backend.auth.store.AccountStore;
import org.chs.camlauncher.backend.auth.store.AuthServerStore;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerService;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendErrorCarrier;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账号编排的端到端行为：不记住登录、账号解析回退链、服务器故障时的降级、角色切换。
 *
 * <p>所有用例都用本地假认证服务器，不访问外网。</p>
 */
class AccountServiceTest {
    private static final String PROFILE_ID = "2c1f0b1d4c364f6c8c19ad0b9f77f0a1";
    private static final String SECOND_PROFILE_ID = "9a8b7c6d5e4f30211234567890abcdef";

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void loginWithoutRememberStaysUsableInThisProcess(@TempDir Path dataDirectory) throws Exception {
        Fixture fixture = fixture(dataDirectory, new AtomicReference<>(okRefresh()));

        AccountSummary account = fixture.service.loginYggdrasil(
                fixture.apiRoot(), "steve", "pw", false);

        assertFalse(account.needsLogin(), "未记住登录的账号在当前进程内应可直接启动");
        AccountService.ResolvedAccount resolved = fixture.service.resolveById(account.id());
        assertEquals("mojang", resolved.session().userType());
        assertTrue(resolved.injector().isPresent());
    }

    @Test
    void loginWithoutRememberAsksForLoginAfterRestart(@TempDir Path dataDirectory) throws Exception {
        AtomicReference<String> refresh = new AtomicReference<>(okRefresh());
        Fixture fixture = fixture(dataDirectory, refresh);
        AccountSummary account = fixture.service.loginYggdrasil(
                fixture.apiRoot(), "steve", "pw", false);

        // 重启后重建全部服务：内存中的临时会话不再存在，磁盘上也没有 token。
        Fixture restarted = fixture(dataDirectory, refresh);
        AccountSummary reloaded = restarted.service.list().stream()
                .filter(item -> item.id().equals(account.id()))
                .findFirst()
                .orElseThrow();
        assertTrue(reloaded.needsLogin(), "重启后应提示重新登录");

        BackendError error = errorOf(assertThrows(BackendException.class,
                () -> restarted.service.resolveById(account.id())));
        assertEquals("auth.error.loginRequired", error.code());
    }

    @Test
    void resolveFallsBackFromInstanceToGlobalToNothing(@TempDir Path dataDirectory) throws Exception {
        Fixture fixture = fixture(dataDirectory, new AtomicReference<>(okRefresh()));
        AccountSummary account = fixture.service.loginYggdrasil(
                fixture.apiRoot(), "steve", "pw", true);
        Path gameDirectory = dataDirectory.resolve("game");

        assertNull(fixture.service.resolve(gameDirectory, "inst"), "没有账号配置时应回退到离线用户名");

        fixture.service.setDefaultAccountId(account.id());
        assertEquals(account.id(), fixture.service.resolve(gameDirectory, "inst").account().id());

        fixture.service.setInstanceAccountId(gameDirectory, "inst", null);
        fixture.service.setDefaultAccountId(null);
        assertTrue(fixture.service.instanceAccountId(gameDirectory, "inst").isEmpty());
    }

    @Test
    void serverFailureKeepsUsingExistingToken(@TempDir Path dataDirectory) throws Exception {
        AtomicReference<String> refresh = new AtomicReference<>(okRefresh());
        Fixture fixture = fixture(dataDirectory, refresh);
        AccountSummary account = fixture.service.loginYggdrasil(
                fixture.apiRoot(), "steve", "pw", true);

        // 皮肤站维护：refresh 返回 503。
        refresh.set(unavailableRefresh());

        AccountService.ResolvedAccount resolved = fixture.service.resolveById(account.id());
        assertEquals("mojang", resolved.session().userType(),
                "认证服务器暂时不可用时应继续使用现有 token");
        assertEquals("tok-1", resolved.session().accessToken());
    }

    @Test
    void switchProfileRebindsAccountWithoutChangingId(@TempDir Path dataDirectory) throws Exception {
        Fixture fixture = fixture(dataDirectory, new AtomicReference<>(okRefresh()));
        AccountSummary account = fixture.service.loginYggdrasil(
                fixture.apiRoot(), "steve", "pw", true);
        assertEquals(2, account.profiles().size());

        AccountSummary switched = fixture.service.switchProfile(account.id(), SECOND_PROFILE_ID);

        assertEquals(account.id(), switched.id(), "切换角色不应产生新账号");
        assertEquals(SECOND_PROFILE_ID, switched.uuid());
        assertEquals("Alex", switched.displayName());
        BackendException missing = assertThrows(BackendException.class,
                () -> fixture.service.switchProfile(account.id(), "ffffffffffffffffffffffffffffffff"));
        assertEquals("auth.error.profileMissing", errorOf(missing).code());
    }

    @Test
    void refreshPersistsProfileRenamedOnServer(@TempDir Path dataDirectory) throws Exception {
        AtomicReference<String> refresh = new AtomicReference<>(okRefresh());
        Fixture fixture = fixture(dataDirectory, refresh);
        AccountSummary account = fixture.service.loginYggdrasil(
                fixture.apiRoot(), "steve", "pw", true);
        refresh.set("""
                {"accessToken":"tok-next","clientToken":"client-token",
                 "selectedProfile":{"id":"%s","name":"SteveRenamed"}}
                """.formatted(PROFILE_ID));

        AccountService.ResolvedAccount resolved = fixture.service.resolveById(account.id());

        assertEquals("SteveRenamed", resolved.session().username(),
                "启动参数应使用服务器返回的最新角色名");
        AccountSummary stored = fixture.service.list().stream()
                .filter(item -> item.id().equals(account.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("SteveRenamed", stored.displayName(), "最新角色名应写回账号存储");
        assertEquals(account.uuid(), stored.uuid(), "改名不应改变角色 UUID");
    }

    // ------------------------------------------------------------------ 工具

    private Fixture fixture(Path dataDirectory, AtomicReference<String> refreshResponse) throws Exception {
        if (server == null) {
            server = startServer(refreshResponse);
        }
        JsonService json = new JsonService();
        AccountStore accounts = new AccountStore(dataDirectory, json);
        AuthServerService servers = new AuthServerService(
                new AuthServerStore(dataDirectory, json));
        AuthlibInjectorService injectors = new AuthlibInjectorService(
                dataDirectory, new FileDownloader());
        // 用真实文件跳过下载与校验，测试不依赖网络。
        Path jar = dataDirectory.resolve("authlib-injector.jar");
        Files.writeString(jar, "jar", StandardCharsets.UTF_8);
        injectors.setOverride(jar);

        SettingsService settings = new SettingsService(dataDirectory);
        AccountService service = new AccountService(accounts, servers, injectors, settings);
        return new Fixture(service, "http://127.0.0.1:" + server.getAddress().getPort()
                + "/api/yggdrasil");
    }

    private static String okRefresh() {
        return """
                {"accessToken":"tok-next","clientToken":"client-token",
                 "selectedProfile":{"id":"%s","name":"Steve"},
                 "availableProfiles":[{"id":"%s","name":"Steve"},{"id":"%s","name":"Alex"}]}
                """.formatted(PROFILE_ID, PROFILE_ID, SECOND_PROFILE_ID);
    }

    private static String unavailableRefresh() {
        return "{\"error\":\"Unavailable\"}";
    }

    private HttpServer startServer(AtomicReference<String> refreshResponse) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String root = "/api/yggdrasil";
        created.createContext(root, exchange -> {
            try (exchange) {
                respond(exchange, 200, """
                        {"meta":{"serverName":"Fake"},"skinDomains":["example.com"]}
                        """);
            }
        });
        created.createContext(root + "/authserver/authenticate", exchange -> {
            try (exchange) {
                respond(exchange, 200, """
                        {"accessToken":"tok-1","clientToken":"client-token",
                         "selectedProfile":{"id":"%s","name":"Steve"},
                         "availableProfiles":[{"id":"%s","name":"Steve"},
                                              {"id":"%s","name":"Alex"}]}
                        """.formatted(PROFILE_ID, PROFILE_ID, SECOND_PROFILE_ID));
            }
        });
        created.createContext(root + "/authserver/refresh", exchange -> {
            try (exchange) {
                String body = refreshResponse.get();
                if (body.contains("Unavailable")) {
                    respond(exchange, 503, body);
                    return;
                }
                // 假服务器按请求里的 selectedProfile 回包，和真实服务器行为一致：
                // 切换角色时 refresh 返回的是被请求的那个角色。
                if (readBody(exchange).contains(SECOND_PROFILE_ID)) {
                    respond(exchange, 200, """
                            {"accessToken":"tok-next","clientToken":"client-token",
                             "selectedProfile":{"id":"%s","name":"Alex"}}
                            """.formatted(SECOND_PROFILE_ID));
                    return;
                }
                respond(exchange, 200, body);
            }
        });
        created.createContext(root + "/authserver/validate", exchange -> {
            try (exchange) {
                respond(exchange, 204, "");
            }
        });
        created.start();
        return created;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        if (bytes.length == 0) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (var input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static BackendError errorOf(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof BackendErrorCarrier carrier) {
                return carrier.error();
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        throw new AssertionError("not a BackendException: " + failure);
    }

    private record Fixture(AccountService service, String apiRoot) {
    }
}
