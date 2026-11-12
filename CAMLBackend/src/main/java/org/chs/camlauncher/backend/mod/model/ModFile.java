package org.chs.camlauncher.backend.mod.model;

import java.net.URI;
import java.util.List;

/** 一个可下载的 Mod 文件版本。 */
public record ModFile(
        ModSource source,
        String projectId,
        String versionId,
        String name,
        String versionNumber,
        String fileName,
        URI downloadUri,
        String sha1,
        long size,
        List<String> gameVersions,
        List<String> loaders,
        List<ModDependency> dependencies,
        String publishedAt,
        long downloads
) {
}
