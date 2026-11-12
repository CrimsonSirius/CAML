package org.chs.camlauncher.backend.mod.model;

import java.util.Locale;

/** Mod 搜索与版本匹配使用的加载器。 */
public enum ModLoader {
    FABRIC("fabric", 4),
    FORGE("forge", 1),
    NEOFORGE("neoforge", 6),
    QUILT("quilt", 5);

    private final String apiName;
    private final int curseForgeType;

    ModLoader(String apiName, int curseForgeType) {
        this.apiName = apiName;
        this.curseForgeType = curseForgeType;
    }

    public String apiName() {
        return apiName;
    }

    public int curseForgeType() {
        return curseForgeType;
    }

    public static ModLoader fromApiName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ModLoader loader : values()) {
            if (loader.apiName.equals(normalized)) {
                return loader;
            }
        }
        return null;
    }
}
