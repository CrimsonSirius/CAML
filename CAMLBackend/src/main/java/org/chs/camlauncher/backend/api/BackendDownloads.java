package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.LoaderVersionCache;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricService;
import org.chs.camlauncher.backend.loader.forge.ForgeService;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeService;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineInstaller;
import org.chs.camlauncher.backend.loader.optifine.OptiFineService;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.loader.quilt.QuiltApiVersion;
import org.chs.camlauncher.backend.loader.quilt.QuiltInstaller;
import org.chs.camlauncher.backend.loader.quilt.QuiltLoaderVersion;
import org.chs.camlauncher.backend.loader.quilt.QuiltService;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.mod.model.ModContentType;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.mod.service.ModService;
import org.chs.camlauncher.backend.mod.service.ModIconService;
import org.chs.camlauncher.backend.modpack.ModpackInstaller;
import org.chs.camlauncher.backend.settings.SettingsService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** 版本、加载器、Mod 和整合包下载入口。 */
public final class BackendDownloads {
    private static final Logger LOGGER = LogManager.getLogger(BackendDownloads.class);
    private static final Duration VERSION_MANIFEST_CACHE_TTL = Duration.ofMinutes(5);
    private static final URI MANIFEST_URI = VersionManifestService.DEFAULT_MANIFEST_URI;

    private final Path dataDirectory;
    private final SettingsService settingsService;
    private final VersionManifestService versionManifestService;
    private final AtomicLong versionManifestGeneration = new AtomicLong();
    private final LoaderVersionCache loaderVersionCache = new LoaderVersionCache();
    private final DownloadPlanner downloadPlanner;
    private final AssetDownloadPlanner assetDownloadPlanner;
    private final ModService modService;
    private final ModpackInstaller modpackInstaller;
    private final ModIconService modIconService;
    private final VersionInstaller versionInstaller;
    private final FabricService fabricService;
    private final FabricInstaller fabricInstaller;
    private final ForgeService forgeService;
    private final NeoForgeService neoForgeService;
    private final QuiltService quiltService;
    private final QuiltInstaller quiltInstaller;
    private final OptiFineService optiFineService;
    private final OptiFineInstaller optiFineInstaller;
    private final ExternalLoaderInstaller externalLoaderInstaller;
    private volatile int downloadConcurrency = DownloadConcurrency.DEFAULT;
    private volatile int archiveMaxEntries = SettingsService.DEFAULT_ARCHIVE_MAX_ENTRIES;
    private volatile long archiveMaxEntryMiB = SettingsService.DEFAULT_ARCHIVE_MAX_ENTRY_MIB;
    private volatile long archiveMaxTotalMiB = SettingsService.DEFAULT_ARCHIVE_MAX_TOTAL_MIB;
    private volatile DownloadSourceMode downloadSourceMode = SettingsService.DEFAULT_DOWNLOAD_SOURCE;
    private volatile String customDownloadApi = "";
    private volatile CachedVersionManifest cachedVersionManifest;

    BackendDownloads(
            Path dataDirectory,
            SettingsService settingsService,
            VersionManifestService versionManifestService,
            DownloadPlanner downloadPlanner,
            AssetDownloadPlanner assetDownloadPlanner,
            ModService modService,
            ModpackInstaller modpackInstaller,
            ModIconService modIconService,
            VersionInstaller versionInstaller,
            FabricService fabricService,
            FabricInstaller fabricInstaller,
            ForgeService forgeService,
            NeoForgeService neoForgeService,
            QuiltService quiltService,
            QuiltInstaller quiltInstaller,
            OptiFineService optiFineService,
            OptiFineInstaller optiFineInstaller,
            ExternalLoaderInstaller externalLoaderInstaller
    ) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.versionManifestService = Objects.requireNonNull(
                versionManifestService, "versionManifestService");
        this.downloadPlanner = Objects.requireNonNull(downloadPlanner, "downloadPlanner");
        this.assetDownloadPlanner = Objects.requireNonNull(
                assetDownloadPlanner, "assetDownloadPlanner");
        this.modService = Objects.requireNonNull(modService, "modService");
        this.modpackInstaller = Objects.requireNonNull(modpackInstaller, "modpackInstaller");
        this.modIconService = Objects.requireNonNull(modIconService, "modIconService");
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.fabricService = Objects.requireNonNull(fabricService, "fabricService");
        this.fabricInstaller = Objects.requireNonNull(fabricInstaller, "fabricInstaller");
        this.forgeService = Objects.requireNonNull(forgeService, "forgeService");
        this.neoForgeService = Objects.requireNonNull(neoForgeService, "neoForgeService");
        this.quiltService = Objects.requireNonNull(quiltService, "quiltService");
        this.quiltInstaller = Objects.requireNonNull(quiltInstaller, "quiltInstaller");
        this.optiFineService = Objects.requireNonNull(optiFineService, "optiFineService");
        this.optiFineInstaller = Objects.requireNonNull(optiFineInstaller, "optiFineInstaller");
        this.externalLoaderInstaller = Objects.requireNonNull(
                externalLoaderInstaller, "externalLoaderInstaller");
    }

    void initialize() {
        try {
            downloadConcurrency = settingsService.downloadConcurrency();
        } catch (IOException e) {
            LOGGER.warn("无法读取下载并发设置，使用默认值 {}", DownloadConcurrency.DEFAULT, e);
            downloadConcurrency = DownloadConcurrency.DEFAULT;
        }
        versionInstaller.setDownloadConcurrency(downloadConcurrency);
        externalLoaderInstaller.setDownloadConcurrency(downloadConcurrency);
        modpackInstaller.setDownloadConcurrency(downloadConcurrency);

        try {
            archiveMaxEntries = settingsService.archiveMaxEntries();
            archiveMaxEntryMiB = settingsService.archiveMaxEntryMiB();
            archiveMaxTotalMiB = settingsService.archiveMaxTotalMiB();
        } catch (IOException e) {
            LOGGER.warn("无法读取压缩包限制，使用默认值", e);
            archiveMaxEntries = SettingsService.DEFAULT_ARCHIVE_MAX_ENTRIES;
            archiveMaxEntryMiB = SettingsService.DEFAULT_ARCHIVE_MAX_ENTRY_MIB;
            archiveMaxTotalMiB = SettingsService.DEFAULT_ARCHIVE_MAX_TOTAL_MIB;
        }
        applyArchiveExtractionLimits();

        try {
            downloadSourceMode = settingsService.downloadSourceMode();
            customDownloadApi = settingsService.customDownloadApi();
        } catch (IOException e) {
            LOGGER.warn("无法读取下载源设置，使用默认值 {}", SettingsService.DEFAULT_DOWNLOAD_SOURCE, e);
            downloadSourceMode = SettingsService.DEFAULT_DOWNLOAD_SOURCE;
            customDownloadApi = "";
        }
        applyDownloadSources();
    }

    public int downloadConcurrency() {
        return downloadConcurrency;
    }

    public void setDownloadConcurrency(int value) throws IOException {
        settingsService.setDownloadConcurrency(value);
        downloadConcurrency = value;
        versionInstaller.setDownloadConcurrency(value);
        externalLoaderInstaller.setDownloadConcurrency(value);
        modpackInstaller.setDownloadConcurrency(value);
    }

    public int archiveMaxEntries() {
        return archiveMaxEntries;
    }

    public long archiveMaxEntryMiB() {
        return archiveMaxEntryMiB;
    }

    public long archiveMaxTotalMiB() {
        return archiveMaxTotalMiB;
    }

    public void setArchiveExtractionLimits(int maxEntries, long maxEntryMiB, long maxTotalMiB)
            throws IOException {
        settingsService.setArchiveExtractionLimits(maxEntries, maxEntryMiB, maxTotalMiB);
        archiveMaxEntries = maxEntries;
        archiveMaxEntryMiB = maxEntryMiB;
        archiveMaxTotalMiB = maxTotalMiB;
        applyArchiveExtractionLimits();
    }

    public DownloadSourceMode downloadSourceMode() {
        return downloadSourceMode;
    }

    public String customDownloadApi() {
        return customDownloadApi;
    }

    public void setDownloadSource(DownloadSourceMode mode, String customApi) throws IOException {
        Objects.requireNonNull(mode, "mode");
        String value = customApi == null ? "" : customApi.trim();
        settingsService.setDownloadSource(mode, value);
        downloadSourceMode = mode;
        customDownloadApi = value;
        applyDownloadSources();
        versionManifestGeneration.incrementAndGet();
        cachedVersionManifest = null;
        loaderVersionCache.invalidateAll();
    }

    public VersionManifest fetchVersionManifest() throws IOException, InterruptedException {
        CachedVersionManifest cached = cachedVersionManifest;
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.manifest();
        }

        long generation = versionManifestGeneration.get();
        VersionManifest manifest = versionManifestService.fetchManifestOrCache(
                MANIFEST_URI,
                dataDirectory.resolve("cache").resolve("version_manifest_v2.json")
        );
        if (versionManifestGeneration.get() == generation) {
            cachedVersionManifest = new CachedVersionManifest(
                    manifest, Instant.now().plus(VERSION_MANIFEST_CACHE_TTL));
        }
        return manifest;
    }

    public ModSearchPage searchMods(ModSearchQuery query)
            throws IOException, InterruptedException {
        return modService.search(query);
    }

    public ModProjectDetail fetchModProject(ModSource source, String projectId)
            throws IOException, InterruptedException {
        return modService.project(source, projectId);
    }

    public ModDownloadResult downloadMod(
            ModFile file, Path gameDirectory, String instanceId,
            VersionIsolationMode isolationMode, DownloadListener listener)
            throws IOException, InterruptedException {
        return downloadMod(file, gameDirectory, instanceId, isolationMode, listener,
                new DownloadCancellationToken());
    }

    public ModDownloadResult downloadMod(
            ModFile file, Path gameDirectory, String instanceId,
            VersionIsolationMode isolationMode, DownloadListener listener,
            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        return modService.download(file, ModContentType.MOD, gameDirectory, instanceId,
                isolationMode, listener, token);
    }

    public ModDownloadResult downloadContent(
            ModContentType type, ModFile file, Path gameDirectory, String instanceId,
            VersionIsolationMode isolationMode, DownloadListener listener)
            throws IOException, InterruptedException {
        return downloadContent(type, file, gameDirectory, instanceId, isolationMode, listener,
                new DownloadCancellationToken());
    }

    public ModDownloadResult downloadContent(
            ModContentType type, ModFile file, Path gameDirectory, String instanceId,
            VersionIsolationMode isolationMode, DownloadListener listener,
            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        return modService.download(file, type, gameDirectory, instanceId, isolationMode, listener, token);
    }

    public ModDownloadResult installModpack(
            ModFile file, Path gameDirectory, String instanceName,
            DownloadListener listener, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        return modpackInstaller.install(file, gameDirectory, instanceName, listener, token);
    }

    public boolean curseForgeConfigured() {
        return modService.curseForgeConfigured();
    }

    public byte[] loadModIcon(String iconUrl) throws IOException, InterruptedException {
        return modIconService.load(iconUrl);
    }

    public InstalledVersion installVersion(
            String versionId, Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installVersion(versionId, versionId, gameDirectory, listener);
    }

    public InstalledVersion installVersion(
            String versionId, String instanceId, Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installVersion(versionId, instanceId, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installVersion(
            String versionId, String instanceId, Path gameDirectory, DownloadListener listener,
            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        InstanceNameRules.ensureNewInstance(gameDirectory, instanceId);
        return versionInstaller.install(
                versionId,
                instanceId,
                gameDirectory,
                MANIFEST_URI,
                dataDirectory.resolve("cache").resolve("version_manifest_v2.json"),
                listener,
                token
        );
    }

    public List<FabricLoaderVersion> fetchFabricLoaderVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.FABRIC_LOADER, gameVersion,
                () -> fabricService.fetchLoaderVersions(gameVersion));
    }

    public List<FabricApiVersion> fetchFabricApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.FABRIC_API, gameVersion,
                () -> fabricService.fetchApiVersions(gameVersion));
    }

    public InstalledVersion installFabricVersion(
            String gameVersion, String instanceId, String loaderVersion,
            FabricApiVersion apiVersion, Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installFabricVersion(gameVersion, instanceId, loaderVersion, apiVersion,
                gameDirectory, listener, new DownloadCancellationToken());
    }

    public InstalledVersion installFabricVersion(
            String gameVersion, String instanceId, String loaderVersion,
            FabricApiVersion apiVersion, Path gameDirectory, DownloadListener listener,
            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        return fabricInstaller.install(gameVersion, instanceId, loaderVersion, apiVersion,
                gameDirectory, listener, token);
    }

    public List<ForgeVersion> fetchForgeVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.FORGE, gameVersion,
                () -> forgeService.fetchVersions(gameVersion));
    }

    public List<NeoForgeVersion> fetchNeoForgeVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.NEOFORGE, gameVersion,
                () -> neoForgeService.fetchVersions(gameVersion));
    }

    public InstalledVersion installForgeVersion(
            String gameVersion, String instanceId, ForgeVersion version,
            Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installForgeVersion(gameVersion, instanceId, version, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installForgeVersion(
            String gameVersion, String instanceId, ForgeVersion version,
            Path gameDirectory, DownloadListener listener, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        return externalLoaderInstaller.install(
                "Forge", gameVersion, instanceId, version.installerUri(),
                gameDirectory, listener, token);
    }

    public InstalledVersion installNeoForgeVersion(
            String gameVersion, String instanceId, NeoForgeVersion version,
            Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installNeoForgeVersion(gameVersion, instanceId, version, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installNeoForgeVersion(
            String gameVersion, String instanceId, NeoForgeVersion version,
            Path gameDirectory, DownloadListener listener, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        return externalLoaderInstaller.install(
                "NeoForge", gameVersion, instanceId, version.installerUri(),
                gameDirectory, listener, token);
    }

    public List<OptiFineVersion> fetchOptiFineVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.OPTIFINE, gameVersion,
                () -> optiFineService.fetchVersions(gameVersion));
    }

    public List<QuiltLoaderVersion> fetchQuiltLoaderVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.QUILT_LOADER, gameVersion,
                () -> quiltService.fetchLoaderVersions(gameVersion));
    }

    public List<QuiltApiVersion> fetchQuiltApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        return loaderVersionCache.get(LoaderVersionCache.Kind.QUILT_API, gameVersion,
                () -> quiltService.fetchApiVersions(gameVersion));
    }

    public InstalledVersion installQuiltVersion(
            String gameVersion, String instanceId, String loaderVersion,
            QuiltApiVersion apiVersion, Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installQuiltVersion(gameVersion, instanceId, loaderVersion, apiVersion,
                gameDirectory, listener, new DownloadCancellationToken());
    }

    public InstalledVersion installQuiltVersion(
            String gameVersion, String instanceId, String loaderVersion,
            QuiltApiVersion apiVersion, Path gameDirectory, DownloadListener listener,
            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        return quiltInstaller.install(gameVersion, instanceId, loaderVersion, apiVersion,
                gameDirectory, listener, token);
    }

    public InstalledVersion installForgeOptiFineVersion(
            String gameVersion, String instanceId, ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion, Path gameDirectory, DownloadListener listener)
            throws IOException, InterruptedException {
        return installForgeOptiFineVersion(gameVersion, instanceId, forgeVersion,
                optiFineVersion, gameDirectory, listener, new DownloadCancellationToken());
    }

    public InstalledVersion installForgeOptiFineVersion(
            String gameVersion, String instanceId, ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion, Path gameDirectory, DownloadListener listener,
            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        Objects.requireNonNull(forgeVersion, "forgeVersion");
        Objects.requireNonNull(optiFineVersion, "optiFineVersion");
        return optiFineInstaller.install(gameVersion, instanceId, forgeVersion,
                optiFineVersion, gameDirectory, listener, token);
    }

    private void applyArchiveExtractionLimits() {
        long entryBytes = archiveMaxEntryMiB * 1024L * 1024L;
        long totalBytes = archiveMaxTotalMiB * 1024L * 1024L;
        modService.setArchiveExtractionLimits(archiveMaxEntries, entryBytes, totalBytes);
        modpackInstaller.setArchiveExtractionLimits(archiveMaxEntries, entryBytes, totalBytes);
    }

    private void applyDownloadSources() {
        List<DownloadSource> sources = DownloadSourceFactory.create(
                downloadSourceMode, customDownloadApi);
        versionManifestService.setSources(sources);
        downloadPlanner.setSources(sources);
        assetDownloadPlanner.setSources(sources);
        externalLoaderInstaller.setSources(sources);
        fabricInstaller.setSources(sources);
        fabricService.setSources(sources);
        forgeService.setSources(sources);
        forgeService.setSourceMode(downloadSourceMode, customDownloadApi);
        neoForgeService.setSources(sources);
        neoForgeService.setSourceMode(downloadSourceMode, customDownloadApi);
        quiltInstaller.setSources(sources);
        quiltService.setSources(sources);
        optiFineService.setSources(sources);
        optiFineService.setSourceMode(downloadSourceMode, customDownloadApi);
        optiFineInstaller.setSources(sources);
    }

    private record CachedVersionManifest(VersionManifest manifest, Instant expiresAt) {
    }

}
