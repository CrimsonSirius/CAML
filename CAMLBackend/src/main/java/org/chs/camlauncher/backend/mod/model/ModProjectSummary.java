package org.chs.camlauncher.backend.mod.model;

import java.util.List;

/** 搜索结果中的 Mod 项目摘要。 */
public record ModProjectSummary(
        ModSource source,
        String projectId,
        String slug,
        String name,
        String summary,
        String author,
        String iconUrl,
        long downloads,
        String updatedAt,
        List<String> categories,
        List<String> gameVersions,
        List<String> loaders,
        String pageUrl
) {
}
