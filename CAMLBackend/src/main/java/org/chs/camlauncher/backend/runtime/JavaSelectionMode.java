package org.chs.camlauncher.backend.runtime;

/** 游戏 Java 的选择方式。 */
public enum JavaSelectionMode {
    AUTO,
    MANUAL;

    public static JavaSelectionMode fromStored(String value) {
        return value != null && "manual".equalsIgnoreCase(value.trim())
                ? MANUAL
                : AUTO;
    }

    public String storedName() {
        return name().toLowerCase();
    }
}
