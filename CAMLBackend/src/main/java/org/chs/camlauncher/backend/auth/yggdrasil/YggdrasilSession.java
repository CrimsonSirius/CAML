package org.chs.camlauncher.backend.auth.yggdrasil;

import org.chs.camlauncher.backend.auth.AuthSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 一次 Yggdrasil 登录会话：认证服务器返回的 accessToken 与选中的角色档案。
 *
 * <p>Yggdrasil 的 {@code selectedProfile.id} 是 32 位无横线十六进制字符串，
 * 而 {@link AuthSession#uuid()} 需要标准 UUID，因此这里统一补横线。</p>
 *
 * <p>{@code profiles} 是服务器返回的全部可用角色。部分服务器（如 LittleSkin）在账号
 * 拥有多个角色时不设置 {@code selectedProfile}，界面需要据此让用户选择角色。</p>
 */
public record YggdrasilSession(
        String username,
        String profileId,
        String accessToken,
        String clientToken,
        List<YggdrasilProfile> profiles
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
        profiles = profiles == null || profiles.isEmpty()
                ? List.of(new YggdrasilProfile(profileId, username))
                : List.copyOf(profiles);
    }

    public YggdrasilSession(String username, String profileId, String accessToken, String clientToken) {
        this(username, profileId, accessToken, clientToken, List.of());
    }

    public UUID uuid() {
        return profileUuid(profileId);
    }

    /** 当前选中角色在 {@link #profiles()} 中的定义；服务器未返回时按会话信息合成一个。 */
    public YggdrasilProfile selectedProfile() {
        return profiles.stream()
                .filter(profile -> profile.sameId(profileId))
                .findFirst()
                .orElseGet(() -> new YggdrasilProfile(profileId, username));
    }

    public Optional<YggdrasilProfile> profile(String profileId) {
        return profiles.stream().filter(profile -> profile.sameId(profileId)).findFirst();
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
