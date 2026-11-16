package org.chs.camlauncher.backend.loader.quilt;

import java.util.Objects;

/** Quilt Loader 的一个可安装版本。 */
public record QuiltLoaderVersion(String version, int build) {
    /**
     * @param version Quilt Loader 版本号，用于拼装 profile 请求
     * @param build   Quilt Meta 返回的该版本构建号，仅作元数据，不能用来比较版本新旧
     */
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