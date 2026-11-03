package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.download.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadItem;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadPlan;
import org.chs.camlauncher.backend.download.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.metadata.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.ManifestResolver;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.metadata.VersionManifest;
import org.chs.camlauncher.backend.metadata.VersionManifestService;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class VersionInstaller {
    private final MetadataService metadata;
    private final VersionManifestService manifestService;
    private final DownloadPlanner downloadPlanner;
    private final AssetDownloadPlanner assetDownloadPlanner;
    private final FileDownloader fileDownloader;
    private final ManifestResolver manifestResolver = new ManifestResolver();

    public VersionInstaller() {
        this(
                new MetadataService(),
                new VersionManifestService(),
                new DownloadPlanner(),
                new AssetDownloadPlanner(),
                new FileDownloader()
        );
    }

    public VersionInstaller(
            MetadataService metadata,
            VersionManifestService manifestService,
            DownloadPlanner downloadPlanner,
            AssetDownloadPlanner assetDownloadPlanner,
            FileDownloader fileDownloader
    ) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.manifestService = Objects.requireNonNull(manifestService, "manifestService");
        this.downloadPlanner = Objects.requireNonNull(downloadPlanner, "downloadPlanner");
        this.assetDownloadPlanner = Objects.requireNonNull(assetDownloadPlanner, "assetDownloadPlanner");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
    }

    public InstalledVersion install(
            String versionId,
            Path gameDirectory,
            URI manifestUri,
            Path manifestCacheFile,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        VersionManifest manifest = manifestService.fetchManifestOrCache(manifestUri, manifestCacheFile);
        VersionManifest.Version remoteVersion = manifest.versions().stream()
                .filter(version -> versionId.equals(version.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown version: " + versionId));

        String versionJson = manifestService.fetchVersionJson(remoteVersion);
        VersionInfo resolved = resolve(metadata.parseVersionInfo(versionJson), gameDirectory);

        Path versionDirectory = gameDirectory.resolve("versions").resolve(versionId);
        Path versionFile = versionDirectory.resolve(versionId + ".json");
        Path clientJar = versionDirectory.resolve(versionId + ".jar");
        Path librariesDirectory = gameDirectory.resolve("libraries");
        Path assetsDirectory = gameDirectory.resolve("assets");

        downloadPlan(downloadPlanner.plan(resolved, gameDirectory), listener);

        Path assetIndexFile = null;
        if (resolved.assetIndex() != null) {
            assetIndexFile = assetsDirectory.resolve("indexes").resolve(resolved.assetIndex().id() + ".json");
            AssetIndexFile index = metadata.readAssetIndex(assetIndexFile);
            downloadPlan(assetDownloadPlanner.plan(index, assetsDirectory), listener);
        }

        writeAtomically(versionFile, versionJson);
        return new InstalledVersion(
                versionId,
                resolved,
                gameDirectory,
                versionDirectory,
                versionFile,
                clientJar,
                librariesDirectory,
                assetsDirectory,
                assetIndexFile
        );
    }

    private void downloadPlan(DownloadPlan plan, DownloadListener listener)
            throws IOException, InterruptedException {
        for (DownloadItem item : plan.missing()) {
            fileDownloader.download(item, listener);
        }
    }

    private VersionInfo resolve(VersionInfo version, Path gameDirectory) throws IOException {
        if (version.inheritsFrom() == null) {
            return version;
        }

        Map<String, VersionInfo> parents = new LinkedHashMap<>();
        VersionInfo current = version;
        while (current.inheritsFrom() != null) {
            String parentId = current.inheritsFrom();
            if (parents.containsKey(parentId)) {
                throw new IllegalArgumentException("Circular inheritance: " + parentId);
            }

            Path parentFile = gameDirectory
                    .resolve("versions")
                    .resolve(parentId)
                    .resolve(parentId + ".json");
            if (!Files.isRegularFile(parentFile)) {
                throw new IOException("Parent version is not installed: " + parentId);
            }

            VersionInfo parent = metadata.readVersionInfo(parentFile);
            parents.put(parent.id(), parent);
            current = parent;
        }
        return manifestResolver.resolve(version, parents);
    }

    private static void writeAtomically(Path file, String text) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporary = Files.createTempFile(parent, "caml-version-", ".part");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
