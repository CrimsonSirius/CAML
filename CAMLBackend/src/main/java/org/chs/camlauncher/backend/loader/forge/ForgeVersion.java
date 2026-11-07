package org.chs.camlauncher.backend.loader.forge;

import java.net.URI;
import java.util.Objects;

/** 某个 Minecraft 版本可用的 Forge 版本。 */
public record ForgeVersion(String gameVersion, String version, URI installerUri) {
    public ForgeVersion {
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(installerUri, "installerUri");
    }

    public String versionId() {
        return gameVersion + "-forge-" + version;
    }
}
