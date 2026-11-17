package org.chs.camlauncher.backend.auth.store;

import org.chs.camlauncher.backend.auth.AccountType;

/**
 * 账号索引中的一条记录（后端内部模型，不直接交给前端）。
 *
 * <p>{@code accessToken} 在内存中是明文，落盘时由 {@link AccountStore} 用 AES-GCM
 * 加密；{@code override toString} 抹掉 token，避免任何日志把凭据打出来。</p>
 */
public record StoredAccount(
        String id,
        AccountType type,
        String displayName,
        String uuid,
        String apiRoot,
        String serverName,
        String accessToken,
        String clientToken
) {
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
    }

    public boolean hasToken() {
        return !accessToken.isBlank();
    }

    public StoredAccount withToken(String newAccessToken, String newClientToken) {
        return new StoredAccount(id, type, displayName, uuid, apiRoot, serverName,
                newAccessToken, newClientToken);
    }

    /** 主动抹掉凭据，用于“不记住登录”或退出登录。 */
    public StoredAccount withoutToken() {
        return new StoredAccount(id, type, displayName, uuid, apiRoot, serverName, "", clientToken);
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
                + "]";
    }
}

