package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;
import org.chs.camlauncher.backend.download.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifest;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.runtime.JavaLocator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

public final class CAMLBackend implements AutoCloseable {
    public static final String VERSION = "1.0-SNAPSHOT";

    private static final Logger LOGGER = LogManager.getLogger(CAMLBackend.class);

    private final BackendConfig config;
    private final JsonService json;
    private final MetadataService metadata;
    private final JavaLocator javaLocator;
    private final VersionManifestService versionManifestService;
    private final DownloadPlanner downloadPlanner;
    private final AssetDownloadPlanner assetDownloadPlanner;
    private final FileDownloader fileDownloader;
    private final VersionInstaller versionInstaller;
    private boolean started;

    public CAMLBackend(BackendConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.json = new JsonService();
        this.metadata = new MetadataService();
        this.javaLocator = new JavaLocator();
        this.versionManifestService = new VersionManifestService();
        this.downloadPlanner = new DownloadPlanner();
        this.assetDownloadPlanner = new AssetDownloadPlanner();
        this.fileDownloader = new FileDownloader();
        this.versionInstaller = new VersionInstaller(
                metadata,
                versionManifestService,
                downloadPlanner,
                assetDownloadPlanner,
                fileDownloader
        );
    }

    public void start() {
        if (started) {
            return;
        }

        try {
            BackendPaths.ensure(config);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare CAML directories", e);
        }

        started = true;
        LOGGER.info("CAML backend {} started", VERSION);
        LOGGER.info("Data directory: {}", config.dataDirectory());
        LOGGER.info("Game directory: {}", config.gameDirectory());
    }

    public BackendStatus status() {
        return new BackendStatus(started, VERSION, config.dataDirectory(), config.gameDirectory());
    }

    public JsonService json() {
        return json;
    }

    public MetadataService metadata() {
        return metadata;
    }

    public JavaLocator javaLocator() {
        return javaLocator;
    }

    public VersionManifestService versionManifestService() {
        return versionManifestService;
    }

    public VersionManifest fetchVersionManifest() throws java.io.IOException, InterruptedException {
        return versionManifestService.fetchManifestOrCache(
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json")
        );
    }

    public DownloadPlanner downloadPlanner() {
        return downloadPlanner;
    }

    public AssetDownloadPlanner assetDownloadPlanner() {
        return assetDownloadPlanner;
    }

    public FileDownloader fileDownloader() {
        return fileDownloader;
    }

    public VersionInstaller versionInstaller() {
        return versionInstaller;
    }

    public InstalledVersion installVersion(String versionId, DownloadListener listener)
            throws java.io.IOException, InterruptedException {
        return versionInstaller.install(
                versionId,
                config.gameDirectory(),
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json"),
                listener
        );
    }

    @Override
    public void close() {
        if (started) {
            started = false;
            LOGGER.info("CAML backend stopped");
        }
    }
}
