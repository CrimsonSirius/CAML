package org.chs.camlauncher.backend.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ManifestResolver {
    public VersionInfo resolve(VersionInfo manifest, Map<String, VersionInfo> versions) {
        return resolve(manifest, versions, new LinkedHashSet<>());
    }

    private VersionInfo resolve(VersionInfo manifest, Map<String, VersionInfo> versions, Set<String> visiting) {
        if (manifest.inheritsFrom() == null) {
            return manifest;
        }
        if (!visiting.add(manifest.id())) {
            throw new IllegalArgumentException("Circular inheritance: " + visiting);
        }

        VersionInfo parent = versions.get(manifest.inheritsFrom());
        if (parent == null) {
            throw new IllegalArgumentException("Missing parent version: " + manifest.inheritsFrom());
        }

        VersionInfo result = merge(resolve(parent, versions, visiting), manifest);
        visiting.remove(manifest.id());
        return result;
    }

    public VersionInfo merge(VersionInfo parent, VersionInfo child) {
        return new VersionInfo(
                child.id(),
                child.type() != null ? child.type() : parent.type(),
                null,
                child.mainClass() != null ? child.mainClass() : parent.mainClass(),
                child.assets() != null ? child.assets() : parent.assets(),
                child.assetIndex() != null ? child.assetIndex() : parent.assetIndex(),
                child.minecraftArguments() != null ? child.minecraftArguments() : parent.minecraftArguments(),
                Arguments.merge(parent.arguments(), child.arguments()),
                mergeMaps(parent.downloads(), child.downloads()),
                child.javaVersion() != null ? child.javaVersion() : parent.javaVersion(),
                concat(child.libraries(), parent.libraries()),
                mergeMaps(parent.logging(), child.logging()),
                child.minimumLauncherVersion() != null ? child.minimumLauncherVersion() : parent.minimumLauncherVersion(),
                child.time() != null ? child.time() : parent.time(),
                child.releaseTime() != null ? child.releaseTime() : parent.releaseTime(),
                child.complianceLevel() != null ? child.complianceLevel() : parent.complianceLevel(),
                child.jar() != null && !child.jar().isBlank()
                        ? child.jar()
                        : parent.jar() != null && !parent.jar().isBlank()
                                ? parent.jar()
                                : parent.id()
        );
    }

    private static <K, V> Map<K, V> mergeMaps(Map<K, V> parent, Map<K, V> child) {
        Map<K, V> result = new LinkedHashMap<>();
        if (parent != null) {
            result.putAll(parent);
        }
        if (child != null) {
            result.putAll(child);
        }
        return Map.copyOf(result);
    }

    private static <T> List<T> concat(List<T> parent, List<T> child) {
        List<T> result = new ArrayList<>(parent.size() + child.size());
        result.addAll(parent);
        result.addAll(child);
        return List.copyOf(result);
    }
}
