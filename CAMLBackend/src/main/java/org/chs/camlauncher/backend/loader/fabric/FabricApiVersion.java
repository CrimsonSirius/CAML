package org.chs.camlauncher.backend.loader.fabric;

import java.net.URI;
import java.util.Objects;

/** 与指定 Minecraft 版本兼容的 Fabric API 版本。 */
public record FabricApiVersion(
        String versionNumber,
        String name,
        String filename,
        URI downloadUri,
        String sha1,
        long size,
        String releaseTime
) {
    public FabricApiVersion {
        Objects.requireNonNull(versionNumber, "versionNumber");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(downloadUri, "downloadUri");
        Objects.requireNonNull(sha1, "sha1");
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }
}
