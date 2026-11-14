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
        List<URI> downloadUris,
        String sha1,
        String md5,
        long size,
        List<String> gameVersions,
        List<String> loaders,
        List<ModDependency> dependencies,
        String publishedAt,
        long downloads
) {
    public ModFile {
        downloadUris = downloadUris == null ? List.of() : List.copyOf(downloadUris);
    }

    public boolean hasDownloadUri() {
        return !downloadUris.isEmpty();
    }

    /** 首个候选地址；仅为兼容现有调用方，下载时优先使用完整候选列表。 */
    public URI downloadUri() {
        return downloadUris.isEmpty() ? null : downloadUris.getFirst();
    }
}
