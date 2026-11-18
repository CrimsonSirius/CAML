package org.chs.camlauncher.backend.auth.yggdrasil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 标准 Yggdrasil 认证协议的 HTTP 客户端。
 *
 * <p>只实现启动器需要的四个调用：拉取 metadata、authenticate、refresh 和 validate。
 * 全部使用 JDK {@link HttpClient}，与项目的下载链路保持一致，不引入额外依赖。</p>
 */
public final class YggdrasilClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String AGENT_NAME = "Minecraft";
    private static final int AGENT_VERSION = 1;

    private final HttpClient httpClient;

    public YggdrasilClient() {
        this(defaultClient());
    }

    public YggdrasilClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    /** 拉取认证服务器 metadata；地址会先规范化。 */
    public AuthServerInfo fetchServer(String apiRoot) throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalized))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = send(request, normalized);
        if (!isSuccess(response.statusCode())) {
            throw new BackendException(BackendError.of(
                    "auth.error.metadataUnavailable", normalized, response.statusCode()));
        }

        JsonObject root = parseObject(response.body(), normalized);
        String serverName = stringValue(nested(root, "meta", "serverName"));
        List<String> skinDomains = new ArrayList<>();
        JsonElement domains = root.get("skinDomains");
        if (domains != null && domains.isJsonArray()) {
            JsonArray array = domains.getAsJsonArray();
            for (JsonElement element : array) {
                if (element != null && element.isJsonPrimitive()) {
                    skinDomains.add(element.getAsString());
                }
            }
        }
        return new AuthServerInfo(normalized, serverName, skinDomains, response.body());
    }

    /**
     * 用户名 + 密码登录。
     *
     * @throws BackendException 凭据错误（{@code auth.error.invalidCredentials}）或网络失败
     */
    public YggdrasilSession authenticate(String apiRoot, String username, String password,
                                         String clientToken)
            throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        if (clientToken != null && !clientToken.isBlank()) {
            body.addProperty("clientToken", clientToken);
        }
        JsonObject agent = new JsonObject();
        agent.addProperty("name", AGENT_NAME);
        agent.addProperty("version", AGENT_VERSION);
        body.add("agent", agent);
        body.addProperty("requestUser", false);

        HttpResponse<String> response = post(normalized + "/authserver/authenticate", body, normalized);
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BackendException(BackendError.of(
                    "auth.error.invalidCredentials", normalized, errorMessage(response.body())));
        }
        if (!isSuccess(response.statusCode())) {
            throw new BackendException(BackendError.of(
                    "auth.error.requestFailed", normalized, response.statusCode(),
                    errorMessage(response.body())));
        }
        JsonObject root = parseObject(response.body(), normalized);
        YggdrasilSession session = parseSession(root, normalized, clientToken, true, null, null);
        if (asObject(root.get("selectedProfile")) == null) {
            // 部分服务器（如 LittleSkin）在账号拥有多个角色时只返回 availableProfiles、
            // 不把 token 绑定到任何角色，之后的 validate/refresh 与进入服务器都会被拒绝。
            // Yggdrasil 允许用 refresh + selectedProfile 补一次绑定，这里就地完成。
            session = refresh(normalized, session.accessToken(), session.clientToken(),
                    session.profileId(), session.username()).orElse(session);
        }
        return session;
    }

    /**
     * 用 accessToken 换取新 token，并把 token 绑定到指定角色。
     *
     * <p>Yggdrasil 允许在 refresh 请求里携带 {@code selectedProfile}。对于“未绑定角色”的
     * accessToken，LittleSkin 等实现会直接返回 400 并提示 {@code please select a profile}，
     * 因此只要知道角色就应当始终传入。</p>
     *
     * @return token 已失效或服务器拒绝绑定角色时返回空，调用方需要重新输入密码
     */
    public Optional<YggdrasilSession> refresh(String apiRoot, String accessToken, String clientToken,
                                              String profileId, String profileName)
            throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        JsonObject body = new JsonObject();
        body.addProperty("accessToken", accessToken);
        body.addProperty("clientToken", clientToken);
        if (profileId != null && !profileId.isBlank()) {
            JsonObject selectedProfile = new JsonObject();
            selectedProfile.addProperty("id", profileId);
            if (profileName != null && !profileName.isBlank()) {
                selectedProfile.addProperty("name", profileName);
            }
            body.add("selectedProfile", selectedProfile);
        }

        HttpResponse<String> response = post(normalized + "/authserver/refresh", body, normalized);
        if (response.statusCode() == 400 || response.statusCode() == 401
                || response.statusCode() == 403) {
            // token 过期、被吊销，或服务器不接受把 token 绑定到该角色：
            // 统一按“需要重新登录”处理，避免把服务器原文直接抛到界面。
            return Optional.empty();
        }
        if (!isSuccess(response.statusCode())) {
            throw new BackendException(BackendError.of(
                    "auth.error.requestFailed", normalized, response.statusCode(),
                    errorMessage(response.body())));
        }
        return Optional.of(parseSession(parseObject(response.body(), normalized), normalized,
                clientToken, false, profileId, profileName));
    }

    /** 校验 accessToken 是否仍然有效；网络错误按调用失败抛出，由调用方决定是否降级。 */
    public boolean validate(String apiRoot, String accessToken, String clientToken)
            throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        JsonObject body = new JsonObject();
        body.addProperty("accessToken", accessToken);
        body.addProperty("clientToken", clientToken);

        HttpResponse<String> response = post(normalized + "/authserver/validate", body, normalized);
        if (response.statusCode() == 204 || isSuccess(response.statusCode())) {
            return true;
        }
        if (response.statusCode() == 401 || response.statusCode() == 403 || response.statusCode() == 400) {
            return false;
        }
        throw new BackendException(BackendError.of(
                "auth.error.requestFailed", normalized, response.statusCode(),
                errorMessage(response.body())));
    }

    private YggdrasilSession parseSession(JsonObject root, String apiRoot, String requestedClientToken,
                                          boolean requireSelectedProfile,
                                          String fallbackProfileId, String fallbackProfileName)
            throws BackendException {
        String accessToken = stringValue(root.get("accessToken"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.responseInvalid", apiRoot));
        }
        String clientToken = stringValue(root.get("clientToken"));
        if (clientToken == null || clientToken.isBlank()) {
            clientToken = requestedClientToken;
        }

        JsonObject profile = asObject(root.get("selectedProfile"));
        if (profile == null) {
            JsonArray available = asArray(root.get("availableProfiles"));
            if (available != null && !available.isEmpty()) {
                profile = asObject(available.get(0));
            }
        }
        if (profile == null) {
            // refresh 的响应允许不带角色信息，此时沿用调用方已知的角色。
            if (fallbackProfileId != null && !fallbackProfileId.isBlank()
                    && fallbackProfileName != null && !fallbackProfileName.isBlank()) {
                return new YggdrasilSession(fallbackProfileName, fallbackProfileId,
                        accessToken, clientToken);
            }
            if (requireSelectedProfile) {
                throw new BackendException(BackendError.of("auth.error.noSelectedProfile", apiRoot));
            }
            throw new BackendException(BackendError.of("auth.error.responseInvalid", apiRoot));
        }

        String name = stringValue(profile.get("name"));
        String id = stringValue(profile.get("id"));
        if (name == null || name.isBlank() || id == null || id.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.responseInvalid", apiRoot));
        }
        return new YggdrasilSession(name, id, accessToken, clientToken);
    }

    private HttpResponse<String> post(String url, JsonObject body, String apiRoot)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return send(request, apiRoot);
    }

    private HttpResponse<String> send(HttpRequest request, String apiRoot)
            throws IOException, InterruptedException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BackendException(BackendError.of(
                    "network.requestFailed", apiRoot, request.uri().toString()), e);
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static JsonObject parseObject(String body, String apiRoot) throws BackendException {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (RuntimeException ignored) {
            // 统一转换成结构化错误，避免把原始响应写进界面文案。
        }
        throw new BackendException(BackendError.of("auth.error.responseInvalid", apiRoot));
    }

    private static String errorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed != null && parsed.isJsonObject()) {
                String message = stringValue(parsed.getAsJsonObject().get("errorMessage"));
                if (message != null) {
                    return message;
                }
                String error = stringValue(parsed.getAsJsonObject().get("error"));
                if (error != null) {
                    return error;
                }
            }
        } catch (RuntimeException ignored) {
            // 错误体不是 JSON 时只保留 HTTP 状态码。
        }
        return "";
    }

    private static JsonObject nested(JsonObject root, String parent, String child) {
        JsonObject container = asObject(root.get(parent));
        return container == null ? null : asObject(container.get(child));
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray asArray(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String stringValue(JsonElement element) {
        return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
