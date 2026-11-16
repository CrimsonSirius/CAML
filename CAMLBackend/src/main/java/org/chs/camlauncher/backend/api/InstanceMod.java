package org.chs.camlauncher.backend.api;

/**
 * 实例 mods 目录中的一个文件。
 *
 * @param fileName  文件名
 * @param sizeBytes 文件大小
 * @param enabled   是否为启用的 .jar 文件；.jar.disabled 视为禁用
 */
public record InstanceMod(String fileName, long sizeBytes, boolean enabled) {
}
