package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.metadata.model.VersionInfo;

import java.nio.file.Path;

public record InstalledVersion(
        String id,
        VersionInfo manifest,
        Path gameDirectory,
        Path versionDirectory,
        Path versionJson,
        Path clientJar,
        Path librariesDirectory,
        Path assetsDirectory,
        Path assetIndexFile
) {
    public InstalledVersion relocateVersionDirectory(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        Path relocatedClientJar = clientJar;
        if (clientJar != null && versionDirectory != null) {
            Path previousDirectory = versionDirectory.toAbsolutePath().normalize();
            Path normalizedClientJar = clientJar.toAbsolutePath().normalize();
            if (normalizedClientJar.startsWith(previousDirectory)) {
                relocatedClientJar = normalized.resolve(previousDirectory.relativize(normalizedClientJar));
            }
        }
        return new InstalledVersion(
                id,
                manifest,
                gameDirectory,
                normalized,
                normalized.resolve(id + ".json"),
                relocatedClientJar,
                librariesDirectory,
                assetsDirectory,
                assetIndexFile
        );
    }
}
