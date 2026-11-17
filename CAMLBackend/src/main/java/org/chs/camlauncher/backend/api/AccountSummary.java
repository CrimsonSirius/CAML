package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.auth.AccountType;

/**
 * 前端可见的账号摘要：只包含展示需要的字段，<b>不包含任何 token</b>。
 */
public record AccountSummary(
        String id,
        String displayName,
        AccountType type,
        String uuid,
        String apiRoot,
        String serverName
) {
    public AccountSummary {
        displayName = displayName == null ? "" : displayName;
        uuid = uuid == null ? "" : uuid;
        apiRoot = apiRoot == null ? "" : apiRoot;
        serverName = serverName == null ? "" : serverName;
    }

    public boolean yggdrasil() {
        return type == AccountType.YGGDRASIL;
    }
}

