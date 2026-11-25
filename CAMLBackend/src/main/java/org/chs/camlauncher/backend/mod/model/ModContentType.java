package org.chs.camlauncher.backend.mod.model;

/** 下载页可搜索并安装到实例的内容类型。 */
public enum ModContentType {
    MOD("mod", 6, "mods"),
    MODPACK("modpack", 4471, "modpacks"),
    SHADER("shader", 6552, "shaderpacks"),
    RESOURCE_PACK("resourcepack", 12, "resourcepacks"),
    WORLD("world", 17, "saves");

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

    /** Modrinth 没有 world 项目类型，地图只查询 CurseForge。 */
    public boolean supportsModrinth() {
        return this != WORLD;
    }

    /** 当前整合包首期只支持 Modrinth .mrpack。 */
    public boolean supportsCurseForge() {
        return this != MODPACK;
    }
}
