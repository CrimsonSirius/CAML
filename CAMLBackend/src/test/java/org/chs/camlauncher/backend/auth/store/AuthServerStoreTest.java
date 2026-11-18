package org.chs.camlauncher.backend.auth.store;

import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.backend.json.JsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 认证服务器 metadata 的磁盘缓存：冷启动回退与损坏容错。 */
class AuthServerStoreTest {
    private static final String API_ROOT = "https://littleskin.cn/api/yggdrasil";

    @Test
    void roundTripsMetadataForColdStart(@TempDir Path dataDirectory) {
        AuthServerStore store = new AuthServerStore(dataDirectory, new JsonService());
        String raw = "{\"meta\":{\"serverName\":\"LittleSkin\"},\"skinDomains\":[\"littleskin.cn\"]}";
        store.save(new AuthServerInfo(API_ROOT, "LittleSkin", List.of("littleskin.cn"), raw));

        // 新建实例模拟“重启启动器”：内存缓存不再存在，只能命中磁盘。
        AuthServerStore reopened = new AuthServerStore(dataDirectory, new JsonService());
        AuthServerInfo restored = reopened.find(API_ROOT).orElseThrow();

        assertEquals("LittleSkin", restored.serverName());
        assertEquals(List.of("littleskin.cn"), restored.skinDomains());
        assertEquals(raw, restored.rawMetadata(), "prefetched 依赖原始 JSON，必须原样保存");
        assertTrue(restored.hasMetadata());
    }

    @Test
    void placeholderMetadataIsNotPrefetched(@TempDir Path dataDirectory) {
        AuthServerStore store = new AuthServerStore(dataDirectory, new JsonService());
        store.save(new AuthServerInfo(API_ROOT, "LittleSkin", List.of(), "{}"));

        AuthServerInfo restored = store.find(API_ROOT).orElseThrow();
        assertEquals("", restored.prefetchedBase64(),
                "占位 metadata 不得生成 prefetched，否则会注入空 metadata");
        assertFalse(restored.hasMetadata());
    }

    @Test
    void corruptCacheIsTreatedAsEmpty(@TempDir Path dataDirectory) throws Exception {
        AuthServerStore store = new AuthServerStore(dataDirectory, new JsonService());
        store.save(new AuthServerInfo(API_ROOT, "LittleSkin", List.of(), "{\"a\":1}"));
        Files.writeString(store.file(), "{ not json", StandardCharsets.UTF_8);

        assertTrue(store.find(API_ROOT).isEmpty(), "缓存损坏时按未缓存处理，不影响启动");
    }

    @Test
    void unknownHostIsIgnored(@TempDir Path dataDirectory) {
        AuthServerStore store = new AuthServerStore(dataDirectory, new JsonService());
        assertTrue(store.find("not-a-url").isEmpty());
        assertTrue(store.find(null).isEmpty());
    }
}

