package org.chs.camlauncher.backend.instance;

/**
 * 实例 resourcepacks 目录中的一个资源包。
 *
 * @param fileName  基础文件名或目录名（不含 .disabled）
 * @param version   从 pack.mcmeta 读取的 pack_format；未知时为空字符串
 * @param sizeBytes 文件大小；目录返回目录结点大小
 * @param enabled   是否为启用的资源包；带 .disabled 后缀视为禁用
 */
public record InstanceResourcePack(
        String fileName,
        String version,
        long sizeBytes,
        boolean enabled
) {
}
