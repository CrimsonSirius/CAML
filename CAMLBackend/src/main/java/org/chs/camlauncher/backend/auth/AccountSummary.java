package org.chs.camlauncher.backend.auth;

import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilProfile;

import java.util.List;

/**
 * 前端可见的账号摘要：只包含展示需要的字段，<b>不包含任何 token</b>。
 *
 * @param profiles   认证服务器返回的全部角色；离线账号为空
 * @param needsLogin 第三方账号没有可用凭据，需要用户重新登录
 */
public record AccountSummary(
        String id,
        String displayName,
        AccountType type,
        String uuid,
        String apiRoot,
        String serverName,
        List<YggdrasilProfile> profiles,
        boolean needsLogin
) {
    public AccountSummary {
        displayName = displayName == null ? "" : displayName;
        uuid = uuid == null ? "" : uuid;
        apiRoot = apiRoot == null ? "" : apiRoot;
        serverName = serverName == null ? "" : serverName;
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }

    public boolean yggdrasil() {
        return type == AccountType.YGGDRASIL;
    }

    /** 是否有多个角色可切换。 */
    public boolean multiProfile() {
        return profiles.size() > 1;
    }
}
