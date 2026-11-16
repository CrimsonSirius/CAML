package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.Library;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.mod.model.ModLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 从实例版本 JSON 和 inheritsFrom 链解析 MC 版本与加载器。 */
public final class InstanceEnvironmentResolver {
    private InstanceEnvironmentResolver() {
    }

    public static InstanceEnvironment resolve(Path gameDirectory, String instanceId,
                                              MetadataService metadata) throws IOException {
        gameDirectory = gameDirectory.toAbsolutePath().normalize();
        Path versions = gameDirectory.resolve("versions");
        Path versionJson = versions.resolve(instanceId).resolve(instanceId + ".json");
        if (!Files.isRegularFile(versionJson)) {
            return new InstanceEnvironment(instanceId, null);
        }

        VersionInfo current = metadata.readVersionInfo(versionJson);
        String gameVersion = current.id();
        ModLoader loader = detectLoader(current);
        Set<String> visited = new HashSet<>();
        while (current.inheritsFrom() != null) {
            String parentId = current.inheritsFrom();
            if (!visited.add(parentId)) {
                break;
            }
            Path parentJson = versions.resolve(parentId).resolve(parentId + ".json");
            if (!Files.isRegularFile(parentJson)) {
                gameVersion = parentId;
                break;
            }
            current = metadata.readVersionInfo(parentJson);
            gameVersion = current.id();
            if (loader == null) {
                loader = detectLoader(current);
            }
        }
        return new InstanceEnvironment(gameVersion, loader);
    }

    private static ModLoader detectLoader(VersionInfo version) {
        String libraries = version.libraries().stream()
                .map(Library::name)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("\n"));
        String mainClass = version.mainClass() == null
                ? ""
                : version.mainClass().toLowerCase(Locale.ROOT);

        if (libraries.contains("fabric-loader") || mainClass.contains("fabricmc")) {
            return ModLoader.FABRIC;
        }
        if (libraries.contains("quilt-loader") || mainClass.contains("quiltmc")) {
            return ModLoader.QUILT;
        }
        if (libraries.contains("neoforge") || mainClass.contains("neoforge")) {
            return ModLoader.NEOFORGE;
        }
        if (libraries.contains("minecraftforge") || mainClass.contains("minecraftforge")) {
            return ModLoader.FORGE;
        }
        return null;
    }
}
