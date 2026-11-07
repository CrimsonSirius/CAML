package org.chs.camlauncher.backend.loader.optifine;

import java.net.URI;
import java.util.Objects;

/** 某个 Minecraft 版本可用的 OptiFine 版本。 */
public record OptiFineVersion(
        String gameVersion,
        String type,
        String patch,
        String filename,
        String forgeVersion,
        URI downloadUri
) {
    public OptiFineVersion {
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(downloadUri, "downloadUri");
    }

    /** 例如 HD_U_I6 或 HD_U_I6_pre4。 */
    public String version() {
        return type + "_" + patch;
    }

    /** 例如 1.20.1_HD_U_I6。 */
    public String fullVersion() {
        return gameVersion + "_" + type + "_" + patch;
    }

    public boolean preview() {
        return patch.startsWith("pre") || patch.startsWith("alpha");
    }
}
