package org.chs.camlauncher.backend.config;

import java.nio.file.Path;
import java.util.Objects;

public record BackendConfig(Path dataDirectory, Path gameDirectory) {
    public BackendConfig {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
    }

    public static BackendConfig defaults() {
        Path userHome = Path.of(System.getProperty("user.home"));
        return new BackendConfig(userHome.resolve(".caml"), defaultGameDirectory(userHome));
    }

    /**
     * 游戏目录统一放在用户主目录下。
     *
     * <p>Linux 与 Windows 一致：发布包会把启动器装到只读的受管目录（Windows 默认
     * {@code C:\Program Files\CAML}），因此不能再以启动器所在目录为基准，否则普通用户
     * 无权写入版本、存档和日志。
     */
    static Path defaultGameDirectory(Path userHome) {
        Objects.requireNonNull(userHome, "userHome");
        return userHome.resolve(".minecraft");
    }
}