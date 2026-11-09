package org.chs.camlauncher.backend.loader.optifine;

import org.chs.camlauncher.backend.loader.forge.ForgeVersion;

import java.util.Objects;

/** OptiFine 与 Forge 版本组合的已知兼容性规则。 */
public final class OptiFineCompatibility {
    private OptiFineCompatibility() {
    }

    public static boolean isForgeCompatible(ForgeVersion forge, OptiFineVersion optiFine) {
        Objects.requireNonNull(forge, "forge");
        Objects.requireNonNull(optiFine, "optiFine");
        if (!"1.20.1".equals(optiFine.gameVersion())
                || !optiFine.version().startsWith("HD_U_I6")) {
            return true;
        }
        return compareVersions(forge.version(), "47.4.27") < 0;
    }

    public static String incompatibleMessage(ForgeVersion forge, OptiFineVersion optiFine) {
        return "Forge " + forge.version() + " 与 OptiFine " + optiFine.version()
                + " 不兼容，请选择 Forge 47.4.26 或更早版本。";
    }

    static int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftValue = parsePart(leftParts, index);
            int rightValue = parsePart(rightParts, index);
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int parsePart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
