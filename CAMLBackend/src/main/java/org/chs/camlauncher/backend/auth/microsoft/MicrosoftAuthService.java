package org.chs.camlauncher.backend.auth.microsoft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 微软设备码登录与 Xbox/Minecraft Services 换取流程。
 *
 * <p>只实现启动游戏所需的设备码、刷新令牌、Xbox Live、XSTS、Minecraft
 * access token、许可证和 profile 查询。网络请求全部使用 JDK HttpClient。</p>
 */
public final class MicrosoftAuthService {
    public static final String CLIENT_ID_PROPERTY = "caml.microsoft.clientId";
    public static final String CLIENT_ID_ENV = "CAML_MICROSOFT_CLIENT_ID";

    private static final Logger LOGGER = LogManager.getLogger(MicrosoftAuthService.class);
    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final String CLIENT_ID_RESOURCE = "/caml.properties";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final long MAX_LOGIN_WAIT_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final long TOKEN_EXPIRY_MARGIN_MILLIS = Duration.ofHours(1).toMillis();

    private final String clientId;
    private final HttpClient httpClient;
    private final MicrosoftEndpoints endpoints;

    public MicrosoftAuthService() {
        this(configuredClientId(), defaultClient(), MicrosoftEndpoints.DEFAULT);
    }

    MicrosoftAuthService(String clientId, HttpClient httpClient, MicrosoftEndpoints endpoints) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.endpoints = Objects.requireNonNull(endpoints, "endpoints");
    }

    /** 使用设备码完成一次完整登录。 */
    public MicrosoftSession login(Consumer<MicrosoftDeviceCode> deviceCodeListener)
            throws IOException, InterruptedException {
        requireClientId();
        JsonObject device = postFormExpectJson(endpoints.deviceCode(), Map.of(
                "client_id", clientId,
                "scope", SCOPE
        ), "Microsoft device code");
        String deviceCode = requiredString(device, "device_code", "Microsoft device code");
        String userCode = requiredString(device, "user_code", "Microsoft device code");
        String verificationUri = firstNonBlank(
                stringValue(device, "verification_uri"),
                stringValue(device, "verification_url"));
        if (verificationUri.isBlank()) {
            throw error("auth.microsoft.missingField", "verification_uri");
        }
        int expiresIn = intValue(device, "expires_in", 900);
        int interval = Math.max(0, intValue(device, "interval", 5));

        if (deviceCodeListener != null) {
            deviceCodeListener.accept(new MicrosoftDeviceCode(
                    userCode, verificationUri, expiresIn, interval));
        }

        long deadline = System.currentTimeMillis()
                + Math.min(Math.max(expiresIn, 1) * 1000L, MAX_LOGIN_WAIT_MILLIS);
        long pollInterval = Math.max(0, interval) * 1000L;
        while (true) {
            if (System.currentTimeMillis() >= deadline) {
                throw error("auth.microsoft.loginTimeout");
            }
            if (pollInterval > 0) {
                Thread.sleep(pollInterval);
            }

            HttpResponse<String> response = postForm(endpoints.deviceToken(), Map.of(
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", clientId,
                    "device_code", deviceCode
            ));
            JsonObject token = parseObject(response.body(), "Microsoft OAuth");
            String oauthError = stringValue(token, "error");
            if ("authorization_pending".equals(oauthError)) {
                continue;
            }
            if ("slow_down".equals(oauthError)) {
                pollInterval += 5000;
                continue;
            }
            if ("expired_token".equals(oauthError)) {
                throw error("auth.microsoft.loginTimeout");
            }
            if ("authorization_declined".equals(oauthError)
                    || "access_denied".equals(oauthError)) {
                throw error("auth.microsoft.loginDeclined");
            }
            if (oauthError != null && !oauthError.isBlank()) {
                if ("invalid_grant".equals(oauthError)
                        && containsIgnoreCase(stringValue(token, "error_description"),
                        "AADSTS70000")) {
                    throw error("auth.microsoft.wrongVerifyMethod");
                }
                throw error("auth.microsoft.loginFailed", oauthError);
            }
            if (!isSuccess(response.statusCode())) {
                throw error("auth.microsoft.requestFailed", "Microsoft OAuth",
                        response.statusCode());
            }

            return finishLogin(
                    requiredString(token, "access_token", "Microsoft OAuth"),
                    requiredString(token, "refresh_token", "Microsoft OAuth"));
        }
    }

    /** 使用已有 refresh token 重新获取 Minecraft 会话。 */
    public MicrosoftSession refresh(String refreshToken) throws IOException, InterruptedException {
        requireClientId();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw error("auth.microsoft.loginExpired");
        }

        HttpResponse<String> response = postForm(endpoints.refreshToken(), Map.of(
                "client_id", clientId,
                "refresh_token", refreshToken,
                "grant_type", "refresh_token",
                "scope", SCOPE
        ));
        JsonObject token = parseObject(response.body(), "Microsoft OAuth");
        String oauthError = stringValue(token, "error");
        if (oauthError != null && !oauthError.isBlank()) {
            if ("invalid_grant".equals(oauthError)
                    || containsIgnoreCase(stringValue(token, "error_description"), "AADSTS70000")) {
                throw error("auth.microsoft.loginExpired");
            }
            throw error("auth.microsoft.loginFailed", oauthError);
        }
        if (!isSuccess(response.statusCode())) {
            throw error("auth.microsoft.requestFailed", "Microsoft OAuth", response.statusCode());
        }

        return finishLogin(
                requiredString(token, "access_token", "Microsoft OAuth"),
                firstNonBlank(stringValue(token, "refresh_token"), refreshToken));
    }

    private MicrosoftSession finishLogin(String microsoftAccessToken, String refreshToken)
            throws IOException, InterruptedException {
        XboxToken xbox = authenticateXbox(microsoftAccessToken);
        String xsts = authorizeXsts(xbox);
        MinecraftToken minecraftToken = loginMinecraft(xbox.uhs(), xsts);
        requireLicense(minecraftToken.accessToken());
        JsonObject profile = fetchProfile(minecraftToken.accessToken());

        String profileIdText = requiredString(profile, "id", "Minecraft profile");
        String profileName = requiredString(profile, "name", "Minecraft profile");
        UUID profileId;
        try {
            profileId = parseUuid(profileIdText);
        } catch (IllegalArgumentException e) {
            throw error("auth.microsoft.responseInvalid", "Minecraft profile");
        }

        long expiresAt = System.currentTimeMillis()
                + Math.max(0, minecraftToken.expiresIn() * 1000L - TOKEN_EXPIRY_MARGIN_MILLIS);
        LOGGER.info("微软账号登录完成: {}", profileName);
        return new MicrosoftSession(profileName, profileId, minecraftToken.accessToken(),
                refreshToken, expiresAt);
    }

    private XboxToken authenticateXbox(String microsoftAccessToken)
            throws IOException, InterruptedException {
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", microsoftAccessToken.startsWith("d=")
                ? microsoftAccessToken
                : "d=" + microsoftAccessToken);
        JsonObject request = new JsonObject();
        request.add("Properties", properties);
        request.addProperty("RelyingParty", "http://auth.xboxlive.com");
        request.addProperty("TokenType", "JWT");

        HttpResponse<String> httpResponse = postJson(endpoints.xboxUserAuth(), request);
        if (httpResponse.statusCode() == 400) {
            throw error("auth.microsoft.wrongVerifyMethod");
        }
        if (!isSuccess(httpResponse.statusCode())) {
            throw error("auth.microsoft.requestFailed", "Xbox Live",
                    httpResponse.statusCode());
        }
        JsonObject response = parseObject(httpResponse.body(), "Xbox Live");
        String token = requiredString(response, "Token", "Xbox Live");
        String uhs = xuiValue(response, "uhs");
        if (uhs.isBlank()) {
            throw error("auth.microsoft.missingField", "Xbox UHS");
        }
        return new XboxToken(token, uhs);
    }

    private String authorizeXsts(XboxToken xbox)
            throws IOException, InterruptedException {
        JsonArray userTokens = new JsonArray();
        userTokens.add(xbox.token());
        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        properties.add("UserTokens", userTokens);
        JsonObject request = new JsonObject();
        request.add("Properties", properties);
        request.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        request.addProperty("TokenType", "JWT");

        HttpResponse<String> httpResponse = postJson(endpoints.xstsAuthorize(), request);
        JsonObject response = parseObject(httpResponse.body(), "Xbox XSTS");
        long xerr = longValue(response, "XErr", 0);
        if (xerr != 0) {
            throw xboxError(xerr);
        }
        if (!isSuccess(httpResponse.statusCode())) {
            throw error("auth.microsoft.requestFailed", "Xbox XSTS", httpResponse.statusCode());
        }

        String xstsUhs = xuiValue(response, "uhs");
        if (xstsUhs.isBlank() || !xbox.uhs().equals(xstsUhs)) {
            throw error("auth.microsoft.responseInvalid", "Xbox XSTS");
        }
        return requiredString(response, "Token", "Xbox XSTS");
    }

    private MinecraftToken loginMinecraft(String uhs, String xsts)
            throws IOException, InterruptedException {
        JsonObject request = new JsonObject();
        request.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xsts);
        HttpResponse<String> response = postJson(endpoints.minecraftLogin(), request);
        if (!isSuccess(response.statusCode())) {
            String body = response.body() == null ? "" : response.body();
            if (body.contains("ACCOUNT_SUSPENDED")) {
                throw error("auth.microsoft.accountSuspended");
            }
            throw switch (response.statusCode()) {
                case 429 -> error("auth.microsoft.tooManyRequests");
                case 403 -> error("auth.microsoft.forbidden");
                case 503 -> error("auth.microsoft.serviceUnavailable");
                default -> error("auth.microsoft.requestFailed", "Minecraft login",
                        response.statusCode());
            };
        }

        JsonObject json = parseObject(response.body(), "Minecraft login");
        return new MinecraftToken(
                requiredString(json, "access_token", "Minecraft login"),
                Math.max(0, intValue(json, "expires_in", 0)));
    }

    private void requireLicense(String minecraftAccessToken)
            throws IOException, InterruptedException {
        HttpResponse<String> response = get(endpoints.entitlements(), minecraftAccessToken);
        if (!isSuccess(response.statusCode())) {
            throw error("auth.microsoft.requestFailed", "Minecraft entitlements",
                    response.statusCode());
        }
        JsonObject json = parseObject(response.body(), "Minecraft entitlements");
        JsonElement items = json.get("items");
        if (!(items instanceof JsonArray array) || array.isEmpty()) {
            throw error("auth.microsoft.noLicense");
        }
    }

    private JsonObject fetchProfile(String minecraftAccessToken)
            throws IOException, InterruptedException {
        HttpResponse<String> response = get(endpoints.profile(), minecraftAccessToken);
        if (response.statusCode() == 404) {
            throw error("auth.microsoft.noProfile");
        }
        if (!isSuccess(response.statusCode())) {
            throw error("auth.microsoft.requestFailed", "Minecraft profile",
                    response.statusCode());
        }
        return parseObject(response.body(), "Minecraft profile");
    }

    private JsonObject postFormExpectJson(URI uri, Map<String, String> values, String operation)
            throws IOException, InterruptedException {
        HttpResponse<String> response = postForm(uri, values);
        if (!isSuccess(response.statusCode())) {
            throw error("auth.microsoft.requestFailed", operation, response.statusCode());
        }
        return parseObject(response.body(), operation);
    }

    private JsonObject postJsonExpectJson(URI uri, JsonObject body, String operation)
            throws IOException, InterruptedException {
        HttpResponse<String> response = postJson(uri, body);
        if (!isSuccess(response.statusCode())) {
            throw error("auth.microsoft.requestFailed", operation, response.statusCode());
        }
        return parseObject(response.body(), operation);
    }

    private HttpResponse<String> postForm(URI uri, Map<String, String> values)
            throws IOException, InterruptedException {
        String form = values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> postJson(URI uri, JsonObject body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(URI uri, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void requireClientId() throws BackendException {
        if (clientId.isBlank()) {
            throw error("auth.microsoft.clientIdMissing");
        }
    }

    private static JsonObject parseObject(String body, String operation) throws BackendException {
        if (body == null || body.isBlank()) {
            throw error("auth.microsoft.responseInvalid", operation);
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                throw error("auth.microsoft.responseInvalid", operation);
            }
            return element.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new BackendException(
                    BackendError.of("auth.microsoft.responseInvalid", operation), e);
        }
    }

    private static String requiredString(JsonObject object, String name, String operation)
            throws BackendException {
        String value = stringValue(object, name);
        if (value == null || value.isBlank()) {
            throw error("auth.microsoft.missingField", operation + "." + name);
        }
        return value;
    }

    private static String xuiValue(JsonObject object, String name) {
        JsonElement claims = object.get("DisplayClaims");
        if (claims == null || !claims.isJsonObject()) {
            return "";
        }
        JsonElement xui = claims.getAsJsonObject().get("xui");
        if (xui == null || !xui.isJsonArray() || xui.getAsJsonArray().isEmpty()) {
            return "";
        }
        JsonElement first = xui.getAsJsonArray().get(0);
        return first.isJsonObject() ? stringValue(first.getAsJsonObject(), name) : "";
    }

    private static BackendException xboxError(long xerr) {
        if (xerr == 2148916227L) {
            return error("auth.microsoft.xboxBanned");
        }
        if (xerr == 2148916233L) {
            return error("auth.microsoft.xboxNoAccount");
        }
        if (xerr == 2148916235L) {
            return error("auth.microsoft.xboxRegion");
        }
        if (xerr == 2148916238L) {
            return error("auth.microsoft.xboxFamily");
        }
        return error("auth.microsoft.xboxUnknown", xerr);
    }

    private static String stringValue(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        JsonElement value = object.get(name);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static long longValue(JsonObject object, String name, long fallback) {
        JsonElement value = object.get(name);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsLong() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static UUID parseUuid(String value) {
        String raw = value.trim();
        if (raw.length() == 32) {
            raw = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-"
                    + raw.substring(12, 16) + "-" + raw.substring(16, 20)
                    + "-" + raw.substring(20);
        }
        return UUID.fromString(raw);
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT)
                .contains(expected.toLowerCase(java.util.Locale.ROOT));
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : (second == null ? "" : second);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static BackendException error(String code, Object... args) {
        return new BackendException(BackendError.of(code, args));
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private static String configuredClientId() {
        String property = System.getProperty(CLIENT_ID_PROPERTY);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String environment = System.getenv(CLIENT_ID_ENV);
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }
        return bundledClientId();
    }

    private static String bundledClientId() {
        InputStream input = MicrosoftAuthService.class.getResourceAsStream(CLIENT_ID_RESOURCE);
        if (input == null) {
            return "";
        }
        try (InputStream stream = input;
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties.getProperty(CLIENT_ID_PROPERTY, "").trim();
        } catch (IOException e) {
            return "";
        }
    }

    private record MinecraftToken(String accessToken, int expiresIn) {
    }

    private record XboxToken(String token, String uhs) {
    }

    record MicrosoftEndpoints(
            URI deviceCode,
            URI deviceToken,
            URI refreshToken,
            URI xboxUserAuth,
            URI xstsAuthorize,
            URI minecraftLogin,
            URI entitlements,
            URI profile
    ) {
        static final MicrosoftEndpoints DEFAULT = new MicrosoftEndpoints(
                URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"),
                URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"),
                URI.create("https://login.live.com/oauth20_token.srf"),
                URI.create("https://user.auth.xboxlive.com/user/authenticate"),
                URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"),
                URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"),
                URI.create("https://api.minecraftservices.com/entitlements/mcstore"),
                URI.create("https://api.minecraftservices.com/minecraft/profile")
        );

        MicrosoftEndpoints {
            Objects.requireNonNull(deviceCode, "deviceCode");
            Objects.requireNonNull(deviceToken, "deviceToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
            Objects.requireNonNull(xboxUserAuth, "xboxUserAuth");
            Objects.requireNonNull(xstsAuthorize, "xstsAuthorize");
            Objects.requireNonNull(minecraftLogin, "minecraftLogin");
            Objects.requireNonNull(entitlements, "entitlements");
            Objects.requireNonNull(profile, "profile");
        }
    }
}
