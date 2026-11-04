package org.chs.camlauncher.backend.instance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 判断某个已安装版本是否启用了“版本隔离”。
 *
 * <p>常见第三方启动器（PCL、HMCL 等）会把游戏数据放在版本目录内，
 * 即 {@code versions/<id>/saves}、{@code versions/<id>/options.txt} 等，
 * 而不是 {@code .minecraft} 根目录。启动这类版本时必须把
 * {@code --gameDir} 指向版本目录，否则游戏看不到原有存档与设置。</p>
 *
 * <p>注意：{@code libraries}、{@code assets} 始终位于 {@code .minecraft} 根目录，
 * 只有“游戏数据目录”会随隔离而改变。</p>
 */
public final class GameDataLayout {
    /**
     * 版本目录中出现任意一个即视为隔离布局。
     * 这些都是 Minecraft 只会写入“游戏目录”的条目。
     */
    private static final List<String> ISOLATION_MARKERS = List.of(
            "options.txt",
            "saves",
            "logs",
            "mods",
            "config",
            "resourcepacks",
            "shaderpacks",
            "usercache.json",
            "command_history.txt"
    );

    private GameDataLayout() {
    }

    /**
     * @param versionDirectory {@code <gameDirectory>/versions/<id>}
     * @return 版本目录是否被当作独立的游戏数据目录
     */
    public static boolean isIsolated(Path versionDirectory) {
        Objects.requireNonNull(versionDirectory, "versionDirectory");
        if (!Files.isDirectory(versionDirectory)) {
            return false;
        }
        for (String marker : ISOLATION_MARKERS) {
            if (Files.exists(versionDirectory.resolve(marker))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析启动时应使用的游戏数据目录。
     *
     * @param gameDirectory    {@code .minecraft} 根目录
     * @param versionDirectory {@code <gameDirectory>/versions/<id>}
     * @return 隔离布局返回版本目录，否则返回根目录
     */
    public static Path resolve(Path gameDirectory, Path versionDirectory) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(versionDirectory, "versionDirectory");
        return isIsolated(versionDirectory) ? versionDirectory : gameDirectory;
    }
}

