package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.Library;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Linux 启动环境预检：识别需要外部 xrandr 的 LWJGL 2 实例。 */
public final class LaunchCompatibilityService {
    private static final String XRANDR = "xrandr";

    private final MetadataService metadata;
    private final String osName;
    private final Predicate<String> commandAvailable;

    public LaunchCompatibilityService(MetadataService metadata) {
        this(
                metadata,
                RuleEvaluator.currentOsName(),
                command -> commandAvailable(
                        command,
                        System.getenv("PATH"),
                        Path.of(System.getProperty("user.home"), ".local", "bin"))
        );
    }

    LaunchCompatibilityService(MetadataService metadata, String osName,
                               Predicate<String> commandAvailable) {
        this.metadata = metadata;
        this.osName = osName;
        this.commandAvailable = commandAvailable;
    }

    /** 只在 Linux、实例使用 LWJGL 2 且找不到 xrandr 时返回 true。 */
    public boolean needsXrandrWarning(Path gameDirectory, String instanceId) throws IOException {
        if (!"linux".equals(osName) || commandAvailable.test(XRANDR)) {
            return false;
        }
        return usesLwjgl2(gameDirectory, instanceId);
    }

    private boolean usesLwjgl2(Path gameDirectory, String instanceId) throws IOException {
        Path versions = gameDirectory.toAbsolutePath().normalize().resolve("versions");
        String currentId = instanceId;
        Set<String> visited = new HashSet<>();
        while (currentId != null && !currentId.isBlank() && visited.add(currentId)) {
            Path versionDirectory = SafePaths.resolveChild(versions, currentId, "instance id");
            Path versionJson = SafePaths.resolveChild(
                    versionDirectory, currentId + ".json", "version json");
            if (!Files.isRegularFile(versionJson)) {
                return false;
            }
            VersionInfo version = metadata.readVersionInfo(versionJson);
            if (version.libraries().stream().anyMatch(LaunchCompatibilityService::isLwjgl2)) {
                return true;
            }
            currentId = version.inheritsFrom();
        }
        return false;
    }

    static boolean isLwjgl2(Library library) {
        String name = library.name();
        if (name == null) {
            return false;
        }
        String[] parts = name.split(":");
        return parts.length >= 3
                && ("org.lwjgl.lwjgl".equals(parts[0]) || "org.lwjgl".equals(parts[0]))
                && "lwjgl".equals(parts[1])
                && parts[2].startsWith("2.");
    }

    static boolean commandAvailable(String command, String path, Path userLocalBin) {
        if (userLocalBin != null && isExecutable(userLocalBin.resolve(command))) {
            return true;
        }
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String entry : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!entry.isBlank() && isExecutable(Path.of(entry).resolve(command))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExecutable(Path path) {
        return Files.isRegularFile(path) && Files.isExecutable(path);
    }
}
