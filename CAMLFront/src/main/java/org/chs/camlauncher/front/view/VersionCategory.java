package org.chs.camlauncher.front.view;

import org.chs.camlauncher.front.translation.Language;

import java.util.Locale;

/** 下载页中的版本类别。 */
enum VersionCategory {
    RELEASE("download.category.release"),
    SNAPSHOT("download.category.snapshot"),
    OLD("download.category.old");

    private final String labelKey;

    VersionCategory(String labelKey) {
        this.labelKey = labelKey;
    }

    String label() {
        return Language.tr(labelKey);
    }

    static String labelOfType(String type) {
        if (type == null || type.isBlank()) {
            return Language.tr("download.category.unknown");
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "release" -> RELEASE.label();
            case "snapshot" -> SNAPSHOT.label();
            case "old_alpha", "old_beta" -> OLD.label();
            default -> type;
        };
    }
}