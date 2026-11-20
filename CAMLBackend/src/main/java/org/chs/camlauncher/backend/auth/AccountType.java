package org.chs.camlauncher.backend.auth;

import java.util.Locale;

/** 账号类型。存储与设置文件中使用小写字符串，避免枚举名变化破坏已有配置。 */
public enum AccountType {
    /** 离线账号：用户名决定 UUID，不访问任何服务器。 */
    OFFLINE,
    /** 第三方 Yggdrasil 认证服务器账号（如 LittleSkin）。 */
    YGGDRASIL,
    /** Microsoft/Xbox 正版账号。 */
    MICROSOFT;

    public String storedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AccountType fromStored(String value) {
        if (value == null) {
            return OFFLINE;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "yggdrasil" -> YGGDRASIL;
            case "microsoft" -> MICROSOFT;
            default -> OFFLINE;
        };
    }
}

