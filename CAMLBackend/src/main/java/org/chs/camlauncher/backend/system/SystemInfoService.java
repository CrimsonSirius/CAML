package org.chs.camlauncher.backend.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** 读取操作系统与运行时信息。 */
public final class SystemInfoService {
    private static final List<Path> OS_RELEASE_FILES = List.of(
            Path.of("/etc/os-release"),
            Path.of("/usr/lib/os-release")
    );

    public SystemInfo read() {
        String osName = property("os.name");
        String osVersion = property("os.version");
        String architecture = property("os.arch");
        String javaVersion = property("java.version");
        String javaVendor = property("java.vendor");
        String normalizedName = osName.toLowerCase(Locale.ROOT);

        if (normalizedName.contains("linux")) {
            String distribution = readLinuxDistribution();
            return new SystemInfo(
                    distribution.isBlank() ? fallback(osName, "Linux") : distribution,
                    osVersion,
                    SystemInfo.VersionKind.KERNEL,
                    architecture,
                    javaVersion,
                    javaVendor
            );
        }
        if (normalizedName.contains("mac")) {
            return new SystemInfo(
                    fallback(osName, "macOS"),
                    osVersion,
                    SystemInfo.VersionKind.SYSTEM,
                    architecture,
                    javaVersion,
                    javaVendor
            );
        }
        return new SystemInfo(
                fallback(osName, "Unknown"),
                osVersion,
                SystemInfo.VersionKind.SYSTEM,
                architecture,
                javaVersion,
                javaVendor
        );
    }

    private static String readLinuxDistribution() {
        for (Path file : OS_RELEASE_FILES) {
            try {
                String name = parseOsRelease(Files.readAllLines(file));
                if (!name.isBlank()) {
                    return name;
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }
        return "";
    }

    static String parseOsRelease(List<String> lines) {
        String name = "";
        String version = "";
        String prettyName = "";
        for (String line : lines) {
            if (line == null || line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = unquote(line.substring(separator + 1).trim());
            if ("PRETTY_NAME".equals(key)) {
                prettyName = value;
            } else if ("NAME".equals(key)) {
                name = value;
            } else if ("VERSION".equals(key)) {
                version = value;
            }
        }
        if (!prettyName.isBlank()) {
            return prettyName;
        }
        if (!name.isBlank() && !version.isBlank()) {
            return name + " " + version;
        }
        return name;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String property(String key) {
        return System.getProperty(key, "").trim();
    }

    private static String fallback(String value, String defaultValue) {
        return value.isBlank() ? defaultValue : value;
    }
}
