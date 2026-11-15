package org.chs.camlauncher.backend.loader.quilt;

import java.util.Objects;

/** Quilt Loader 的一个可安装版本。 */
public record QuiltLoaderVersion(String version, int build) {
    public QuiltLoaderVersion {
        Objects.requireNonNull(version, "version");
        if (version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        if (build < 0) {
            throw new IllegalArgumentException("build must not be negative");
        }
    }
}

