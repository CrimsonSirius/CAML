package org.chs.camlauncher.backend.mod.model;

/** 下载页可搜索并安装到实例的内容类型。 */
public enum ModContentType {
    MOD("mod", 6, "mods"),
    SHADER("shader", 6552, "shaderpacks");

    private final String modrinthProjectType;
    private final int curseForgeClassId;
    private final String directoryName;

    ModContentType(String modrinthProjectType, int curseForgeClassId, String directoryName) {
        this.modrinthProjectType = modrinthProjectType;
        this.curseForgeClassId = curseForgeClassId;
        this.directoryName = directoryName;
    }

    public String modrinthProjectType() {
        return modrinthProjectType;
    }

    public int curseForgeClassId() {
        return curseForgeClassId;
    }

    public String directoryName() {
        return directoryName;
    }
}
