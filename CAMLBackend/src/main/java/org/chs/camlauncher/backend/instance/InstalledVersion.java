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
}
