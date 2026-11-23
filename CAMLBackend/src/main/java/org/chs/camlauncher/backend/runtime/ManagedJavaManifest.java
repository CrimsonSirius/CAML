package org.chs.camlauncher.backend.runtime;

import java.time.Instant;

/** 托管 Java 安装清单。 */
record ManagedJavaManifest(
        String name,
        String distributionId,
        String distributionName,
        String packageType,
        int majorVersion,
        String javaVersion,
        String archiveType,
        String fileName,
        String checksum,
        String installedAt
) {
    static ManagedJavaManifest from(JavaDownloadOption option, String name,
                                    String checksum) {
        return new ManagedJavaManifest(
                name,
                option.distributionId(),
                option.distributionName(),
                option.packageType().apiValue(),
                option.majorVersion(),
                option.javaVersion(),
                option.archiveType(),
                option.fileName(),
                checksum,
                Instant.now().toString()
        );
    }
}
