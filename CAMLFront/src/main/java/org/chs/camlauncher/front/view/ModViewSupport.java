package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.mod.model.ModDependencyType;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.front.text.UiText;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Mod 页面的展示辅助。 */
final class ModViewSupport {
    private ModViewSupport() {
    }

    static String sourceLabel(ModSource source) {
        return UiText.tr(source == ModSource.MODRINTH
                ? "mods.source.modrinth"
                : "mods.source.curseforge");
    }

    static String loaderLabel(ModLoader loader) {
        return UiText.tr("mods.loader." + loader.apiName());
    }

    static String loaderLabel(String loader) {
        ModLoader value = ModLoader.fromApiName(loader);
        return value == null ? loader : loaderLabel(value);
    }

    static String joinLoaders(List<String> loaders) {
        return loaders.isEmpty()
                ? UiText.tr("mods.value.unknown")
                : loaders.stream().map(ModViewSupport::loaderLabel).collect(Collectors.joining(" / "));
    }

    static String joinVersions(List<String> versions) {
        return versions.isEmpty()
                ? UiText.tr("mods.value.unknown")
                : String.join(" / ", versions);
    }

    static String downloads(long count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    static String dependencyType(ModDependencyType type) {
        return UiText.tr("mods.dependency." + type.name().toLowerCase(Locale.ROOT));
    }
}
