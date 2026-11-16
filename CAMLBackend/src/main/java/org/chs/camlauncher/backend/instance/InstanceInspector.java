package org.chs.camlauncher.backend.instance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.InstanceDetails;
import org.chs.camlauncher.backend.api.InstanceMod;
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

/** 读取实例实际数据目录和 mods 文件列表。 */
public final class InstanceInspector {
    private static final Logger LOGGER = LogManager.getLogger(InstanceInspector.class);
    private static final String DISABLED_SUFFIX = ".jar.disabled";

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

        return new InstanceDetails(
                instance,
                versionDirectory,
                dataDirectory,
                modsDirectory,
                scanMods(modsDirectory, configuredDisabled)
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
                    mods.add(new InstanceMod(baseName, Files.size(path), !disabled));
                } catch (IOException e) {
                    LOGGER.warn("无法读取 Mod 文件大小: {}", path, e);
                }
            });
        }
        mods.sort(Comparator.comparing(InstanceMod::fileName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(mods);
    }
}
