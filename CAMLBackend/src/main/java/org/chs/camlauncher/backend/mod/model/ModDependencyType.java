package org.chs.camlauncher.backend.mod.model;

/** Mod 版本依赖类型。 */
public enum ModDependencyType {
    REQUIRED,
    OPTIONAL,
    INCOMPATIBLE,
    EMBEDDED,
    UNKNOWN
}
