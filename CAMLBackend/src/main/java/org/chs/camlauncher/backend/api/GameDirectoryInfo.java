package org.chs.camlauncher.backend.api;

import java.nio.file.Path;
import java.util.List;

/**
 * 游戏目录探测结果。
 *
 * <p>CAMLFront 不允许直接访问文件系统，因此目录是否存在、包含哪些已安装版本，
 * 统一由后端读取磁盘后通过该 DTO 返回给界面。</p>
 *
 * @param directory         规范化后的绝对路径
 * @param directoryExists   目录是否存在且确实是一个目录
 * @param installedVersions 已安装版本 ID 列表（按字典序排序，可能为空）
 * @param isolatedVersions  启用了版本隔离的版本 ID 列表
 *                           （游戏数据位于 {@code versions/<id>}，如 PCL、HMCL 的默认行为）
 */
public record GameDirectoryInfo(
        Path directory,
        boolean directoryExists,
        List<String> installedVersions,
        List<String> isolatedVersions
) {
    public GameDirectoryInfo {
        installedVersions = List.copyOf(installedVersions);
        isolatedVersions = List.copyOf(isolatedVersions);
    }

    /**
     * @return 该目录下是否存在可以直接启动的已安装版本
     */
    public boolean hasInstalledVersions() {
        return !installedVersions.isEmpty();
    }

    /**
     * @param versionId 版本 ID
     * @return 该版本是否启用了版本隔离
     */
    public boolean isIsolated(String versionId) {
        return isolatedVersions.contains(versionId);
    }
}