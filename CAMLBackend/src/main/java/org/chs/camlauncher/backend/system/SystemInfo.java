package org.chs.camlauncher.backend.system;

import java.util.Locale;

/** 启动器运行环境信息；由后端读取，前端只负责展示。 */
public record SystemInfo(
        String operatingSystem,
        String version,
        VersionKind versionKind,
        String architecture,
        String javaVersion,
        String javaVendor
) {
    public enum VersionKind {
        SYSTEM,
        KERNEL
    }

    public boolean isLinux() {
        return operatingSystem != null
                && operatingSystem.toLowerCase(Locale.ROOT).contains("linux");
    }
}
