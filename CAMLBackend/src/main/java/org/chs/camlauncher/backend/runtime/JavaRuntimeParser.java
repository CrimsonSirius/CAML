package org.chs.camlauncher.backend.runtime;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class JavaRuntimeParser {
    private JavaRuntimeParser() {
    }

    public static Map<String, String> parseProperties(String output) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (String line : output.lines().toList()) {
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                properties.put(key, value);
            }
        }
        return Map.copyOf(properties);
    }

    public static int parseMajorVersion(String version) {
        if (version == null || version.isBlank()) {
            return -1;
        }

        String[] parts = version.split("[._+\\-]");
        try {
            if (parts.length > 1 && "1".equals(parts[0])) {
                return Integer.parseInt(parts[1]);
            }
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String normalizeArchitecture(String architecture) {
        if (architecture == null) {
            return "";
        }

        String normalized = architecture.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "amd64", "x86_64", "x64" -> "x86_64";
            case "x86", "i386", "i686" -> "x86";
            case "aarch64", "arm64" -> "arm64";
            default -> normalized;
        };
    }

    public static int parseBits(String dataModel, String architecture) {
        if (dataModel != null) {
            try {
                int bits = Integer.parseInt(dataModel.trim());
                if (bits == 32 || bits == 64) {
                    return bits;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return "x86".equals(normalizeArchitecture(architecture)) ? 32 : 64;
    }
}
