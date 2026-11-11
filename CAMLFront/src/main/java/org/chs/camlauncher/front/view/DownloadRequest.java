package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.language.Language;

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
            return Language.tr("download.description.optifine", forge.version(), optiFine.version());
        }
        if (forge != null) {
            return Language.tr("download.description.forge", forge.version());
        }
        if (fabricLoader != null) {
            return fabricApi == null
                    ? Language.tr("download.description.fabric", fabricLoader.version())
                    : Language.tr("download.description.fabricWithApi",
                            fabricLoader.version(), fabricApi.versionNumber());
        }
        if (neoForge != null) {
            return Language.tr("download.description.neoforge", neoForge.version());
        }
        return Language.tr("download.description.vanilla", version.id());
    }
}
