package org.chs.camlauncher.backend.instance;

/** 游戏数据目录的版本隔离策略。 */
public enum VersionIsolationMode {
    /** 始终使用 {@code versions/<id>} 作为游戏数据目录。 */
    FULL,
    /** 始终使用 {@code .minecraft} 根目录作为游戏数据目录。 */
    NONE
}
