package org.chs.camlauncher.language.generator;

/** Modrinth 整合包相关文案的共享英文默认值；各语言可覆盖。 */
final class ModpackTexts {
    private ModpackTexts() {
    }

    static void populateDefaults(TranslationTable t) {
        put(t, "modpacks.nav", "Modpacks");
        put(t, "modpacks.search.title", "Search modpacks");
        put(t, "modpacks.search.nameHint", "Enter a modpack name");
        put(t, "modpacks.search.loading", "Searching modpacks...");
        put(t, "modpacks.search.empty", "No matching modpacks found");
        put(t, "modpacks.search.failed", "Failed to load modpacks: %s");
        put(t, "modpacks.list.meta", "Version: %s · Downloads %s · Updated %s");
        put(t, "modpacks.detail.loading", "Loading modpack details...");
        put(t, "modpacks.detail.install", "Install modpack");
        put(t, "modpacks.detail.installing", "Installing modpack %s...");
        put(t, "modpacks.detail.installed", "Modpack installed: %s");
        put(t, "modpacks.install.instanceName", "Instance name");
        put(t, "modpacks.install.instanceNameEmpty", "Instance name cannot be empty");
        put(t, "operation.searchModpacks", "Search modpacks %s");
        put(t, "operation.fetchModpackProject", "Fetch modpack project %s");
        put(t, "operation.installModpack", "Install modpack %s");
        put(t, "modpack.error.invalidArchive", "Invalid modpack archive: %s");
        put(t, "modpack.error.missingMinecraft", "The modpack does not declare a Minecraft version");
        put(t, "modpack.error.missingFileUrl", "Modpack file has no download URL: %s");
        put(t, "modpack.error.invalidFile", "Invalid modpack file path: %s");
        put(t, "modpack.error.unsupportedLoader", "Unsupported modpack loader: %s");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
