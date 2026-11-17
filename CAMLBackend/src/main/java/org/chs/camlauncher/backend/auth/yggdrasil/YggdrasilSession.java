package org.chs.camlauncher.backend.auth.yggdrasil;

import org.chs.camlauncher.backend.auth.AuthSession;

import java.util.UUID;

/**
 * 一次 Yggdrasil 登录会话：认证服务器返回的 accessToken 与选中的角色档案。
 *
 * <p>Yggdrasil 的 {@code selectedProfile.id} 是 32 位无横线十六进制字符串，
 * 而 {@link AuthSession#uuid()} 需要标准 UUID，因此这里统一补横线。</p>
 */
public record YggdrasilSession(
        String username,
        String profileId,
        String accessToken,
        String clientToken
) {
    public YggdrasilSession {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
    }

    public UUID uuid() {
        return profileUuid(profileId);
    }

    /**
     * 转换为启动参数使用的会话。
     *
     * <p>{@code userType} 必须是 {@code mojang}：authlib-injector 与部分游戏版本
     * 依赖该值走皮肤/会话路径；离线账号仍使用 {@code legacy}。</p>
     */
    public AuthSession toAuthSession() {
        return new AuthSession(username, uuid(), accessToken, "mojang", "{}");
    }

    /** 把 32 位无横线的角色 ID 转换为标准 UUID；已带横线时直接解析。 */
    public static UUID profileUuid(String profileId) {
        String raw = profileId == null ? "" : profileId.trim();
        if (raw.length() == 32) {
            String dashed = raw.substring(0, 8)
                    + "-" + raw.substring(8, 12)
                    + "-" + raw.substring(12, 16)
                    + "-" + raw.substring(16, 20)
                    + "-" + raw.substring(20);
            return UUID.fromString(dashed);
        }
        return UUID.fromString(raw);
    }
}

