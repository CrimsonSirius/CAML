package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.instance.GameDirectoryInfo;
import org.chs.camlauncher.backend.instance.InstanceDetails;
import org.chs.camlauncher.backend.instance.InstanceEnvironment;
import org.chs.camlauncher.backend.instance.InstanceEnvironmentResolver;
import org.chs.camlauncher.backend.instance.InstanceInspector;
import org.chs.camlauncher.backend.instance.InstanceMod;
import org.chs.camlauncher.backend.instance.InstanceModIconService;
import org.chs.camlauncher.backend.instance.InstanceModUpdate;
import org.chs.camlauncher.backend.instance.InstanceNameCheck;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.service.ModService;
import org.chs.camlauncher.backend.mod.source.CurseForgeFingerprint;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.backend.system.DirectoryOpener;
import org.chs.camlauncher.backend.download.DownloadListener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/** 游戏目录探测、实例内容和 Mod/资源包管理入口。 */
public final class BackendInstances {
    private static final Logger LOGGER = LogManager.getLogger(BackendInstances.class);

    private final SettingsService settingsService;
    private final MetadataService metadata;
    private final ModService modService;

    BackendInstances(SettingsService settingsService, MetadataService metadata, ModService modService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.modService = Objects.requireNonNull(modService, "modService");
    }

    public InstanceNameCheck checkInstanceName(Path gameDirectory, String instanceId) {
        return InstanceNameRules.check(gameDirectory, instanceId);
    }

    public GameDirectoryInfo inspectGameDirectory(Path directory) {
        Objects.requireNonNull(directory, "directory");
        Path normalized = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            return new GameDirectoryInfo(normalized, false, List.of());
        }

        Path versionsDirectory = normalized.resolve("versions");
        if (!Files.isDirectory(versionsDirectory)) {
            return new GameDirectoryInfo(normalized, true, List.of());
        }

        List<String> versions;
        try (Stream<Path> entries = Files.list(versionsDirectory)) {
            List<Path> installed = entries
                    .filter(Files::isDirectory)
                    .filter(entry -> Files.isRegularFile(
                            entry.resolve(entry.getFileName().toString() + ".json")))
                    .toList();
            versions = installed.stream()
                    .map(entry -> entry.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to inspect game directory: " + normalized, e);
        }
        return new GameDirectoryInfo(normalized, true, versions);
    }

    public InstanceDetails inspectInstance(Path gameDirectory, String instanceId) throws IOException {
        return inspectInstance(gameDirectory, instanceId,
                settingsService.versionIsolationMode());
    }

    private InstanceDetails inspectInstance(Path gameDirectory, String instanceId,
                                            VersionIsolationMode isolationMode) throws IOException {
        String instance = InstanceNameRules.validate(instanceId);
        Set<String> disabled = settingsService.disabledMods(gameDirectory, instance);
        return InstanceInspector.inspect(gameDirectory, instance, isolationMode, disabled);
    }

    public void openInstanceFolder(Path gameDirectory, String instanceId) throws IOException {
        DirectoryOpener.open(InstanceInspector.dataDirectory(
                gameDirectory, instanceId, settingsService.versionIsolationMode()));
    }

    public void setModDisabled(Path gameDirectory, String instanceId, String fileName,
                               boolean disabled) throws IOException {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        String instance = InstanceNameRules.validate(instanceId);
        VersionIsolationMode isolationMode = settingsService.versionIsolationMode();
        String baseName = normalizeModFileName(fileName);
        Path modsDirectory = InstanceInspector.dataDirectory(
                normalizedGameDirectory, instance, isolationMode)
                .resolve("mods");
        Path enabledPath = SafePaths.resolveChild(modsDirectory, baseName, "mod file");
        Path disabledPath = SafePaths.resolveChild(modsDirectory, baseName + ".disabled",
                "disabled mod file");

        boolean moved = false;
        Path source = disabled ? enabledPath : disabledPath;
        Path target = disabled ? disabledPath : enabledPath;
        if (Files.isRegularFile(source)) {
            if (Files.exists(target)) {
                throw new IOException("目标 Mod 文件已存在: " + target);
            }
            move(source, target);
            moved = true;
        } else if (!Files.isRegularFile(target)) {
            throw new IOException("Mod 文件不存在: " + source);
        }

        try {
            settingsService.setModDisabled(normalizedGameDirectory, instance, baseName, disabled);
        } catch (IOException | RuntimeException e) {
            if (moved) {
                try {
                    move(target, source);
                } catch (IOException rollbackFailure) {
                    LOGGER.warn("回滚 Mod 文件状态失败: {}", target, rollbackFailure);
                }
            }
            throw e;
        }
    }

    public void setResourcePackDisabled(Path gameDirectory, String instanceId, String fileName,
                                        boolean disabled) throws IOException {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        String instance = InstanceNameRules.validate(instanceId);
        VersionIsolationMode isolationMode = settingsService.versionIsolationMode();
        String baseName = normalizeResourcePackFileName(fileName);
        Path resourcePacksDirectory = InstanceInspector.dataDirectory(
                normalizedGameDirectory, instance, isolationMode).resolve("resourcepacks");
        Path enabledPath = SafePaths.resolveChild(
                resourcePacksDirectory, baseName, "resource pack");
        Path disabledPath = SafePaths.resolveChild(
                resourcePacksDirectory, baseName + ".disabled", "disabled resource pack");

        Path source = disabled ? enabledPath : disabledPath;
        Path target = disabled ? disabledPath : enabledPath;
        if (!Files.exists(source)) {
            if (Files.exists(target)) {
                return;
            }
            throw new IOException("资源包不存在: " + source);
        }
        if (Files.exists(target)) {
            throw new IOException("目标资源包已存在: " + target);
        }
        move(source, target);
    }

    public byte[] loadInstanceModIcon(Path gameDirectory, String instanceId, String fileName)
            throws IOException {
        return InstanceModIconService.load(gameDirectory, instanceId, fileName,
                settingsService.versionIsolationMode());
    }

    public List<InstanceModUpdate> checkInstanceModUpdates(
            Path gameDirectory, String instanceId) throws IOException, InterruptedException {
        VersionIsolationMode isolationMode = settingsService.versionIsolationMode();
        InstanceDetails details = inspectInstance(gameDirectory, instanceId, isolationMode);
        InstanceEnvironment environment = InstanceEnvironmentResolver.resolve(
                gameDirectory, instanceId, metadata);
        if (environment.loader() == null
                || environment.gameVersion() == null || environment.gameVersion().isBlank()) {
            return details.mods().stream()
                    .map(mod -> new InstanceModUpdate(
                            mod.fileName(), displayVersion(mod.version()),
                            displayVersion(mod.version()), false, null, "unsupported"))
                    .toList();
        }

        Set<String> hashes = new LinkedHashSet<>();
        for (InstanceMod mod : details.mods()) {
            Path file = InstanceInspector.modFile(
                    gameDirectory, instanceId, mod.fileName(), isolationMode);
            if (Files.isRegularFile(file)) {
                hashes.add(HashVerifier.sha1(file));
            }
        }
        Map<String, ModFile> latestByHash = modService.latestVersions(
                new ArrayList<>(hashes),
                environment.gameVersion(),
                environment.loader());

        Map<Long, ModFile> latestByFingerprint = Map.of();
        if (modService.curseForgeConfigured()) {
            Map<Long, String> fingerprintToFile = new LinkedHashMap<>();
            for (InstanceMod mod : details.mods()) {
                String hash = null;
                Path file = InstanceInspector.modFile(
                        gameDirectory, instanceId, mod.fileName(), isolationMode);
                if (Files.isRegularFile(file)) {
                    hash = HashVerifier.sha1(file);
                }
                if (hash != null && latestByHash.containsKey(hash)) {
                    continue;
                }
                if (Files.isRegularFile(file)) {
                    fingerprintToFile.put(CurseForgeFingerprint.of(file), mod.fileName());
                }
            }
            if (!fingerprintToFile.isEmpty()) {
                try {
                    Map<Long, String> fingerprintToModId = modService.curseForgeFingerprintModIds(
                            new ArrayList<>(fingerprintToFile.keySet()));
                    latestByFingerprint = modService.latestCurseForgeVersions(
                            fingerprintToModId, environment.gameVersion(), environment.loader());
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("CurseForge 更新检查失败", e);
                }
            }
        }

        List<InstanceModUpdate> updates = new ArrayList<>();
        for (InstanceMod mod : details.mods()) {
            String currentVersion = displayVersion(mod.version());
            Path file = InstanceInspector.modFile(
                    gameDirectory, instanceId, mod.fileName(), isolationMode);
            String hash = Files.isRegularFile(file) ? HashVerifier.sha1(file) : "";
            ModFile latest = latestByHash.get(hash);
            if (latest == null && Files.isRegularFile(file)) {
                latest = latestByFingerprint.get(CurseForgeFingerprint.of(file));
            }
            boolean updateAvailable = latest != null && !sameVersion(
                    mod.version(), latest.versionNumber(), latest.sha1(), hash);
            String latestVersion = latest == null
                    ? currentVersion
                    : displayVersion(latest.versionNumber());
            updates.add(new InstanceModUpdate(
                    mod.fileName(), currentVersion, latestVersion,
                    updateAvailable, latest, null));
        }
        return List.copyOf(updates);
    }

    public ModDownloadResult updateInstanceMod(
            Path gameDirectory, String instanceId, String currentFileName,
            ModFile latestFile, DownloadListener listener)
            throws IOException, InterruptedException {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        String instance = InstanceNameRules.validate(instanceId);
        VersionIsolationMode isolationMode = settingsService.versionIsolationMode();
        String oldBase = normalizeModFileName(currentFileName);
        String newBase = normalizeModFileName(latestFile.fileName());
        Path modsDirectory = InstanceInspector.dataDirectory(
                normalizedGameDirectory, instance, isolationMode).resolve("mods");
        Path oldEnabled = SafePaths.resolveChild(modsDirectory, oldBase, "mod file");
        Path oldDisabled = SafePaths.resolveChild(modsDirectory, oldBase + ".disabled",
                "disabled mod file");
        boolean wasDisabled = settingsService.disabledMods(normalizedGameDirectory, instance)
                .contains(oldBase) || Files.isRegularFile(oldDisabled);

        ModDownloadResult downloaded = modService.download(
                latestFile, normalizedGameDirectory, instance, isolationMode, listener);

        if (!oldBase.equals(newBase)) {
            Files.deleteIfExists(oldEnabled);
            Files.deleteIfExists(oldDisabled);
        }
        Path newEnabled = SafePaths.resolveChild(modsDirectory, newBase, "mod file");
        Path newDisabled = SafePaths.resolveChild(modsDirectory, newBase + ".disabled",
                "disabled mod file");
        Path resultPath = downloaded.path();
        if (wasDisabled && Files.isRegularFile(newEnabled)) {
            Files.move(newEnabled, newDisabled, StandardCopyOption.REPLACE_EXISTING);
            resultPath = newDisabled;
        }

        settingsService.setModDisabled(normalizedGameDirectory, instance, newBase, wasDisabled);
        if (!oldBase.equals(newBase)) {
            settingsService.setModDisabled(normalizedGameDirectory, instance, oldBase, false);
        }
        return new ModDownloadResult(resultPath, latestFile);
    }

    public Path modsDirectory(Path gameDirectory, String instanceId,
                              VersionIsolationMode isolationMode) {
        return modService.modsDirectory(gameDirectory, instanceId, isolationMode);
    }

    private static String displayVersion(String version) {
        return version == null || version.isBlank() ? "?" : version;
    }

    private static boolean sameVersion(String current, String latest, String latestSha1, String hash) {
        if (latestSha1 != null && !latestSha1.isBlank()
                && latestSha1.equalsIgnoreCase(hash)) {
            return true;
        }
        if (current == null || current.isBlank()) {
            return false;
        }
        return latest != null && current.equalsIgnoreCase(latest);
    }

    private static String normalizeModFileName(String fileName) {
        String value = Objects.requireNonNull(fileName, "fileName").trim();
        if (value.endsWith(".disabled")) {
            value = value.substring(0, value.length() - ".disabled".length());
        }
        if (!value.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
            throw new IllegalArgumentException("不是有效的 Mod jar: " + fileName);
        }
        return value;
    }

    private static String normalizeResourcePackFileName(String fileName) {
        String value = Objects.requireNonNull(fileName, "fileName").trim();
        if (value.endsWith(".disabled")) {
            value = value.substring(0, value.length() - ".disabled".length());
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("资源包文件名不能为空");
        }
        return value;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }
}
