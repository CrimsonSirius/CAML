package org.chs.camlauncher.backend.mod.model;

/** Mod 搜索条件；source 为 null 时同时查询两个官方来源。 */
public record ModSearchQuery(
        String text,
        ModSource source,
        String gameVersion,
        ModLoader loader,
        int pageIndex,
        int pageSize
) {
}
