package org.chs.camlauncher.backend.auth.yggdrasil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务器 metadata 的拉取与内存缓存。
 *
 * <p>metadata 只在切换服务器或首次登录时拉取；缓存按规范化后的 apiRoot 作为键，
 * 有效期与启动器进程生命周期一致。皮肤站临时不可用时会回退到最近一次成功结果，
 * 避免因为网络波动导致已经登录的账号无法启动游戏。</p>
 */
public final class AuthServerService {
    /** 内置预设：仅保留 LittleSkin，其他服务器通过自定义地址添加。 */
    public static final String LITTLESKIN_API_ROOT = "https://littleskin.cn/api/yggdrasil";

    private final YggdrasilClient client;
    private final Map<String, AuthServerInfo> cache = new ConcurrentHashMap<>();

    public AuthServerService() {
        this(new YggdrasilClient());
    }

    public AuthServerService(YggdrasilClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /** 拉取并缓存 metadata。 */
    public AuthServerInfo fetch(String apiRoot) throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        AuthServerInfo info = client.fetchServer(normalized);
        cache.put(normalized, info);
        return info;
    }

    /** 优先使用缓存；没有缓存时才请求网络。 */
    public AuthServerInfo cachedOrFetch(String apiRoot) throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        AuthServerInfo cached = cache.get(normalized);
        return cached != null ? cached : fetch(normalized);
    }

    /** 网络失败时回退到最近一次成功结果；从未成功过则返回空。 */
    public AuthServerInfo fetchOrRemember(String apiRoot) {
        String normalized;
        try {
            normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        } catch (IllegalArgumentException e) {
            return null;
        }
        try {
            return fetch(normalized);
        } catch (IOException e) {
            return cache.get(normalized);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return cache.get(normalized);
        }
    }

    public void remember(AuthServerInfo info) {
        if (info != null) {
            cache.put(info.apiRoot(), info);
        }
    }

    /** 预设服务器列表（按展示顺序）。 */
    public List<AuthServerInfo> presets() {
        List<AuthServerInfo> presets = new ArrayList<>();
        AuthServerInfo cached = cache.get(LITTLESKIN_API_ROOT);
        if (cached != null) {
            presets.add(cached);
        } else {
            presets.add(new AuthServerInfo(LITTLESKIN_API_ROOT, "LittleSkin", List.of(), "{}"));
        }
        return List.copyOf(presets);
    }

    /** 预设服务器地址与名称表，供前端下拉框直接展示。 */
    public Map<String, String> presetNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (AuthServerInfo info : presets()) {
            names.put(info.apiRoot(), info.serverName());
        }
        return Map.copyOf(names);
    }
}

