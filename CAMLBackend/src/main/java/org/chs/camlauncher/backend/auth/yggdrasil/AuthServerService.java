package org.chs.camlauncher.backend.auth.yggdrasil;

import org.chs.camlauncher.backend.auth.store.AuthServerStore;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务器 metadata 的拉取与缓存。
 *
 * <p>回退顺序是“网络 → 内存 → 磁盘”：内存缓存只在当前进程有效，磁盘缓存
 * （{@link AuthServerStore}）保证冷启动或皮肤站维护期间仍能拿到 {@code prefetched}，
 * 从而不影响启动与皮肤加载。</p>
 */
public final class AuthServerService {
    /** 内置预设：仅保留 LittleSkin，其他服务器通过自定义地址添加。 */
    public static final String LITTLESKIN_API_ROOT = "https://littleskin.cn/api/yggdrasil";
    private static final String LITTLESKIN_NAME = "LittleSkin";

    private final YggdrasilClient client;
    private final AuthServerStore store;
    private final Map<String, AuthServerInfo> cache = new ConcurrentHashMap<>();

    public AuthServerService(AuthServerStore store) {
        this(store, new YggdrasilClient());
    }

    public AuthServerService(AuthServerStore store, YggdrasilClient client) {
        this.store = Objects.requireNonNull(store, "store");
        this.client = Objects.requireNonNull(client, "client");
    }

    /** 强制从网络拉取并写入内存与磁盘缓存。 */
    public AuthServerInfo fetch(String apiRoot) throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        AuthServerInfo info = client.fetchServer(normalized);
        remember(info);
        return info;
    }

    /**
     * 尽力而为地获取 metadata：网络失败时回退到内存缓存、再回退到磁盘缓存。
     *
     * @return 从未成功获取过时返回 {@code null}，调用方应跳过 prefetched
     */
    public AuthServerInfo fetchOrRemember(String apiRoot) {
        String normalized;
        try {
            normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        } catch (IllegalArgumentException e) {
            return null;
        }
        try {
            return fetch(normalized);
        } catch (BackendException e) {
            // metadata 异常（含 4xx/5xx）都属于“暂时拿不到”，交给缓存兜底。
            return fallback(normalized);
        } catch (IOException e) {
            return fallback(normalized);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback(normalized);
        }
    }

    /** 只查缓存（内存 → 磁盘），不访问网络。 */
    public Optional<AuthServerInfo> cached(String apiRoot) {
        String normalized;
        try {
            normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        AuthServerInfo inMemory = cache.get(normalized);
        if (inMemory != null) {
            return Optional.of(inMemory);
        }
        Optional<AuthServerInfo> onDisk = store.find(normalized);
        onDisk.ifPresent(info -> cache.put(normalized, info));
        return onDisk;
    }

    public void remember(AuthServerInfo info) {
        if (info == null) {
            return;
        }
        cache.put(info.apiRoot(), info);
        store.save(info);
    }

    /** 预设服务器地址与名称表，供前端下拉框直接展示。 */
    public Map<String, String> presetNames() {
        Map<String, String> names = new LinkedHashMap<>();
        String name = cached(LITTLESKIN_API_ROOT)
                .map(AuthServerInfo::serverName)
                .filter(value -> !value.isBlank())
                .orElse(LITTLESKIN_NAME);
        names.put(LITTLESKIN_API_ROOT, name);
        return names;
    }

    private AuthServerInfo fallback(String normalized) {
        AuthServerInfo inMemory = cache.get(normalized);
        if (inMemory != null) {
            return inMemory;
        }
        return store.find(normalized)
                .map(info -> {
                    cache.put(normalized, info);
                    return info;
                })
                .orElse(null);
    }
}

