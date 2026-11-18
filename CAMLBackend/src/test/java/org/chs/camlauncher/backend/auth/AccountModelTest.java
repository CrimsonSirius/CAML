package org.chs.camlauncher.backend.auth;

import org.chs.camlauncher.backend.auth.store.AccountStore;
import org.chs.camlauncher.backend.auth.store.StoredAccount;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilSession;
import org.chs.camlauncher.backend.json.JsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 第三方账号相关的基础模型、地址规范化与本地账号存储。 */
class AccountModelTest {
    @Test
    void normalizesApiRootAndRejectsInvalidAddresses() {
        assertEquals("https://littleskin.cn/api/yggdrasil",
                AuthServerInfo.normalizeApiRoot("  https://littleskin.cn/api/yggdrasil/  "));
        assertThrows(IllegalArgumentException.class,
                () -> AuthServerInfo.normalizeApiRoot(""));
        assertThrows(IllegalArgumentException.class,
                () -> AuthServerInfo.normalizeApiRoot("littleskin.cn/api/yggdrasil"));
        assertThrows(IllegalArgumentException.class,
                () -> AuthServerInfo.normalizeApiRoot("ftp://littleskin.cn/api/yggdrasil"));
    }

    @Test
    void prefetchedMetadataIsBase64OfRawJson() {
        String raw = "{\"meta\":{\"serverName\":\"LittleSkin\"}}";
        AuthServerInfo info = new AuthServerInfo("https://example.com/api/yggdrasil",
                "LittleSkin", List.of("example.com"), raw);

        assertEquals(raw, new String(java.util.Base64.getDecoder()
                .decode(info.prefetchedBase64()), StandardCharsets.UTF_8));
        assertEquals("LittleSkin", info.serverName());
    }

    @Test
    void convertsThirtyTwoCharProfileIdToUuid() {
        UUID uuid = YggdrasilSession.profileUuid("2c1f0b1d4c364f6c8c19ad0b9f77f0a1");
        assertEquals("2c1f0b1d-4c36-4f6c-8c19-ad0b9f77f0a1", uuid.toString());
        assertEquals(uuid, YggdrasilSession.profileUuid("2c1f0b1d-4c36-4f6c-8c19-ad0b9f77f0a1"));
    }

    @Test
    void yggdrasilSessionUsesMojangUserType() {
        YggdrasilSession session = new YggdrasilSession("Steve",
                "2c1f0b1d4c364f6c8c19ad0b9f77f0a1", "access-token", "client-token");
        AuthSession auth = session.toAuthSession();

        assertEquals("Steve", auth.username());
        assertEquals("mojang", auth.userType());
        assertEquals("{}", auth.userProperties());
        assertEquals("access-token", auth.accessToken());
    }

    @Test
    void accountStoreRoundTripsAndEncryptsTokens(@TempDir Path dataDirectory) throws Exception {
        AccountStore store = new AccountStore(dataDirectory, new JsonService());
        StoredAccount account = new StoredAccount("yggdrasil:abc", AccountType.YGGDRASIL,
                "Steve", "2c1f0b1d4c364f6c8c19ad0b9f77f0a1",
                "https://littleskin.cn/api/yggdrasil", "LittleSkin",
                "plaintext-access-token", "client-token");

        store.upsert(account);

        StoredAccount loaded = store.find("yggdrasil:abc").orElseThrow();
        assertEquals("Steve", loaded.displayName());
        assertEquals("plaintext-access-token", loaded.accessToken());
        assertEquals(AccountType.YGGDRASIL, loaded.type());

        String raw = Files.readString(store.accountsFile(), StandardCharsets.UTF_8);
        assertFalse(raw.contains("plaintext-access-token"), "落盘内容不应包含明文 token");
        assertTrue(raw.contains("enc:v1:"));

        store.remove("yggdrasil:abc");
        assertTrue(store.load().isEmpty());
    }

    @Test
    void offlineAccountStoreKeepsEmptyToken(@TempDir Path dataDirectory) throws Exception {
        AccountStore store = new AccountStore(dataDirectory, new JsonService());
        store.upsert(new StoredAccount("offline:" + UUID.nameUUIDFromBytes(
                "OfflinePlayer:Steve".getBytes(StandardCharsets.UTF_8)),
                AccountType.OFFLINE, "Steve", "", "", "", "", ""));

        StoredAccount loaded = store.load().get(0);
        assertEquals(AccountType.OFFLINE, loaded.type());
        assertFalse(loaded.hasToken());
    }
}
