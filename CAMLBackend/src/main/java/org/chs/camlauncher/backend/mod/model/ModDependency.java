package org.chs.camlauncher.backend.mod.model;

/** 某个 Mod 文件声明的依赖。 */
public record ModDependency(
        ModSource source,
        String projectId,
        String versionId,
        String name,
        ModDependencyType type
) {
}
