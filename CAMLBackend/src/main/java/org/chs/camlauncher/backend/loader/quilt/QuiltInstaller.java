package org.chs.camlauncher.backend.loader.quilt;

import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.io.DownloadExecutor;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceInstallLock;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.SharedLibraryTransaction;
import org.chs.camlauncher.backend.instance.VersionDirectoryStage;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** 原版 + Quilt Loader + 可选 Quilted Fabric API 的安装编排。 */
public final class QuiltInstaller {
    private final VersionInstaller versionInstaller;
    private final QuiltService quiltService;
    private final FileDownloader fileDownloader;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

    public QuiltInstaller(
            VersionInstaller versionInstaller,
            QuiltService quiltService,
            FileDownloader fileDownloader,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.quiltService = Objects.requireNonNull(quiltService, "quiltService");
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
            QuiltApiVersion apiVersion,
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
            QuiltApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        try (SharedLibraryTransaction libraryTransaction = SharedLibraryTransaction.begin(
                gameDirectory.resolve("libraries"))) {
            InstalledVersion installed = install(gameVersion, instanceId, loaderVersion, apiVersion,
                    gameDirectory, listener, token, libraryTransaction);
            libraryTransaction.commit();
            return installed;
        }
    }

    /** 把 loader 库变更纳入调用方事务；调用方负责最终 commit/rollback。 */
    public InstalledVersion install(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            QuiltApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            SharedLibraryTransaction libraryTransaction
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(libraryTransaction, "libraryTransaction");
        token.throwIfCancelled();
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(loaderVersion, "loaderVersion");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String normalizedInstanceId = InstanceNameRules.validate(instanceId);
        if (normalizedInstanceId.equals(gameVersion)) {
            throw new IllegalArgumentException("安装 Quilt 时实例名不能与游戏版本号相同");
        }
        Path targetVersionDirectory = SafePaths.resolveChild(
                gameDirectory.resolve("versions"), normalizedInstanceId, "version id");
        boolean versionCommitted = false;
        try (InstanceInstallLock ignored = InstanceInstallLock.acquire(
                gameDirectory, normalizedInstanceId)) {
            InstanceNameRules.ensureNewInstance(gameDirectory, normalizedInstanceId);
            versionInstaller.install(
                    gameVersion,
                    gameVersion,
                    gameDirectory,
                    manifestUri,
                    manifestCacheFile,
                    listener,
                    token
            );

            DownloadListener.notifyStage(listener, DownloadStage.of("quilt.config.fetch"));
            token.throwIfCancelled();
            String profileJson = quiltService.fetchLoaderProfile(gameVersion, loaderVersion);
            DownloadListener.notifyStage(listener, DownloadStage.of("quilt.loader.install"));
            try (VersionDirectoryStage stage = VersionDirectoryStage.create(
                    gameDirectory, normalizedInstanceId)) {
                InstalledVersion installed = versionInstaller.installProfileInto(
                        normalizedInstanceId,
                        gameDirectory,
                        stage.directory(),
                        profileJson,
                        listener,
                        token,
                        libraryTransaction
                );

                Path apiFile = null;
                if (apiVersion != null) {
                    DownloadListener.notifyStage(listener, DownloadStage.of("quilt.api.download"));
                    apiFile = downloadApi(apiVersion, stage.directory().resolve("mods"),
                            listener, token);
                }

                DownloadListener.notifyStage(listener, DownloadStage.of("quilt.verify"));
                ExternalLoaderInstaller.verifyInstalledLibraries("Quilt", installed);
                token.throwIfCancelled();
                installed = stage.commit(installed);
                versionCommitted = true;

                if (apiFile != null) {
                    Path rootMods = gameDirectory.resolve("mods");
                    Path rootApiFile = SafePaths.resolveChild(
                            rootMods, apiFile.getFileName().toString(), "Quilted Fabric API file");
                    Path installedApiFile = installed.versionDirectory().resolve("mods")
                            .resolve(apiFile.getFileName().toString());
                    copyIfDifferent(installedApiFile, rootApiFile);
                }
                return installed;
            }
        } catch (IOException | RuntimeException | InterruptedException e) {
            if (versionCommitted) {
                deleteRecursively(targetVersionDirectory);
            }
            throw e;
        }
    }

    private Path downloadApi(QuiltApiVersion apiVersion, Path modsDirectory,
                             DownloadListener listener, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        String filename;
        try {
            filename = Path.of(apiVersion.filename()).getFileName().toString();
        } catch (InvalidPathException e) {
            throw new IOException("Quilted Fabric API 文件名无效: " + apiVersion.filename(), e);
        }

        Path normalizedMods = modsDirectory.toAbsolutePath().normalize();
        Path destination = normalizedMods.resolve(filename).normalize();
        if (!destination.startsWith(normalizedMods)) {
            throw new IOException("Quilted Fabric API 文件名越界: " + apiVersion.filename());
        }

        DownloadExecutor.downloadSingle(new DownloadItem(
                        apiVersion.name(),
                        DownloadSources.candidates(sources, apiVersion.downloadUri().toString()),
                        destination,
                        apiVersion.sha1(),
                        apiVersion.size()
                ),
                listener,
                (item, itemListener) -> fileDownloader.download(item, itemListener, null, token),
                token);
        return destination;
    }

    private static void copyIfDifferent(Path source, Path target) throws IOException {
        if (Files.isRegularFile(target)
                && Files.size(source) == Files.size(target)
                && Files.mismatch(source, target) == -1) {
            return;
        }
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".caml-quilt-mod-", ".part");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
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

