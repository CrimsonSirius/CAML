package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.front.text.UiText;

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
            return UiText.tr("download.description.optifine", forge.version(), optiFine.version());
        }
        if (forge != null) {
            return UiText.tr("download.description.forge", forge.version());
        }
        if (fabricLoader != null) {
            return fabricApi == null
                    ? UiText.tr("download.description.fabric", fabricLoader.version())
                    : UiText.tr("download.description.fabricWithApi",
                            fabricLoader.version(), fabricApi.versionNumber());
        }
        if (neoForge != null) {
            return UiText.tr("download.description.neoforge", neoForge.version());
        }
        return UiText.tr("download.description.vanilla", version.id());
    }
}
