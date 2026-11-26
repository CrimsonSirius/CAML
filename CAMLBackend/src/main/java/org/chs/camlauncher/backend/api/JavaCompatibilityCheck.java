package org.chs.camlauncher.backend.api;

import java.nio.file.Path;

/** 启动前 Java 兼容性检查结果。 */
public record JavaCompatibilityCheck(
        int requiredMajorVersion,
        int selectedMajorVersion,
        Path selectedJava,
        boolean compatible
) {
    public boolean hasJava() {
        return selectedJava != null && selectedMajorVersion > 0;
    }
}
