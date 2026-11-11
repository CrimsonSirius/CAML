package org.chs.camlauncher.backend.settings;

/** 启动器界面语言的选择方式。 */
public enum LanguageSelectionMode {
    AUTO,
    MANUAL;

    public static LanguageSelectionMode fromStored(String value) {
        return value != null && "manual".equalsIgnoreCase(value.trim())
                ? MANUAL
                : AUTO;
    }

    public String storedName() {
        return name().toLowerCase();
    }
}
