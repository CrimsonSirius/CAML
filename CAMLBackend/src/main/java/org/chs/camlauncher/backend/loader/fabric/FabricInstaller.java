package org.chs.camlauncher.backend.loader.fabric;

import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.SafePaths;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** 原版 + Fabric Loader + 可选 Fabric API 的安装编排。 */
public final class FabricInstaller {
    private final VersionInstaller versionInstaller;
    private final FabricService fabricService;
    private final FileDownloader fileDownloader;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

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

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    }

    public InstalledVersion install(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return install(gameVersion, instanceId, loaderVersion, apiVersion, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion install(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(loaderVersion, "loaderVersion");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String normalizedInstanceId = InstanceNameRules.validate(instanceId);
        if (normalizedInstanceId.equals(gameVersion)) {
            throw new IllegalArgumentException("安装 Fabric 时实例名不能与游戏版本号相同");
        }
        InstanceNameRules.ensureNewInstance(gameDirectory, normalizedInstanceId);

        Path targetVersionDirectory = SafePaths.resolveChild(
                gameDirectory.resolve("versions"), normalizedInstanceId, "version id");
        try {
            versionInstaller.install(
                    gameVersion,
                    gameVersion,
                    gameDirectory,
                    manifestUri,
                    manifestCacheFile,
                    listener,
                    token
            );

            DownloadListener.notifyStage(listener, DownloadStage.of("fabric.config.fetch"));
            token.throwIfCancelled();
            String profileJson = fabricService.fetchLoaderProfile(gameVersion, loaderVersion);
            DownloadListener.notifyStage(listener, DownloadStage.of("fabric.loader.install"));
            InstalledVersion installed = versionInstaller.installProfile(
                    normalizedInstanceId,
                    gameDirectory,
                    profileJson,
                    listener,
                    token
            );

            if (apiVersion != null) {
                DownloadListener.notifyStage(listener, DownloadStage.of("fabric.api.download"));
                Path versionMods = targetVersionDirectory.resolve("mods");
                Path apiFile = downloadApi(apiVersion, versionMods, listener, token);
                Path rootMods = gameDirectory.resolve("mods");
                Path rootApiFile = SafePaths.resolveChild(
                        rootMods, apiFile.getFileName().toString(), "Fabric API file");
                copyIfDifferent(apiFile, rootApiFile);
            }
            DownloadListener.notifyStage(listener, DownloadStage.of("fabric.verify"));
            return installed;
        } catch (IOException | RuntimeException | InterruptedException e) {
            deleteRecursively(targetVersionDirectory);
            throw e;
        }
    }

    private Path downloadApi(FabricApiVersion apiVersion, Path modsDirectory,
                             DownloadListener listener, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
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
                DownloadSources.candidates(sources, apiVersion.downloadUri().toString()),
                destination,
                apiVersion.sha1(),
                apiVersion.size()
        ), listener, null, token);
        return destination;
    }

    private static void copyIfDifferent(Path source, Path target) throws IOException {
        if (Files.isRegularFile(target)
                && Files.size(source) == Files.size(target)
                && Files.mismatch(source, target) == -1) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 保留原始安装异常。
                }
            }
        } catch (IOException ignored) {
            // 保留原始安装异常。
        }
    }
}
