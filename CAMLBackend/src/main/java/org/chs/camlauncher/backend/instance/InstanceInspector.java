package org.chs.camlauncher.backend.instance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.InstanceDetails;
import org.chs.camlauncher.backend.api.InstanceMod;
import org.chs.camlauncher.backend.api.InstanceResourcePack;
import org.chs.camlauncher.backend.io.SafePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/** 读取实例实际数据目录、mods 和资源包文件列表。 */
public final class InstanceInspector {
    private static final Logger LOGGER = LogManager.getLogger(InstanceInspector.class);
    private static final String DISABLED_SUFFIX = ".jar.disabled";
    private static final String RESOURCE_PACK_DISABLED_SUFFIX = ".disabled";

    private InstanceInspector() {
    }

    public static InstanceDetails inspect(Path gameDirectory, String instanceId) throws IOException {
        return inspect(gameDirectory, instanceId, Set.of());
    }

    public static InstanceDetails inspect(Path gameDirectory, String instanceId,
                                          Set<String> configuredDisabled) throws IOException {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        Objects.requireNonNull(configuredDisabled, "configuredDisabled");
        String instance = InstanceNameRules.validate(instanceId);
        Path versionDirectory = SafePaths.resolveChild(
                normalizedGameDirectory.resolve("versions"), instance, "instance id");
        Path dataDirectory = GameDataLayout.resolve(
                normalizedGameDirectory, versionDirectory, VersionIsolationMode.AUTO);
        Path modsDirectory = dataDirectory.resolve("mods");
        Path resourcePacksDirectory = dataDirectory.resolve("resourcepacks");

        return new InstanceDetails(
                instance,
                versionDirectory,
                dataDirectory,
                modsDirectory,
                scanMods(modsDirectory, configuredDisabled),
                resourcePacksDirectory,
                scanResourcePacks(resourcePacksDirectory)
        );
    }

    /** 解析打开实例文件夹时需要使用的数据目录。 */
    public static Path dataDirectory(Path gameDirectory, String instanceId) {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        String instance = InstanceNameRules.validate(instanceId);
        Path versionDirectory = SafePaths.resolveChild(
                normalizedGameDirectory.resolve("versions"), instance, "instance id");
        return GameDataLayout.resolve(
                normalizedGameDirectory, versionDirectory, VersionIsolationMode.AUTO);
    }

    /** 解析本地 Mod 的实际文件路径，兼容 .jar 和 .jar.disabled。 */
    public static Path modFile(Path gameDirectory, String instanceId, String fileName) {
        String baseName = fileName != null && fileName.endsWith(".disabled")
                ? fileName.substring(0, fileName.length() - ".disabled".length())
                : fileName;
        Path modsDirectory = dataDirectory(gameDirectory, instanceId).resolve("mods");
        Path enabled = SafePaths.resolveChild(modsDirectory, baseName, "mod file");
        if (Files.isRegularFile(enabled)) {
            return enabled;
        }
        return SafePaths.resolveChild(modsDirectory, baseName + ".disabled",
                "disabled mod file");
    }

    private static List<InstanceMod> scanMods(Path modsDirectory,
                                              Set<String> configuredDisabled) throws IOException {
        if (!Files.isDirectory(modsDirectory)) {
            return List.of();
        }

        List<InstanceMod> mods = new ArrayList<>();
        try (Stream<Path> entries = Files.list(modsDirectory)) {
            entries.filter(Files::isRegularFile).forEach(path -> {
                String fileName = path.getFileName().toString();
                String lowerName = fileName.toLowerCase(Locale.ROOT);
                boolean suffixDisabled = lowerName.endsWith(DISABLED_SUFFIX);
                if (!lowerName.endsWith(".jar") && !suffixDisabled) {
                    return;
                }
                String baseName = suffixDisabled
                        ? fileName.substring(0, fileName.length() - ".disabled".length())
                        : fileName;
                boolean disabled = suffixDisabled || configuredDisabled.contains(baseName);
                try {
                    mods.add(new InstanceMod(baseName,
                            InstanceModMetadataService.version(path), Files.size(path), !disabled));
                } catch (IOException e) {
                    LOGGER.warn("无法读取 Mod 文件大小: {}", path, e);
                }
            });
        }
        mods.sort(Comparator.comparing(InstanceMod::fileName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(mods);
    }

    private static List<InstanceResourcePack> scanResourcePacks(Path resourcePacksDirectory)
            throws IOException {
        if (!Files.isDirectory(resourcePacksDirectory)) {
            return List.of();
        }

        List<InstanceResourcePack> resourcePacks = new ArrayList<>();
        try (Stream<Path> entries = Files.list(resourcePacksDirectory)) {
            entries.forEach(path -> {
                String fileName = path.getFileName().toString();
                String lowerName = fileName.toLowerCase(Locale.ROOT);
                boolean suffixDisabled = lowerName.endsWith(RESOURCE_PACK_DISABLED_SUFFIX);
                String baseName = suffixDisabled
                        ? fileName.substring(0,
                                fileName.length() - RESOURCE_PACK_DISABLED_SUFFIX.length())
                        : fileName;
                boolean directory = Files.isDirectory(path);
                if (directory) {
                    if (!Files.isRegularFile(path.resolve("pack.mcmeta"))) {
                        return;
                    }
                } else if (!lowerName.endsWith(".zip")
                        && !lowerName.endsWith(".zip" + RESOURCE_PACK_DISABLED_SUFFIX)) {
                    return;
                }
                try {
                    resourcePacks.add(new InstanceResourcePack(
                            baseName,
                            InstanceResourcePackMetadataService.version(path),
                            Files.size(path),
                            !suffixDisabled));
                } catch (IOException e) {
                    LOGGER.warn("无法读取资源包文件大小: {}", path, e);
                }
            });
        }
        resourcePacks.sort(Comparator.comparing(
                InstanceResourcePack::fileName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(resourcePacks);
    }
}
