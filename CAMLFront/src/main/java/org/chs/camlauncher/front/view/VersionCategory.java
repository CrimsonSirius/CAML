package org.chs.camlauncher.front.view;

import java.util.Locale;

/** 下载页中的版本类别。 */
enum VersionCategory {
    RELEASE("正式版"),
    SNAPSHOT("快照版"),
    OLD("远古版");

    private final String label;

    VersionCategory(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }

    static String labelOfType(String type) {
        if (type == null || type.isBlank()) {
            return "未知版本";
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "release" -> RELEASE.label;
            case "snapshot" -> SNAPSHOT.label;
            case "old_alpha", "old_beta" -> OLD.label;
            default -> type;
        };
    }
}
