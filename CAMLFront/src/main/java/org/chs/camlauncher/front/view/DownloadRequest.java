package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;

import java.nio.file.Path;

/** 下载设置页提交给下载进度页的不可变快照。 */
record DownloadRequest(
        VersionManifest.Version version,
        String instanceId,
        Path gameDirectory,
        ForgeVersion forge,
        FabricLoaderVersion fabricLoader,
        FabricApiVersion fabricApi,
        OptiFineVersion optiFine,
        NeoForgeVersion neoForge
) {
    String description() {
        if (optiFine != null) {
            return "Forge " + forge.version() + " + OptiFine " + optiFine.version();
        }
        if (forge != null) {
            return "Forge " + forge.version();
        }
        if (fabricLoader != null) {
            String api = fabricApi == null ? "" : " + Fabric API " + fabricApi.versionNumber();
            return "Fabric Loader " + fabricLoader.version() + api;
        }
        if (neoForge != null) {
            return "NeoForge " + neoForge.version();
        }
        return "Minecraft " + version.id();
    }
}
