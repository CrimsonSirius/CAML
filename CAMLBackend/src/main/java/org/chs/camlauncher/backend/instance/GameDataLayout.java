package org.chs.camlauncher.backend.instance;

import java.nio.file.Path;
import java.util.Objects;

/** 按 FULL/NONE 解析游戏数据目录。 */
public final class GameDataLayout {
    private GameDataLayout() {
    }

    /**
     * 按显式隔离策略解析启动时应使用的游戏数据目录。
     *
     * @param gameDirectory    {@code .minecraft} 根目录
     * @param versionDirectory {@code <gameDirectory>/versions/<id>}
     * @param mode             隔离策略
     * @return 完全隔离返回版本目录，不隔离返回根目录
     */
    public static Path resolve(Path gameDirectory, Path versionDirectory, VersionIsolationMode mode) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(versionDirectory, "versionDirectory");
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case FULL -> versionDirectory;
            case NONE -> gameDirectory;
        };
    }
}

