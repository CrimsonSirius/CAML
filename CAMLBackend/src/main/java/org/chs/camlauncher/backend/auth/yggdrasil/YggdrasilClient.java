package org.chs.camlauncher.backend.auth.yggdrasil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 标准 Yggdrasil 认证协议的 HTTP 客户端。
 *
 * <p>只实现启动器需要的四个调用：拉取 metadata、authenticate、refresh 和 validate。
 * 全部使用 JDK {@link HttpClient}，与项目的下载链路保持一致，不引入额外依赖。</p>
 *
 * <p><b>错误分类</b>：网络失败、超时、5xx 和限流抛出
 * {@link YggdrasilUnavailableException}，语义是“暂时无法确认凭据状态”，启动流程可以
 * 继续使用已有的 accessToken；凭据被拒绝（400/401/403）一律转换为
 * {@code Optional.empty()} 或固定错误码，服务器原文只写日志、不进界面。</p>
 */
public final class YggdrasilClient {
    private static final Logger LOGGER = LogManager.getLogger(YggdrasilClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "CAML/1.0";
    private static final String AGENT_NAME = "Minecraft";
    private static final int AGENT_VERSION = 1;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

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
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = send(request, normalized);
        if (!isSuccess(response.statusCode())) {
            // metadata 属于可选扩展：拿不到时调用方会回退到缓存或跳过 prefetched，
            // 因此统一按“暂时不可用”处理，不让 metadata 阻塞登录。
            throw new YggdrasilUnavailableException(BackendError.of(
                    "auth.error.metadataUnavailable", normalized, response.statusCode()));
        }

        JsonObject root = parseObject(response.body(), normalized);
        String serverName = stringValue(nested(root, "meta", "serverName"));
        List<String> skinDomains = new ArrayList<>();
        JsonArray domains = asArray(root.get("skinDomains"));
        if (domains != null) {
            for (JsonElement element : domains) {
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
     * @throws BackendException 凭据错误（{@code auth.error.invalidCredentials}）或限流
     * @throws YggdrasilUnavailableException 服务器暂时不可用
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
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            LOGGER.debug("认证被拒绝（{}）: {}", status, errorMessage(response.body()));
            throw new BackendException(BackendError.of("auth.error.invalidCredentials", normalized));
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            throw new BackendException(BackendError.of("auth.error.tooManyRequests", normalized));
        }
        if (isServerSideFailure(status)) {
            throw new YggdrasilUnavailableException(BackendError.of(
                    "auth.error.requestFailed", normalized, status));
        }
        if (!isSuccess(status)) {
            LOGGER.debug("认证请求失败（{}）: {}", status, errorMessage(response.body()));
            throw new BackendException(BackendError.of(
                    "auth.error.requestFailed", normalized, status));
        }

        JsonObject root = parseObject(response.body(), normalized);
        YggdrasilSession session = parseSession(root, normalized, clientToken, true, null, null);
        if (asObject(root.get("selectedProfile")) == null) {
            // 部分服务器（如 LittleSkin）在账号拥有多个角色时只返回 availableProfiles、
            // 不把 token 绑定到任何角色，之后的 validate/refresh 与进入服务器都会被拒绝。
            // Yggdrasil 允许用 refresh + selectedProfile 补一次绑定，这里就地完成。
            try {
                session = refresh(normalized, session.accessToken(), session.clientToken(),
                        session.profileId(), session.username()).orElse(session);
            } catch (YggdrasilUnavailableException e) {
                // 绑定失败不影响本次登录：启动时会再尝试一次。
                LOGGER.warn("登录成功但无法绑定角色，启动时会重试: {}", normalized, e);
            }
        }
        return session;
    }

    /**
     * 用 accessToken 换取新 token。
     *
     * @return token 已失效或服务器拒绝时返回空，调用方需要重新输入密码
     */
    public Optional<YggdrasilSession> refresh(String apiRoot, String accessToken, String clientToken)
            throws IOException, InterruptedException {
        return refresh(apiRoot, accessToken, clientToken, null, null);
    }

    /**
     * 用 accessToken 换取新 token，并把 token 绑定到指定角色。
     *
     * <p>Yggdrasil 允许在 refresh 请求里携带 {@code selectedProfile}。对于“未绑定角色”的
     * accessToken，LittleSkin 等实现会直接返回 400 并提示 {@code please select a profile}，
     * 因此只要知道角色就应当始终传入。同一个方法也用于切换角色：把 token 重新绑定到
     * 另一个 availableProfile。</p>
     *
     * @return token 已失效或服务器拒绝绑定角色时返回空，调用方需要重新输入密码
     * @throws YggdrasilUnavailableException 服务器暂时不可用（5xx、限流、网络失败）
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
        int status = response.statusCode();
        if (status == HTTP_TOO_MANY_REQUESTS) {
            throw new YggdrasilUnavailableException(BackendError.of(
                    "auth.error.tooManyRequests", normalized));
        }
        if (isServerSideFailure(status)) {
            throw new YggdrasilUnavailableException(BackendError.of(
                    "auth.error.requestFailed", normalized, status));
        }
        if (!isSuccess(status)) {
            // token 过期、被吊销，或服务器不接受把 token 绑定到该角色：
            // 统一按“需要重新登录”处理，避免把服务器原文直接抛到界面。
            LOGGER.debug("refresh 被拒绝（{}）: {}", status, errorMessage(response.body()));
            return Optional.empty();
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
        int status = response.statusCode();
        if (status == 204 || isSuccess(status)) {
            return true;
        }
        if (status == 400 || status == 401 || status == 403) {
            return false;
        }
        throw new YggdrasilUnavailableException(BackendError.of(
                "auth.error.requestFailed", normalized, status));
    }

    /**
     * 解析 authenticate / refresh 的响应体。
     *
     * @param login        {@code true} 表示登录请求：完全没有角色时提示“账号没有角色”
     * @param fallbackId   refresh 响应允许不带角色信息，此时沿用调用方已知的角色
     * @param fallbackName 同上
     */
    private YggdrasilSession parseSession(JsonObject root, String apiRoot,
                                          String requestedClientToken, boolean login,
                                          String fallbackId, String fallbackName)
            throws BackendException {
        String accessToken = stringValue(root.get("accessToken"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.responseInvalid", apiRoot));
        }
        String clientToken = stringValue(root.get("clientToken"));
        if (clientToken == null || clientToken.isBlank()) {
            clientToken = requestedClientToken;
        }

        List<YggdrasilProfile> profiles = parseProfiles(root.get("availableProfiles"));
        YggdrasilProfile selected = parseProfile(root.get("selectedProfile"));
        if (selected == null) {
            selected = profiles.stream()
                    .filter(profile -> profile.sameId(fallbackId))
                    .findFirst()
                    .orElse(profiles.isEmpty() ? null : profiles.get(0));
        }
        if (selected == null && fallbackId != null && !fallbackId.isBlank()
                && fallbackName != null && !fallbackName.isBlank()) {
            selected = new YggdrasilProfile(fallbackId, fallbackName);
        }
        if (selected == null) {
            throw new BackendException(BackendError.of(
                    login ? "auth.error.noSelectedProfile" : "auth.error.responseInvalid", apiRoot));
        }
        YggdrasilProfile chosen = selected;
        if (profiles.stream().noneMatch(profile -> profile.sameId(chosen.id()))) {
            List<YggdrasilProfile> merged = new ArrayList<>(profiles);
            merged.add(chosen);
            profiles = List.copyOf(merged);
        }
        return new YggdrasilSession(chosen.name(), chosen.id(), accessToken, clientToken,
                profiles);
    }

    private static List<YggdrasilProfile> parseProfiles(JsonElement element) {
        JsonArray array = asArray(element);
        if (array == null) {
            return List.of();
        }
        List<YggdrasilProfile> profiles = new ArrayList<>(array.size());
        for (JsonElement item : array) {
            YggdrasilProfile profile = parseProfile(item);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        return List.copyOf(profiles);
    }

    private static YggdrasilProfile parseProfile(JsonElement element) {
        JsonObject object = asObject(element);
        if (object == null) {
            return null;
        }
        String id = canonicalProfileId(stringValue(object.get("id")));
        if (id == null) {
            return null;
        }
        String name = stringValue(object.get("name"));
        return new YggdrasilProfile(id, name == null ? "" : name);
    }

    /**
     * 把角色 id 规范化为 32 位小写十六进制。
     *
     * <p>Yggdrasil 规范里 {@code id} 是 32 位无横线字符串，但个别实现会返回带横线的
     * UUID 或大写十六进制。统一规范化后，账号存储里的 {@code uuid}、角色列表和
     * {@code selectedProfile} 比较才能始终一致；格式不合法时返回 {@code null}，
     * 由调用方按“响应里没有这个角色”处理，而不是在启动时才抛 UUID 解析异常。</p>
     */
    private static String canonicalProfileId(String value) {
        if (value == null) {
            return null;
        }
        String raw = value.trim().replace("-", "");
        if (raw.length() != 32 || !raw.matches("[0-9a-fA-F]{32}")) {
            return null;
        }
        return raw.toLowerCase(Locale.ROOT);
    }

    private HttpResponse<String> post(String url, JsonObject body, String apiRoot)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return send(request, apiRoot);
    }

    private HttpResponse<String> send(HttpRequest request, String apiRoot)
            throws IOException, InterruptedException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new YggdrasilUnavailableException(BackendError.of(
                    "network.requestFailed", apiRoot, request.uri().toString()), e);
        }
    }

    private static boolean isServerSideFailure(int status) {
        return status >= 500;
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

    /** 服务器返回的错误描述只用于日志，不进入界面文案。 */
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