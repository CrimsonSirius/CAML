package org.chs.camlauncher.backend.auth.injector;

import java.nio.file.Path;

/**
 * 一次启动要注入的 authlib-injector 参数。
 *
 * @param jar            authlib-injector jar 的本地路径
 * @param apiRoot        Yggdrasil 认证服务器地址（注入到 {@code -javaagent:<jar>=<apiRoot>}）
 * @param prefetchedBase64 预取的服务器 metadata（Base64）；为空表示不注入该参数
 */
public record AuthlibInjector(Path jar, String apiRoot, String prefetchedBase64) {
    public AuthlibInjector {
        if (jar == null) {
            throw new IllegalArgumentException("jar must not be null");
        }
        if (apiRoot == null || apiRoot.isBlank()) {
            throw new IllegalArgumentException("apiRoot must not be blank");
        }
        prefetchedBase64 = prefetchedBase64 == null ? "" : prefetchedBase64;
    }

    /** {@code -javaagent} 参数值（不含 {@code -javaagent:} 前缀）。 */
    public String javaAgentValue() {
        return jar.toString() + "=" + apiRoot;
    }

    public boolean hasPrefetched() {
        return !prefetchedBase64.isBlank();
    }
}

