package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.mod.model.ModLoader;

/** 实例对应的 Minecraft 版本和加载器。 */
public record InstanceEnvironment(String gameVersion, ModLoader loader) {
}
