package org.chs.camlauncher.backend.metadata;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RuleEvaluator {
    private final String osName;
    private final String osArch;
    private final String osVersion;
    private final Map<String, Boolean> features;

    public RuleEvaluator() {
        this(currentOsName(), currentOsArch(), System.getProperty("os.version", ""), Map.of());
    }

    public RuleEvaluator(String osName, String osArch, String osVersion, Map<String, Boolean> features) {
        this.osName = Objects.requireNonNull(osName, "osName");
        this.osArch = Objects.requireNonNull(osArch, "osArch");
        this.osVersion = Objects.requireNonNull(osVersion, "osVersion");
        this.features = features == null ? Map.of() : Map.copyOf(features);
    }

    public boolean isAllowed(List<Rule> rules) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }

        boolean allowed = false;
        for (Rule rule : rules) {
            if (matches(rule)) {
                allowed = rule.action() == Rule.Action.ALLOW;
            }
        }
        return allowed;
    }

    private boolean matches(Rule rule) {
        if (rule == null) {
            return false;
        }

        Rule.Os os = rule.os();
        if (os != null) {
            if (os.name() != null && !os.name().equalsIgnoreCase(osName)) {
                return false;
            }
            if (os.arch() != null && !os.arch().equalsIgnoreCase(osArch)) {
                return false;
            }
            if (os.version() != null && !os.version().isBlank()) {
                try {
                    if (!Pattern.compile(os.version()).matcher(osVersion).find()) {
                        return false;
                    }
                } catch (PatternSyntaxException e) {
                    return false;
                }
            }
        }

        for (Map.Entry<String, Boolean> feature : rule.features().entrySet()) {
            if (features.getOrDefault(feature.getKey(), false) != feature.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static String currentOsName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "osx";
        }
        if (os.contains("linux")) {
            return "linux";
        }
        return os;
    }

    public static String currentOsArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return switch (arch) {
            case "amd64", "x86_64", "x64" -> "x86_64";
            case "x86", "i386", "i686" -> "x86";
            case "aarch64", "arm64" -> "arm64";
            default -> arch;
        };
    }
}
