package org.chs.camlauncher.backend.mod.model;

/** 下载内容搜索条件；source 为 null 时同时查询两个官方来源。 */
public record ModSearchQuery(
        String text,
        ModSource source,
        String gameVersion,
        ModLoader loader,
        int pageIndex,
        int pageSize,
        ModContentType type
) {
    public ModSearchQuery {
        type = type == null ? ModContentType.MOD : type;
    }

    /** 兼容原有 Mod 搜索调用。 */
    public ModSearchQuery(String text, ModSource source, String gameVersion, ModLoader loader,
                          int pageIndex, int pageSize) {
        this(text, source, gameVersion, loader, pageIndex, pageSize, ModContentType.MOD);
    }
}
