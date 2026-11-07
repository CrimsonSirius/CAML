package org.chs.camlauncher.backend.loader.neoforge;

import java.net.URI;
import java.util.Objects;

/** 某个 Minecraft 版本可用的 NeoForge 版本。 */
public record NeoForgeVersion(String gameVersion, String version, URI installerUri) {
    public NeoForgeVersion {
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(installerUri, "installerUri");
    }

    public String versionId() {
        return "neoforge-" + version;
    }
}
