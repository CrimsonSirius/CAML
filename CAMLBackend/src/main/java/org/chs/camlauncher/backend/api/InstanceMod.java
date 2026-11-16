package org.chs.camlauncher.backend.api;

/**
 * 实例 mods 目录中的一个文件。
 *
 * @param fileName  基础文件名（不含 .disabled）
 * @param version   本地元数据中的版本号；未知时为空字符串
 * @param sizeBytes 文件大小
 * @param enabled   是否为启用的 .jar 文件；.jar.disabled 或配置禁用视为禁用
 */
public record InstanceMod(String fileName, String version, long sizeBytes, boolean enabled) {
}
