package org.chs.camlauncher.backend.mod.model;

import java.util.List;

/** Mod 详情页所需的项目信息与全部版本。 */
public record ModProjectDetail(
        ModProjectSummary project,
        String body,
        List<ModFile> files
) {
}
