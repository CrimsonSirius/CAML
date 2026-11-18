package org.chs.camlauncher.backend.auth.yggdrasil;

/**
 * 认证服务器上的一个角色档案（Minecraft profile）。
 *
 * <p>{@code id} 是 Yggdrasil 返回的 32 位无横线十六进制字符串，{@code name} 是角色名。
 * 一个账号可以有多个角色，accessToken 每次只绑定其中一个。</p>
 */
public record YggdrasilProfile(String id, String name) {
    public YggdrasilProfile {
        id = id == null ? "" : id.trim();
        name = name == null ? "" : name.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("profile id must not be blank");
        }
    }

    /** 忽略大小写比较角色 id，用于判断“当前选中角色”是否仍是同一个。 */
    public boolean sameId(String profileId) {
        return profileId != null && id.equalsIgnoreCase(profileId.trim());
    }
}
