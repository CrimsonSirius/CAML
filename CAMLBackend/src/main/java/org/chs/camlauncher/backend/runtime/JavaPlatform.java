package org.chs.camlauncher.backend.runtime;

import java.util.Locale;

/** 当前 Java 平台及 Foojay 参数。 */
public record JavaPlatform(String operatingSystem, String architecture,
                           String foojayArchitecture, String archiveType) {

    public static JavaPlatform current() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String operatingSystem = os.contains("win") ? "windows"
                : os.contains("mac") ? "macos" : "linux";
        String architecture = JavaRuntimeParser.normalizeArchitecture(
                System.getProperty("os.arch", ""));
        String foojayArchitecture = switch (architecture) {
            case "x86_64" -> "x64";
            case "arm64" -> "aarch64";
            case "x86" -> "x86";
            default -> architecture;
        };
        return new JavaPlatform(operatingSystem, architecture, foojayArchitecture,
                "windows".equals(operatingSystem) ? "zip" : "tar.gz");
    }

    public String key() {
        return operatingSystem + "-" + architecture;
    }
}
