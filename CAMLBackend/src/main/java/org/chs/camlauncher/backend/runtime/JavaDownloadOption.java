package org.chs.camlauncher.backend.runtime;

import java.util.Objects;

/** 一个可下载的 Java 归档。 */
public record JavaDownloadOption(
        String packageId,
        String distributionId,
        String distributionName,
        JavaPackageType packageType,
        int majorVersion,
        String javaVersion,
        String distributionVersion,
        String archiveType,
        String fileName,
        String packageInfoUri,
        long size
) {
    public JavaDownloadOption {
        Objects.requireNonNull(packageId, "packageId");
        Objects.requireNonNull(distributionId, "distributionId");
        Objects.requireNonNull(packageType, "packageType");
        fileName = fileName == null ? "" : fileName;
        archiveType = archiveType == null ? "" : archiveType;
        packageInfoUri = packageInfoUri == null ? "" : packageInfoUri;
    }

    public String displayName() {
        return "Java " + majorVersion + " · " + javaVersion + " · " + packageType.displayName();
    }
}
