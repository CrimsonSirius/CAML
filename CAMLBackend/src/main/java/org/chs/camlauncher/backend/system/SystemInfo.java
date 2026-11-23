package org.chs.camlauncher.backend.system;

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
}
