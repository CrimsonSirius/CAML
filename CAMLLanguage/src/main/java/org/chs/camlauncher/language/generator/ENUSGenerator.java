package org.chs.camlauncher.language.generator;

import org.chs.camlauncher.language.LanguageTranslationGenerator;
import org.chs.camlauncher.language.TranslationTable;

/** English translations. */
public final class ENUSGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "en_us";
    }

    @Override
    protected void populate(TranslationTable t) {
        // Common
        t.put("common.back", "Back");
        t.put("common.cancel", "Cancel");
        t.put("common.browse", "Browse");
        t.put("common.save", "Save");

        // Top navigation and footer
        t.put("nav.home", "Home");
        t.put("nav.downloads", "Downloads");
        t.put("nav.settings", "Settings");
        t.put("app.subtitle", "Minecraft Launcher");
        t.put("footer.status", "CAML %s · Backend %s · Data directory: %s");
        t.put("footer.backend.ready", "Ready");
        t.put("footer.backend.notReady", "Not ready");

        // Home launch form
        t.put("home.username", "Offline username");
        t.put("home.launch", "Launch game");
        t.put("home.launching", "Launching...");
        t.put("home.browseInstances", "Browse instances");
        t.put("home.manageInstances", "Manage instances");
        t.put("home.instance.current", "Current instance: %s");
        t.put("home.instance.current.none", "Current instance: not selected");
        t.put("home.gameDirectory.none", "Game directory: not set");
        t.put("home.gameDirectory.missing", "Game directory: missing · %s");
        t.put("home.gameDirectory.value", "Game directory: %s");
        t.put("home.saveLocation.none", "Save location: not set");
        t.put("home.saveLocation.fixDirectory", "Save location: fix the game directory first");
        t.put("home.saveLocation.selectInstance", "Save location: select a game instance first");
        t.put("home.saveLocation.value", "Save location: %s");
        t.put("home.saveLocation.isolated", " (full version isolation)");
        t.put("home.error.inspectDirectory", "Failed to inspect game directory: %s");
        t.put("home.error.noGameDirectory", "Save the game directory in settings first");
        t.put("home.error.missingGameDirectory", "Game directory does not exist; save a valid directory in settings");
        t.put("home.error.noInstance", "Browse and select a game instance first");
        t.put("home.error.noUsername", "Enter an offline username");

        // Log pane
        t.put("log.title", "Run log");
        t.put("log.subtitle", "Download progress and game process output");
        t.put("log.accessible", "Run log");
        t.put("log.download.installed.vanilla", "[Download] Installed instance %s (vanilla %s)");
        t.put("log.download.installed.fabric", "[Download] Installed Fabric instance %s (Loader %s)");
        t.put("log.download.installed.loader", "[Download] Installed %s instance %s");
        t.put("log.download.progress", "[Download] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[Download] %s (%d)");
        t.put("log.gameProcess.exit", "Game process exited with code: %d");
        t.put("log.gameProcess.exitError", "Game process exited abnormally with code: %d");
        t.put("log.error.operationFailed", "[%s] failed: %s");

        // Launch status
        t.put("status.starting", "Starting %s...");
        t.put("status.started", "Launch command sent");
        t.put("status.launchFailed", "Launch failed!");
        t.put("status.launchFailedDetail", "Launch failed: %s");
        t.put("error.unknown", "An unknown error occurred. Check the logs.");
        t.put("download.error.failed", "Failed to download %s after %s attempts (last address: %s)");
        t.put("network.requestFailed", "Failed to request %s (last address: %s)");
        t.put("network.timeout", "Request timed out (%s ms): %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "Failed to parse response: %s");
        t.put("network.redirectLocationMissing", "Redirect is missing Location: %s");
        t.put("network.tooManyRedirects", "Too many redirects: %s");
        t.put("network.noSource", "No download source available: %s");
        t.put("metadata.manifestEmpty", "Version manifest is empty");
        t.put("metadata.versionJsonEmpty", "Version JSON is empty");
        t.put("launch.permissionDenied", "Permission denied while running Java or accessing the game directory. Check file permissions.");
        t.put("launch.executableNotFound", "Java executable not found. Select a Java installation again.");
        t.put("launch.commandTooLong", "The launch command is too long. Shorten the game path or classpath.");
        t.put("launch.commandTooLongAfterFallback", "The launch command is too long even after argfile and manifest fallback. Shorten the game path or classpath.");
        t.put("launch.processStartFailed", "Failed to start the game process. Check the logs for details.");
        t.put("launch.manualJavaNotConfigured", "Manual Java selection is enabled, but no Java path is selected.");
        t.put("launch.manualJavaUnavailable", "The selected Java is unavailable: %s");
        t.put("launch.javaRequirementNotFound", "No Java satisfying the version requirement %s was found. Install or select Java.");

        // Version categories and download sources
        t.put("download.category.release", "Release");
        t.put("download.category.snapshot", "Snapshot");
        t.put("download.category.old", "Old");
        t.put("download.category.unknown", "Unknown version");
        t.put("download.source.mirror", "Mirror");
        t.put("download.source.official", "Official");
        t.put("download.source.custom", "Custom");

        // Version list
        t.put("download.versions.loading", "Loading %2$s list from %1$s...");
        t.put("download.versions.empty", "The download source returned no downloadable %s versions");
        t.put("download.versions.count", "%d %s versions, newest first by release time");
        t.put("download.versions.error", "Failed to load %s list: %s");
        t.put("download.versions.accessible", "%s, %s");

        // Download description
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // Download setup page
        t.put("download.setup.status.idle", "Downloads the vanilla game when no loader is selected.");
        t.put("download.setup.instanceName", "Instance name");
        t.put("download.setup.download", "Download instance");
        t.put("download.setup.option.versionInfo", "Version information");
        t.put("download.setup.option.forge", "Select Forge version");
        t.put("download.setup.option.optifine", "Select OptiFine version");
        t.put("download.setup.option.fabric", "Select Fabric version");
        t.put("download.setup.option.fabricApi", "Select Fabric API version");
        t.put("download.setup.option.neoforge", "Select NeoForge version");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge and OptiFine are incompatible; switched to Forge %s");
        t.put("download.setup.optifineIncompatibleForge", "The current Forge is incompatible with OptiFine");
        t.put("download.setup.noCompatibleOptiFine", "No OptiFine version is compatible with the current Forge");
        t.put("download.setup.optifineForgeConflict", "Forge %s is not compatible with OptiFine %s. Choose Forge %s or earlier.");
        t.put("download.setup.nameEmpty", "Instance name cannot be empty");
        t.put("download.setup.nameDotSegments", "Instance name cannot be . or ..");
        t.put("download.setup.nameTrailing", "Instance name cannot end with a dot or space");
        t.put("download.setup.nameIllegalCharacter", "Instance name contains an illegal character: %s");
        t.put("download.setup.nameExists", "Instance name already exists: %s. Choose another name.");
        t.put("download.setup.noGameDirectory", "Game directory is not set");
        t.put("download.setup.checkingName", "Checking instance name...");
        t.put("download.setup.nameSameAsLoader", "When installing a loader, the instance name cannot equal the game version");
        t.put("download.setup.nameAvailable", "Instance name is available");
        t.put("download.setup.checkNameFailed", "Failed to check instance name: %s");
        t.put("download.setup.nameNotChecked", "The instance name has not passed the check yet; try again shortly.");
        t.put("download.setup.noGameDirectoryLong", "Game directory is not set; save it in settings first.");
        t.put("download.setup.optifineRequiresForge", "OptiFine can only be installed with Forge.");

        // Loader prompts
        t.put("download.prompt.loading.fabric", "Loading Fabric versions...");
        t.put("download.prompt.loading.forge", "Loading Forge versions...");
        t.put("download.prompt.loading.neoforge", "Loading NeoForge versions...");
        t.put("download.prompt.fabricFirst", "Select a Fabric version first");
        t.put("download.prompt.forgeFirst", "Select a Forge version first");
        t.put("download.prompt.fabricApiFirst", "Select a Fabric version first");
        t.put("download.prompt.select.fabric", "Select a Fabric version");
        t.put("download.prompt.select.forge", "Select a Forge version");
        t.put("download.prompt.select.neoforge", "Select a NeoForge version");
        t.put("download.prompt.select.optifine", "Select an OptiFine version");
        t.put("download.prompt.select.fabricApi", "Select a Fabric API version");
        t.put("download.prompt.empty.fabric", "No Fabric versions available");
        t.put("download.prompt.empty.fabricApi", "No Fabric API versions available");
        t.put("download.prompt.empty.forge", "No Forge versions available");
        t.put("download.prompt.empty.neoforge", "No NeoForge versions available");
        t.put("download.prompt.empty.optifine", "No OptiFine versions available");
        t.put("download.prompt.error.fabric", "Failed to load Fabric versions");
        t.put("download.prompt.error.fabricApi", "Failed to load Fabric API versions");
        t.put("download.prompt.error.forge", "Failed to load Forge versions");
        t.put("download.prompt.error.neoforge", "Failed to load NeoForge versions");
        t.put("download.prompt.error.optifine", "Failed to load OptiFine versions");

        // Download progress page
        t.put("download.progress.title", "Downloading %s");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "Back to downloads");
        t.put("download.progress.speed", "Network speed");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "Download progress");
        t.put("download.progress.preparing", "Preparing download");
        t.put("download.progress.waiting", "Waiting for download task...");
        t.put("download.progress.completed", "Download complete");
        t.put("download.progress.completedTotal", "Download complete");
        t.put("download.progress.total", "Total %s");
        t.put("download.progress.totalWithMax", "Total %s / %s");
        t.put("download.progress.failed", "Download failed: %s");
        t.put("download.progress.currentFile", "Current file: %s");
        t.put("download.progress.currentFileWithBytes", "Current file: %s · %s / %s");
        t.put("download.progress.finishedHint", "The instance is installed; you can return to the list.");
        t.put("download.progress.failedHint", "Download stopped; return to the list and try again.");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · files %d / %d");
        t.put("download.progress.filesDetail", "Files %d / %d");
        t.put("download.progress.planning", "Preparing download plan...");

        // Download stages
        t.put("download.stage.preparing", "Preparing download");
        t.put("download.stage.completed", "Complete");
        t.put("download.stage.failed", "Download failed");
        t.put("download.stage.unknownError", "Unknown error");
        t.put("download.stage.version.fetch", "Fetching version metadata");
        t.put("download.stage.version.checkLocal", "Checking local version");
        t.put("download.stage.version.downloadFiles", "Downloading game files and libraries");
        t.put("download.stage.version.downloadAssets", "Downloading assets");
        t.put("download.stage.version.writeJson", "Writing version metadata");
        t.put("download.stage.loader.installer.prepare", "Preparing %s installer");
        t.put("download.stage.loader.installer.download", "Downloading %s installer");
        t.put("download.stage.loader.installer.run", "Running %s installer");
        t.put("download.stage.loader.libraries.download", "Downloading %s libraries");
        t.put("download.stage.loader.files.merge", "Merging %s files");
        t.put("download.stage.loader.verify", "Verifying %s installation");
        t.put("download.stage.fabric.config.fetch", "Fetching Fabric configuration");
        t.put("download.stage.fabric.loader.install", "Installing Fabric Loader");
        t.put("download.stage.fabric.api.download", "Downloading Fabric API");
        t.put("download.stage.fabric.verify", "Verifying Fabric installation");
        t.put("download.stage.optifine.installer.download", "Downloading OptiFine installer");
        t.put("download.stage.optifine.installer.run", "Running OptiFine installer");
        t.put("download.stage.optifine.files.merge", "Merging OptiFine files");

        // Download settings
        t.put("download.settings.source", "Download source");
        t.put("download.settings.source.description", "Choose the preferred download address for versions, libraries, and assets.");
        t.put("download.settings.source.mirror", "Prefer mirror source");
        t.put("download.settings.source.official", "Prefer official source");
        t.put("download.settings.source.custom", "Import custom API");
        t.put("download.settings.customApi.placeholder", "e.g. https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "Download concurrency");
        t.put("download.settings.concurrency.description", "Number of files downloaded simultaneously, range 1–256.");
        t.put("download.settings.concurrency.label", "Download concurrency");
        t.put("download.settings.concurrency.value", "Current concurrency: %d");
        t.put("download.settings.concurrency.accessible", "Download concurrency");

        // Settings navigation
        t.put("settings.section.game", "Game settings");
        t.put("settings.section.download", "Download settings");
        t.put("settings.section.appearance", "Appearance");

        // Game directory
        t.put("settings.game.location.title", "Game directory");
        t.put("settings.game.location.description", "Set the Minecraft game data directory. Changes apply only after saving.");
        t.put("settings.game.location.browse", "Browse");
        t.put("settings.game.location.save", "Save");
        t.put("settings.game.location.chooserTitle", "Choose game directory");
        t.put("settings.game.location.notSaved", "Not saved");
        t.put("settings.game.location.empty", "Game directory cannot be empty");
        t.put("settings.game.location.invalid", "Invalid game directory format");
        t.put("settings.game.location.checking", "Checking directory...");
        t.put("settings.game.location.missing", "Directory does not exist: %s");
        t.put("settings.game.location.saved", "Game directory saved");
        t.put("settings.game.location.checkFailed", "Failed to check directory: %s");

        // Memory
        t.put("settings.game.memory.title", "Memory");
        t.put("settings.game.memory.accessible", "Maximum memory");
        t.put("settings.game.memory.description", "Adjust the memory used when launching the game.");
        t.put("settings.game.memory.label", "Memory allocation");
        t.put("settings.game.memory.hint", "Drag the handle to adjust the game allocation; the gray bar shows used + game memory, and the light gray bar on the right is unused memory.");
        t.put("settings.game.memory.summary", "Used %d MB / Game %d MB / Available %d MB / Total %d MB");

        // Version isolation
        t.put("settings.game.isolation.title", "Version isolation");
        t.put("settings.game.isolation.full", "Full version isolation");
        t.put("settings.game.isolation.none", "No version isolation");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "Choose the Java runtime used to launch the game. Automatic mode picks a Java that meets the game version requirements.");
        t.put("settings.game.java.auto", "Automatic");
        t.put("settings.game.java.manual", "Manual");
        t.put("settings.game.java.rescan", "Rescan");
        t.put("settings.game.java.add", "Add Java");
        t.put("settings.game.java.scanning", "Scanning for Java...");
        t.put("settings.game.java.scanningAll", "Scanning the system for Java...");
        t.put("settings.game.java.scanFailedPrompt", "Failed to scan for Java");
        t.put("settings.game.java.scanFailed", "Failed to scan for Java: %s");
        t.put("settings.game.java.noneFound", "No Java found");
        t.put("settings.game.java.select", "Select a Java");
        t.put("settings.game.java.autoSelected", "Automatic");
        t.put("settings.game.java.chooserTitle", "Choose Java executable");
        t.put("settings.game.java.checking", "Checking Java...");
        t.put("settings.game.java.addFailed", "Failed to add Java: %s");
        t.put("settings.game.java.autoHint", "Automatic mode selects a Java from the scan results that meets the current game version requirements.");
        t.put("settings.game.java.selectOrAdd", "Select a Java, or click \"Add Java\".");
        t.put("settings.game.java.unavailable", "This Java is currently unavailable: %s");
        t.put("settings.game.java.detail", "Version %s · %s · %s · %d-bit%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // Appearance
        t.put("settings.appearance.title", "Appearance");
        t.put("settings.appearance.description", "Adjust the launcher's appearance.");
        t.put("settings.appearance.themeHint", "Themes come from ~/.caml/themes; changes apply immediately");
        t.put("settings.appearance.theme", "Theme");
        t.put("settings.appearance.theme.accessible", "Theme");
        t.put("settings.appearance.language.title", "Language");
        t.put("settings.appearance.language.description", "Choose how the launcher interface language is selected.");
        t.put("settings.appearance.language.auto", "Automatic");
        t.put("settings.appearance.language.manual", "Manual");
        t.put("settings.appearance.language.label", "Interface language");
        t.put("settings.appearance.language.hint", "Automatic follows the system language; manual uses the language selected below.");
        t.put("settings.appearance.language.accessible", "Interface language");

        // Instance picker
        t.put("instances.picker.scanning", "Scanning game instances...");
        t.put("instances.picker.title", "Select game instance");
        t.put("instances.picker.noGameDirectory", "Game directory is not set");
        t.put("instances.picker.gameDirectory", "Game directory: %s");
        t.put("instances.picker.noGameDirectoryHint", "Game directory is not set; save it in settings first");
        t.put("instances.picker.missing", "Game directory does not exist: %s");
        t.put("instances.picker.empty", "No installed instances in the current game directory");
        t.put("instances.picker.count", "%d game instances");
        t.put("instances.picker.scanFailed", "Failed to scan game instances: %s");
        t.put("instances.manage.title", "Manage instances");

        // Background operation names
        t.put("operation.inspectGameDirectory", "Inspect game directory");
        t.put("operation.checkInstanceName", "Check instance name");
        t.put("operation.fetchVersionList", "Fetch official %s list");
        t.put("operation.installVanilla", "Install vanilla instance %s");
        t.put("operation.fetchFabricLoader", "Fetch Fabric Loader versions");
        t.put("operation.fetchFabricApi", "Fetch Fabric API versions");
        t.put("operation.installFabric", "Install Fabric instance %s");
        t.put("operation.fetchForge", "Fetch Forge versions");
        t.put("operation.fetchNeoForge", "Fetch NeoForge versions");
        t.put("operation.fetchOptiFine", "Fetch OptiFine versions");
        t.put("operation.installLoader", "Install %s instance %s");
        t.put("operation.scanJava", "Scan Java");
        t.put("operation.addManualJava", "Add manual Java");
        t.put("operation.saveJavaSelection", "Save Java selection");
        t.put("operation.saveMaxMemory", "Save max memory setting");
        t.put("operation.saveGameDirectory", "Save game directory");
        t.put("operation.saveVersionIsolation", "Save version isolation setting");
        t.put("operation.saveDownloadConcurrency", "Save download concurrency");
        t.put("operation.saveDownloadSource", "Save download source setting");
        t.put("operation.saveTheme", "Save theme selection");
        t.put("operation.saveLanguage", "Save language setting");
        t.put("operation.saveLastInstance", "Save last launched instance");
        t.put("operation.launchGame", "Launch game %s");
    }
}