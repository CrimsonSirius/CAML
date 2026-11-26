package org.chs.camlauncher.backend.auth.store;

import org.chs.camlauncher.backend.auth.AccountType;
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilProfile;

import java.util.List;
import java.util.Optional;

/**
 * 账号索引中的一条记录（后端内部模型，不直接交给前端）。
 *
 * <p>{@code accessToken} 和 {@code refreshToken} 在内存中是明文，落盘时由
 * {@link AccountStore} 用 AES-GCM 加密；{@code override toString} 抹掉凭据，
 * 避免任何日志把 token 打出来。</p>
 *
 * <p>{@code profiles} 保存认证服务器返回的全部角色，{@code uuid} 是当前选中的角色 id。
 * 这样账号 id 可以在切换角色时保持不变，避免同一账号在列表里变成多个条目。</p>
 */
public record StoredAccount(
        String id,
        AccountType type,
        String displayName,
        String uuid,
        String apiRoot,
        String serverName,
        String accessToken,
        String clientToken,
        String refreshToken,
        long expiresAt,
        List<YggdrasilProfile> profiles
) {
    public StoredAccount(String id, AccountType type, String displayName, String uuid,
                         String apiRoot, String serverName, String accessToken,
                         String clientToken) {
        this(id, type, displayName, uuid, apiRoot, serverName, accessToken, clientToken,
                "", 0, List.of());
    }

    public StoredAccount(String id, AccountType type, String displayName, String uuid,
                         String apiRoot, String serverName, String accessToken,
                         String clientToken, String refreshToken, long expiresAt) {
        this(id, type, displayName, uuid, apiRoot, serverName, accessToken, clientToken,
                refreshToken, expiresAt, List.of());
    }

    public StoredAccount(String id, AccountType type, String displayName, String uuid,
                         String apiRoot, String serverName, String accessToken,
                         String clientToken, List<YggdrasilProfile> profiles) {
        this(id, type, displayName, uuid, apiRoot, serverName, accessToken, clientToken,
                "", 0, profiles);
    }

    public StoredAccount {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        type = type == null ? AccountType.OFFLINE : type;
        displayName = displayName == null ? "" : displayName;
        uuid = uuid == null ? "" : uuid;
        apiRoot = apiRoot == null ? "" : apiRoot;
        serverName = serverName == null ? "" : serverName;
        accessToken = accessToken == null ? "" : accessToken;
        clientToken = clientToken == null ? "" : clientToken;
        refreshToken = refreshToken == null ? "" : refreshToken;
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }

    public boolean hasToken() {
        return !accessToken.isBlank();
    }

    /** 全部角色；旧数据没有记录时退化为“当前角色”一个。 */
    public List<YggdrasilProfile> profilesOrDefault() {
        if (!profiles.isEmpty()) {
            return profiles;
        }
        if (uuid.isBlank() || displayName.isBlank()) {
            return List.of();
        }
        return List.of(new YggdrasilProfile(uuid, displayName));
    }

    /** 当前选中的角色。 */
    public Optional<YggdrasilProfile> selectedProfile() {
        if (uuid.isBlank()) {
            return Optional.empty();
        }
        return profilesOrDefault().stream()
                .filter(profile -> profile.sameId(uuid))
                .findFirst()
                .or(() -> Optional.of(new YggdrasilProfile(uuid, displayName)));
    }

    public StoredAccount withToken(String newAccessToken, String newClientToken) {
        return new StoredAccount(id, type, displayName, uuid, apiRoot, serverName,
                newAccessToken, newClientToken, refreshToken, expiresAt, profiles);
    }

    public StoredAccount withMicrosoftTokens(String newDisplayName, String newAccessToken,
                                             String newRefreshToken, long newExpiresAt) {
        return new StoredAccount(id, type, newDisplayName, uuid, apiRoot, serverName,
                newAccessToken, clientToken, newRefreshToken, newExpiresAt, profiles);
    }

    /** 切换角色：更新显示名、uuid 与角色列表，账号 id 保持不变。 */
    public StoredAccount withProfile(YggdrasilProfile profile, List<YggdrasilProfile> newProfiles,
                                     String newAccessToken, String newClientToken) {
        return new StoredAccount(id, type, profile.name(), profile.id(), apiRoot, serverName,
                newAccessToken, newClientToken, refreshToken, expiresAt, newProfiles);
    }

    @Override
    public String toString() {
        return "StoredAccount[id=" + id
                + ", type=" + type
                + ", displayName=" + displayName
                + ", uuid=" + uuid
                + ", apiRoot=" + apiRoot
                + ", serverName=" + serverName
                + ", hasToken=" + hasToken()
                + ", profiles=" + profiles.size()
                + "]";
    }
}