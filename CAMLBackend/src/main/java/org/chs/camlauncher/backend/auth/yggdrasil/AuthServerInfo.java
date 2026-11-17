package org.chs.camlauncher.backend.auth.yggdrasil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * 第三方 Yggdrasil 认证服务器的 metadata。
 *
 * <p>{@code rawMetadata} 保留服务器返回的原始 JSON，用于 authlib-injector 的
 * {@code -Dauthlibinjector.yggdrasil.prefetched} 参数：游戏启动时无需再次联网
 * 拉取 metadata，皮肤站短暂不可用也不影响进入游戏。</p>
 */
public record AuthServerInfo(
        String apiRoot,
        String serverName,
        List<String> skinDomains,
        String rawMetadata
) {
    public AuthServerInfo {
        apiRoot = normalizeApiRoot(apiRoot);
        String name = serverName == null ? "" : serverName.trim();
        serverName = name.isEmpty() ? hostOf(apiRoot) : name;
        skinDomains = skinDomains == null ? List.of() : List.copyOf(skinDomains);
        rawMetadata = rawMetadata == null || rawMetadata.isBlank() ? "{}" : rawMetadata;
    }

    /** authlib-injector 的 {@code -Dauthlibinjector.yggdrasil.prefetched} 参数值。 */
    public String prefetchedBase64() {
        return Base64.getEncoder().encodeToString(rawMetadata.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 规范化认证服务器地址：保留 scheme 与路径，去掉多余空白和结尾斜杠。
     *
     * @throws IllegalArgumentException 地址为空、缺少 scheme 或不可解析
     */
    public static String normalizeApiRoot(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("apiRoot must not be blank");
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("apiRoot must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("apiRoot is not a valid URI: " + value, e);
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("apiRoot has no scheme: " + value);
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
            throw new IllegalArgumentException("apiRoot must use http or https: " + value);
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("apiRoot has no host: " + value);
        }
        return trimmed;
    }

    private static String hostOf(String apiRoot) {
        String host = URI.create(apiRoot).getHost();
        return host == null ? apiRoot : host;
    }
}

