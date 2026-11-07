package org.chs.camlauncher.backend.loader.fabric;

import org.chs.camlauncher.backend.download.DownloadItem;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionInstaller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** 原版 + Fabric Loader + 可选 Fabric API 的安装编排。 */
public final class FabricInstaller {
    private final VersionInstaller versionInstaller;
    private final FabricService fabricService;
    private final FileDownloader fileDownloader;
    private final URI manifestUri;
    private final Path manifestCacheFile;

    public FabricInstaller(
            VersionInstaller versionInstaller,
            FabricService fabricService,
            FileDownloader fileDownloader,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.fabricService = Objects.requireNonNull(fabricService, "fabricService");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
    }

    public InstalledVersion install(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            boolean fullVersionIsolation,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(loaderVersion, "loaderVersion");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String normalizedInstanceId = instanceId.trim();
        if (normalizedInstanceId.equals(gameVersion)) {
            throw new IllegalArgumentException("安装 Fabric 时实例名不能与游戏版本号相同");
        }

        versionInstaller.install(
                gameVersion,
                gameVersion,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                listener
        );

        String profileJson = fabricService.fetchLoaderProfile(gameVersion, loaderVersion);
        InstalledVersion installed = versionInstaller.installProfile(
                normalizedInstanceId,
                gameDirectory,
                profileJson,
                listener
        );

        if (apiVersion != null) {
            Path modsDirectory = fullVersionIsolation
                    ? gameDirectory.resolve("versions").resolve(normalizedInstanceId).resolve("mods")
                    : gameDirectory.resolve("mods");
            downloadApi(apiVersion, modsDirectory, listener);
        }
        return installed;
    }

    private void downloadApi(FabricApiVersion apiVersion, Path modsDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        String filename;
        try {
            filename = Path.of(apiVersion.filename()).getFileName().toString();
        } catch (InvalidPathException e) {
            throw new IOException("Fabric API 文件名无效: " + apiVersion.filename(), e);
        }

        Path normalizedMods = modsDirectory.toAbsolutePath().normalize();
        Path destination = normalizedMods.resolve(filename).normalize();
        if (!destination.startsWith(normalizedMods)) {
            throw new IOException("Fabric API 文件名越界: " + apiVersion.filename());
        }

        fileDownloader.download(new DownloadItem(
                apiVersion.name(),
                List.of(apiVersion.downloadUri()),
                destination,
                apiVersion.sha1(),
                apiVersion.size()
        ), listener);
    }
}
