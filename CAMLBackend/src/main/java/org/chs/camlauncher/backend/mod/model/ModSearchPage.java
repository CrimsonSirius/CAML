package org.chs.camlauncher.backend.mod.model;

import java.util.List;

/** 一页 Mod 搜索结果。 */
public record ModSearchPage(
        List<ModProjectSummary> items,
        int pageIndex,
        int pageSize,
        int totalCount,
        boolean hasNext
) {
}
