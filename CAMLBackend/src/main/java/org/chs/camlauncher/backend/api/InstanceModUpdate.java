package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.mod.model.ModFile;

/**
 * 一个本地 Mod 的更新检查结果。
 *
 * @param fileName       本地基础文件名
 * @param currentVersion 本地版本号
 * @param latestVersion  当前 MC 版本/加载器下的最新版本号；无更新时等于本地版本
 * @param updateAvailable 是否存在更新
 * @param latestFile     最新版本文件；没有可更新版本时为 null
 * @param error          检查失败的提示；成功时为 null
 */
public record InstanceModUpdate(
        String fileName,
        String currentVersion,
        String latestVersion,
        boolean updateAvailable,
        ModFile latestFile,
        String error
) {
}
