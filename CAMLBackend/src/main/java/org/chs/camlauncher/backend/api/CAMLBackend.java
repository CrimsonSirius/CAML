package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.AccountService;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjectorService;
import org.chs.camlauncher.backend.auth.store.AccountStore;
import org.chs.camlauncher.backend.auth.store.AuthServerStore;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerService;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.GameDataLayout;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceNameCheck;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.InstanceInspector;
import org.chs.camlauncher.backend.instance.InstanceModIconService;
import org.chs.camlauncher.backend.instance.InstanceEnvironment;
import org.chs.camlauncher.backend.instance.InstanceEnvironmentResolver;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.log.LatestLogService;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricService;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.LoaderVersionCache;
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
import org.chs.camlauncher.backend.launch.GameLaunch;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.launch.LaunchPlanner;
import org.chs.camlauncher.backend.launch.classpath.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.command.GameArgumentsBuilder;
import org.chs.camlauncher.backend.launch.command.JvmArgumentsBuilder;
import org.chs.camlauncher.backend.launch.command.JvmArgumentsParser;
import org.chs.camlauncher.backend.launch.command.LaunchCommandBuilder;
import org.chs.camlauncher.backend.launch.prepare.NativeExtractor;
import org.chs.camlauncher.backend.launch.prepare.VirtualAssetInstaller;
import org.chs.camlauncher.backend.launch.process.ProcessLauncher;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.language.LanguageSnapshot;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.mod.service.ModService;
import org.chs.camlauncher.backend.mod.service.ModIconService;
import org.chs.camlauncher.backend.mod.source.CurseForgeClient;
import org.chs.camlauncher.backend.mod.source.CurseForgeFingerprint;
import org.chs.camlauncher.backend.mod.source.ModrinthClient;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaCandidate;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.language.contract.LanguageEventListener;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.backend.system.SystemMemoryService;
import org.chs.camlauncher.backend.system.DirectoryOpener;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.backend.theme.ThemeService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class CAMLBackend implements AutoCloseable {
    public static final String VERSION = "1.0-SNAPSHOT";

    private static final Logger LOGGER = LogManager.getLogger(CAMLBackend.class);
    private static final Duration VERSION_MANIFEST_CACHE_TTL = Duration.ofMinutes(5);

    private final BackendConfig config;
    private final JsonService json;
    private final MetadataService metadata;
    private final JavaLocator javaLocator;
    private final VersionManifestService versionManifestService;
    private final AtomicLong versionManifestGeneration = new AtomicLong();
    private final LoaderVersionCache loaderVersionCache = new LoaderVersionCache();
    private final DownloadPlanner downloadPlanner;
    private final AssetDownloadPlanner assetDownloadPlanner;
    private final FileDownloader fileDownloader;
    private final ModService modService;
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
    private final LaunchPlanner launchPlanner;
    private final ClasspathBuilder classpathBuilder;
    private final JvmArgumentsBuilder jvmArgumentsBuilder;
    private final GameArgumentsBuilder gameArgumentsBuilder;
    private final NativeExtractor nativeExtractor;
    private final VirtualAssetInstaller virtualAssetInstaller;
    private final LaunchCommandBuilder launchCommandBuilder;
    private final ProcessLauncher processLauncher;
    private final GameLaunchService gameLaunchService;
    private final ThemeService themeService;
    private final SettingsService settingsService;
    private final LanguageService languageService;
    private final SystemMemoryService systemMemoryService;
    private final LatestLogService latestLogService;
    private final AccountStore accountStore;
    private final AuthServerService authServerService;
    private final AuthlibInjectorService authlibInjectorService;
    private final AccountService accountService;
    private volatile int downloadConcurrency = DownloadConcurrency.DEFAULT;
    private volatile DownloadSourceMode downloadSourceMode = SettingsService.DEFAULT_DOWNLOAD_SOURCE;
    private volatile String customDownloadApi = "";
    private volatile boolean autoGameLanguage = true;
    private volatile CachedVersionManifest cachedVersionManifest;
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
        this.modService = new ModService(
                new ModrinthClient(), new CurseForgeClient(), fileDownloader);
        this.modIconService = new ModIconService(config.dataDirectory());
        this.versionInstaller = new VersionInstaller(
                metadata,
                versionManifestService,
                downloadPlanner,
                assetDownloadPlanner,
                fileDownloader
        );
        this.fabricService = new FabricService();
        this.fabricInstaller = new FabricInstaller(
                versionInstaller,
                fabricService,
                fileDownloader,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json")
        );
        this.forgeService = new ForgeService();
        this.neoForgeService = new NeoForgeService();
        this.quiltService = new QuiltService();
        this.quiltInstaller = new QuiltInstaller(
                versionInstaller,
                quiltService,
                fileDownloader,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json")
        );
        this.optiFineService = new OptiFineService();
        this.externalLoaderInstaller = new ExternalLoaderInstaller(
                versionInstaller,
                javaLocator,
                fileDownloader,
                metadata,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json")
        );
        this.optiFineInstaller = new OptiFineInstaller(
                versionInstaller,
                externalLoaderInstaller,
                javaLocator,
                fileDownloader,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json"),
                optiFineService
        );
        this.launchPlanner = new LaunchPlanner();
        this.classpathBuilder = new ClasspathBuilder();
        this.jvmArgumentsBuilder = new JvmArgumentsBuilder();
        this.gameArgumentsBuilder = new GameArgumentsBuilder();
        this.nativeExtractor = new NativeExtractor();
        this.virtualAssetInstaller = new VirtualAssetInstaller();
        this.launchCommandBuilder = new LaunchCommandBuilder();
        this.processLauncher = new ProcessLauncher();
        this.themeService = new ThemeService(config.dataDirectory());
        this.settingsService = new SettingsService(config.dataDirectory());
        this.languageService = new LanguageService(config.dataDirectory(), settingsService);
        this.systemMemoryService = new SystemMemoryService();
        this.latestLogService = new LatestLogService(
                config.dataDirectory().resolve("logs").resolve("latest.log"));
        this.gameLaunchService = new GameLaunchService(
                config.gameDirectory(),
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json"),
                versionInstaller,
                javaLocator,
                launchPlanner,
                classpathBuilder,
                nativeExtractor,
                virtualAssetInstaller,
                processLauncher
        );
        this.accountStore = new AccountStore(config.dataDirectory(), json);
        this.authServerService = new AuthServerService(
                new AuthServerStore(config.dataDirectory(), json));
        this.authlibInjectorService = new AuthlibInjectorService(
                config.dataDirectory(), fileDownloader);
        this.accountService = new AccountService(
                accountStore, authServerService, authlibInjectorService, settingsService);

    }

    public void start() {
        if (started) {
            return;
        }

        try {
            BackendPaths.ensure(config);
            themeService.ensureDefaultTheme();
            latestLogService.start();
            languageService.initialize();
        } catch (IOException e) {
            LOGGER.error("后端初始化失败，无法准备目录、主题、日志或语言系统", e);
            throw new UncheckedIOException("Failed to prepare CAML directories", e);
        }
        try {
            downloadConcurrency = settingsService.downloadConcurrency();
        } catch (IOException e) {
            LOGGER.warn("无法读取下载并发设置，使用默认值 {}", DownloadConcurrency.DEFAULT, e);
            downloadConcurrency = DownloadConcurrency.DEFAULT;
        }
        versionInstaller.setDownloadConcurrency(downloadConcurrency);
        externalLoaderInstaller.setDownloadConcurrency(downloadConcurrency);
        try {
            downloadSourceMode = settingsService.downloadSourceMode();
            customDownloadApi = settingsService.customDownloadApi();
        } catch (IOException e) {
            LOGGER.warn("无法读取下载源设置，使用默认值 {}", SettingsService.DEFAULT_DOWNLOAD_SOURCE, e);
            downloadSourceMode = SettingsService.DEFAULT_DOWNLOAD_SOURCE;
            customDownloadApi = "";
        }
        try {
            autoGameLanguage = settingsService.autoGameLanguage();
        } catch (IOException e) {
            LOGGER.warn("无法读取自动游戏语言设置，使用默认值 true", e);
            autoGameLanguage = true;
        }
        applyDownloadSources();
        try {
            gameLaunchService.setJavaSelection(
                    settingsService.javaSelectionMode(),
                    settingsService.selectedJavaPath().orElse(null)
            );
        } catch (IOException e) {
            LOGGER.warn("无法读取 Java 选择设置，使用自动选择", e);
            gameLaunchService.setJavaSelection(JavaSelectionMode.AUTO, null);
        }
        try {
            authlibInjectorService.setOverride(
                    settingsService.authlibInjectorPath().orElse(null));
        } catch (IOException e) {
            LOGGER.warn("无法读取 authlib-injector 路径设置，使用内置版本", e);
            authlibInjectorService.setOverride(null);
        }

        started = true;
        LOGGER.info("CAML backend {} started", VERSION);
        LOGGER.info("Data directory: {}", config.dataDirectory());
        LOGGER.info("Game directory: {}", config.gameDirectory());
        LOGGER.info("Themes directory: {}", themeService.themesDirectory());
    }

    public BackendStatus status() {
        return new BackendStatus(started, VERSION, config.dataDirectory(), config.gameDirectory());
    }

    public void appendUiLog(String line) {
        latestLogService.append(line);
    }

    public void markUiFailure() {
        latestLogService.markFailure();
    }

    public JsonService json() {
        return json;
    }

    public List<ThemeInfo> listThemes() throws IOException {
        return themeService.listThemes();
    }

    public ThemeInfo activeTheme() throws IOException {
        return themeService.activeTheme();
    }

    public void selectTheme(String id) throws IOException {
        themeService.selectTheme(id);
    }

    public int maxMemoryMb() throws IOException {
        return settingsService.maxMemoryMb();
    }

    public void setMaxMemoryMb(int value) throws IOException {
        settingsService.setMaxMemoryMb(value);
    }

    public String jvmArguments() throws IOException {
        return settingsService.jvmArguments();
    }

    public void setJvmArguments(String arguments) throws IOException {
        settingsService.setJvmArguments(arguments);
    }

    public List<String> parseJvmArguments(String arguments) {
        return JvmArgumentsParser.parse(arguments);
    }

    public SystemMemoryInfo systemMemory() {
        return systemMemoryService.read();
    }

    public Optional<Path> savedGameDirectory() throws IOException {
        return settingsService.gameDirectory();
    }

    public void setGameDirectory(Path directory) throws IOException {
        settingsService.setGameDirectory(directory);
    }

    public VersionIsolationMode versionIsolationMode() throws IOException {
        return settingsService.versionIsolationMode();
    }

    public void setVersionIsolationMode(VersionIsolationMode mode) throws IOException {
        settingsService.setVersionIsolationMode(mode);
    }

    public boolean autoGameLanguage() throws IOException {
        autoGameLanguage = settingsService.autoGameLanguage();
        return autoGameLanguage;
    }

    public void setAutoGameLanguage(boolean enabled) throws IOException {
        settingsService.setAutoGameLanguage(enabled);
        autoGameLanguage = enabled;
    }

    public int downloadConcurrency() {
        return downloadConcurrency;
    }

    public void setDownloadConcurrency(int value) throws IOException {
        settingsService.setDownloadConcurrency(value);
        downloadConcurrency = value;
        versionInstaller.setDownloadConcurrency(value);
        externalLoaderInstaller.setDownloadConcurrency(value);
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

    public Optional<String> lastLaunchedInstanceId() throws IOException {
        return settingsService.lastLaunchedInstanceId();
    }

    public void setLastLaunchedInstanceId(String instanceId) throws IOException {
        settingsService.setLastLaunchedInstanceId(instanceId);
    }

    public MetadataService metadata() {
        return metadata;
    }

    public JavaLocator javaLocator() {
        return javaLocator;
    }

    public JavaSelectionMode javaSelectionMode() throws IOException {
        return settingsService.javaSelectionMode();
    }

    public Optional<Path> selectedJavaPath() throws IOException {
        return settingsService.selectedJavaPath();
    }

    public List<Path> manualJavaPaths() throws IOException {
        return settingsService.manualJavaPaths();
    }

    public List<JavaCandidate> listJavaCandidates() throws IOException {
        return javaLocator.discoverCandidates(
                settingsService.manualJavaPaths(),
                settingsService.selectedJavaPath().orElse(null)
        );
    }

    public JavaCandidate addManualJavaPath(Path executable) throws IOException {
        Objects.requireNonNull(executable, "executable");
        Path normalized = executable.toAbsolutePath().normalize();
        JavaRuntime runtime = javaLocator.inspect(normalized)
                .orElseThrow(() -> new IOException("不是有效的 Java 可执行文件：" + normalized));
        settingsService.addManualJavaPath(normalized);
        return JavaCandidate.from(runtime, true);
    }

    public void setJavaSelection(JavaSelectionMode mode, Path selectedJavaPath) throws IOException {
        Objects.requireNonNull(mode, "mode");
        Path normalized = selectedJavaPath == null
                ? null
                : selectedJavaPath.toAbsolutePath().normalize();
        settingsService.setJavaSelection(mode, normalized);
        gameLaunchService.setJavaSelection(mode, normalized);
    }

    public LanguageSelectionMode languageSelectionMode() throws IOException {
        return languageService.selectionMode();
    }

    public Optional<String> language() throws IOException {
        String locale = languageService.storedLocale();
        return locale == null || locale.isBlank() ? Optional.empty() : Optional.of(locale);
    }

    public LanguageSnapshot setLanguage(LanguageSelectionMode mode, String locale) throws IOException {
        return languageService.switchLanguage(mode, locale);
    }

    public LanguageFallbackMode languageFallbackMode() {
        return languageService.fallbackMode();
    }

    public String languageFallbackLocale() {
        return languageService.fallbackLocale();
    }

    public LanguageSnapshot setLanguageFallback(LanguageFallbackMode mode, String locale)
            throws IOException {
        return languageService.setLanguageFallback(mode, locale);
    }

    public LanguageService languageService() {
        return languageService;
    }

    public LanguageSnapshot languageSnapshot() {
        return languageService.snapshot();
    }

    public void addLanguageEventListener(LanguageEventListener listener) {
        languageService.addEventListener(listener);
    }

    public void removeLanguageEventListener(LanguageEventListener listener) {
        languageService.removeEventListener(listener);
    }

    public VersionManifestService versionManifestService() {
        return versionManifestService;
    }

    public VersionManifest fetchVersionManifest() throws java.io.IOException, InterruptedException {
        CachedVersionManifest cached = cachedVersionManifest;
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.manifest();
        }

        long generation = versionManifestGeneration.get();
        VersionManifest manifest = versionManifestService.fetchManifestOrCache(
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json")
        );
        if (versionManifestGeneration.get() == generation) {
            cachedVersionManifest = new CachedVersionManifest(
                    manifest, Instant.now().plus(VERSION_MANIFEST_CACHE_TTL));
        }
        return manifest;
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

    public ModSearchPage searchMods(ModSearchQuery query) throws IOException, InterruptedException {
        return modService.search(query);
    }

    public ModProjectDetail fetchModProject(ModSource source, String projectId)
            throws IOException, InterruptedException {
        return modService.project(source, projectId);
    }

    public ModDownloadResult downloadMod(
            ModFile file,
            Path gameDirectory,
            String instanceId,
            VersionIsolationMode isolationMode,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return modService.download(file, gameDirectory, instanceId, isolationMode, listener);
    }

    public boolean curseForgeConfigured() {
        return modService.curseForgeConfigured();
    }

    public byte[] loadModIcon(String iconUrl) throws IOException, InterruptedException {
        return modIconService.load(iconUrl);
    }

    public Path modsDirectory(Path gameDirectory, String instanceId,
                              VersionIsolationMode isolationMode) {
        return modService.modsDirectory(gameDirectory, instanceId, isolationMode);
    }

    public VersionInstaller versionInstaller() {
        return versionInstaller;
    }

    public InstalledVersion installVersion(String versionId, DownloadListener listener)
            throws java.io.IOException, InterruptedException {
        return installVersion(versionId, config.gameDirectory(), listener);
    }

    /**
     * 在指定游戏目录安装官方版本。
     */
    public InstalledVersion installVersion(
            String versionId,
            Path gameDirectory,
            DownloadListener listener
    ) throws java.io.IOException, InterruptedException {
        return installVersion(versionId, versionId, gameDirectory, listener);
    }

    /**
     * 使用指定实例名安装官方版本。
     */
    public InstalledVersion installVersion(
            String versionId,
            String instanceId,
            Path gameDirectory,
            DownloadListener listener
    ) throws java.io.IOException, InterruptedException {
        return installVersion(versionId, instanceId, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installVersion(
            String versionId,
            String instanceId,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws java.io.IOException, InterruptedException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        InstanceNameRules.ensureNewInstance(gameDirectory, instanceId);
        return versionInstaller.install(
                versionId,
                instanceId,
                gameDirectory,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json"),
                listener,
                token
        );
    }

    public InstanceNameCheck checkInstanceName(Path gameDirectory, String instanceId) {
        return InstanceNameRules.check(gameDirectory, instanceId);
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
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return installFabricVersion(gameVersion, instanceId, loaderVersion, apiVersion,
                gameDirectory, listener, new DownloadCancellationToken());
    }

    public InstalledVersion installFabricVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        return fabricInstaller.install(
                gameVersion,
                instanceId,
                loaderVersion,
                apiVersion,
                gameDirectory,
                listener,
                token
        );
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
            String gameVersion,
            String instanceId,
            ForgeVersion version,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return installForgeVersion(gameVersion, instanceId, version, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installForgeVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion version,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        return externalLoaderInstaller.install(
                "Forge",
                gameVersion,
                instanceId,
                version.installerUri(),
                gameDirectory,
                listener,
                token
        );
    }

    public InstalledVersion installNeoForgeVersion(
            String gameVersion,
            String instanceId,
            NeoForgeVersion version,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return installNeoForgeVersion(gameVersion, instanceId, version, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installNeoForgeVersion(
            String gameVersion,
            String instanceId,
            NeoForgeVersion version,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        return externalLoaderInstaller.install(
                "NeoForge",
                gameVersion,
                instanceId,
                version.installerUri(),
                gameDirectory,
                listener,
                token
        );
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
            String gameVersion,
            String instanceId,
            String loaderVersion,
            QuiltApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return installQuiltVersion(gameVersion, instanceId, loaderVersion, apiVersion,
                gameDirectory, listener, new DownloadCancellationToken());
    }

    public InstalledVersion installQuiltVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            QuiltApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        return quiltInstaller.install(
                gameVersion,
                instanceId,
                loaderVersion,
                apiVersion,
                gameDirectory,
                listener,
                token
        );
    }

    /**
     * 安装 Forge + OptiFine。OptiFine 只允许与 Forge 组合。
     */
    public InstalledVersion installForgeOptiFineVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return installForgeOptiFineVersion(gameVersion, instanceId, forgeVersion,
                optiFineVersion, gameDirectory, listener, new DownloadCancellationToken());
    }

    public InstalledVersion installForgeOptiFineVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(forgeVersion, "forgeVersion");
        Objects.requireNonNull(optiFineVersion, "optiFineVersion");
        return optiFineInstaller.install(
                gameVersion,
                instanceId,
                forgeVersion,
                optiFineVersion,
                gameDirectory,
                listener,
                token
        );
    }

    /**
     * 探测指定游戏目录，返回目录状态和已安装版本列表。
     *
     * <p>CAMLFront 不允许直接读取文件系统，因此“游戏位置”界面在用户选择目录后,
     * 通过该方法把磁盘探测交给后端完成。</p>
     *
     * @param directory 用户选择或使用默认值的游戏目录（通常是 {@code .minecraft}）
     * @return 规范化后的目录信息；目录不存在时 {@code directoryExists} 为 {@code false}
     */
    public GameDirectoryInfo inspectGameDirectory(Path directory) {
        Objects.requireNonNull(directory, "directory");
        Path normalized = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            return new GameDirectoryInfo(normalized, false, List.of(), List.of());
        }

        Path versionsDirectory = normalized.resolve("versions");
        if (!Files.isDirectory(versionsDirectory)) {
            return new GameDirectoryInfo(normalized, true, List.of(), List.of());
        }

        List<String> versions;
        List<String> isolated;
        try (Stream<Path> entries = Files.list(versionsDirectory)) {
            List<Path> installed = entries
                    .filter(Files::isDirectory)
                    // 只统计真正安装完成的版本：versions/<id>/<id>.json
                    .filter(entry -> Files.isRegularFile(
                            entry.resolve(entry.getFileName().toString() + ".json")))
                    .toList();
            versions = installed.stream()
                    .map(entry -> entry.getFileName().toString())
                    .sorted()
                    .toList();
            // 标记启用“版本隔离”的版本：游戏数据（saves、options.txt 等）位于版本目录内。
            isolated = installed.stream()
                    .filter(GameDataLayout::isIsolated)
                    .map(entry -> entry.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to inspect game directory: " + normalized, e);
        }
        return new GameDirectoryInfo(normalized, true, versions, isolated);
    }

    /** 读取实例数据目录和 mods 列表。 */
    public InstanceDetails inspectInstance(Path gameDirectory, String instanceId) throws IOException {
        String instance = InstanceNameRules.validate(instanceId);
        Set<String> disabled = settingsService.disabledMods(gameDirectory, instance);
        return InstanceInspector.inspect(gameDirectory, instance, disabled);
    }

    /** 使用系统文件管理器打开实例的实际数据目录。 */
    public void openInstanceFolder(Path gameDirectory, String instanceId) throws IOException {
        DirectoryOpener.open(InstanceInspector.dataDirectory(gameDirectory, instanceId));
    }

    /** 切换实例 Mod 的启用状态，并把状态写入 CAML 配置。 */
    public void setModDisabled(Path gameDirectory, String instanceId, String fileName,
                               boolean disabled) throws IOException {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        String instance = InstanceNameRules.validate(instanceId);
        String baseName = normalizeModFileName(fileName);
        Path modsDirectory = InstanceInspector.dataDirectory(normalizedGameDirectory, instance)
                .resolve("mods");
        Path enabledPath = SafePaths.resolveChild(modsDirectory, baseName, "mod file");
        Path disabledPath = SafePaths.resolveChild(modsDirectory, baseName + ".disabled",
                "disabled mod file");

        boolean moved = false;
        Path source = disabled ? enabledPath : disabledPath;
        Path target = disabled ? disabledPath : enabledPath;
        if (Files.isRegularFile(source)) {
            if (Files.exists(target)) {
                throw new IOException("目标 Mod 文件已存在: " + target);
            }
            move(source, target);
            moved = true;
        } else if (!Files.isRegularFile(target)) {
            throw new IOException("Mod 文件不存在: " + source);
        }

        try {
            settingsService.setModDisabled(normalizedGameDirectory, instance, baseName, disabled);
        } catch (IOException | RuntimeException e) {
            if (moved) {
                try {
                    move(target, source);
                } catch (IOException rollbackFailure) {
                    LOGGER.warn("回滚 Mod 文件状态失败: {}", target, rollbackFailure);
                }
            }
            throw e;
        }
    }

    /** 从本地 mod jar 读取图标；没有图标时返回空数组。 */
    public byte[] loadInstanceModIcon(Path gameDirectory, String instanceId, String fileName) {
        return InstanceModIconService.load(gameDirectory, instanceId, fileName);
    }

    /** 检查实例中每个 Mod 在当前 MC 版本/加载器下是否有更新。 */
    public List<InstanceModUpdate> checkInstanceModUpdates(
            Path gameDirectory, String instanceId) throws IOException, InterruptedException {
        InstanceDetails details = inspectInstance(gameDirectory, instanceId);
        InstanceEnvironment environment = InstanceEnvironmentResolver.resolve(
                gameDirectory, instanceId, metadata);
        if (environment.loader() == null
                || environment.gameVersion() == null || environment.gameVersion().isBlank()) {
            return details.mods().stream()
                    .map(mod -> new InstanceModUpdate(
                            mod.fileName(), displayVersion(mod.version()),
                            displayVersion(mod.version()), false, null, "unsupported"))
                    .toList();
        }

        Set<String> hashes = new LinkedHashSet<>();
        for (InstanceMod mod : details.mods()) {
            Path file = InstanceInspector.modFile(gameDirectory, instanceId, mod.fileName());
            if (Files.isRegularFile(file)) {
                hashes.add(HashVerifier.sha1(file));
            }
        }
        Map<String, ModFile> latestByHash = modService.latestVersions(
                new ArrayList<>(hashes),
                environment.gameVersion(),
                environment.loader());

        Map<Long, ModFile> latestByFingerprint = Map.of();
        if (modService.curseForgeConfigured()) {
            Map<Long, String> fingerprintToFile = new LinkedHashMap<>();
            for (InstanceMod mod : details.mods()) {
                String hash = null;
                Path file = InstanceInspector.modFile(gameDirectory, instanceId, mod.fileName());
                if (Files.isRegularFile(file)) {
                    hash = HashVerifier.sha1(file);
                }
                if (hash != null && latestByHash.containsKey(hash)) {
                    continue;
                }
                if (Files.isRegularFile(file)) {
                    fingerprintToFile.put(CurseForgeFingerprint.of(file), mod.fileName());
                }
            }
            if (!fingerprintToFile.isEmpty()) {
                try {
                    Map<Long, String> fingerprintToModId = modService.curseForgeFingerprintModIds(
                            new ArrayList<>(fingerprintToFile.keySet()));
                    latestByFingerprint = modService.latestCurseForgeVersions(
                            fingerprintToModId, environment.gameVersion(), environment.loader());
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("CurseForge 更新检查失败", e);
                }
            }
        }

        List<InstanceModUpdate> updates = new ArrayList<>();
        for (InstanceMod mod : details.mods()) {
            String currentVersion = displayVersion(mod.version());
            Path file = InstanceInspector.modFile(gameDirectory, instanceId, mod.fileName());
            String hash = Files.isRegularFile(file) ? HashVerifier.sha1(file) : "";
            ModFile latest = latestByHash.get(hash);
            if (latest == null && Files.isRegularFile(file)) {
                latest = latestByFingerprint.get(CurseForgeFingerprint.of(file));
            }
            boolean updateAvailable = latest != null && !sameVersion(
                    mod.version(), latest.versionNumber(), latest.sha1(), hash);
            String latestVersion = latest == null
                    ? currentVersion
                    : displayVersion(latest.versionNumber());
            updates.add(new InstanceModUpdate(
                    mod.fileName(), currentVersion, latestVersion,
                    updateAvailable, latest, null));
        }
        return List.copyOf(updates);
    }

    /** 下载最新 Mod 文件，替换旧文件并保留禁用状态。 */
    public ModDownloadResult updateInstanceMod(
            Path gameDirectory, String instanceId, String currentFileName,
            ModFile latestFile, DownloadListener listener)
            throws IOException, InterruptedException {
        Path normalizedGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath()
                .normalize();
        String instance = InstanceNameRules.validate(instanceId);
        String oldBase = normalizeModFileName(currentFileName);
        String newBase = normalizeModFileName(latestFile.fileName());
        Path modsDirectory = InstanceInspector.dataDirectory(normalizedGameDirectory, instance)
                .resolve("mods");
        Path oldEnabled = SafePaths.resolveChild(modsDirectory, oldBase, "mod file");
        Path oldDisabled = SafePaths.resolveChild(modsDirectory, oldBase + ".disabled",
                "disabled mod file");
        boolean wasDisabled = settingsService.disabledMods(normalizedGameDirectory, instance)
                .contains(oldBase) || Files.isRegularFile(oldDisabled);

        ModDownloadResult downloaded = modService.download(
                latestFile, normalizedGameDirectory, instance, VersionIsolationMode.AUTO, listener);

        if (!oldBase.equals(newBase)) {
            Files.deleteIfExists(oldEnabled);
            Files.deleteIfExists(oldDisabled);
        }
        Path newEnabled = SafePaths.resolveChild(modsDirectory, newBase, "mod file");
        Path newDisabled = SafePaths.resolveChild(modsDirectory, newBase + ".disabled",
                "disabled mod file");
        Path resultPath = downloaded.path();
        if (wasDisabled && Files.isRegularFile(newEnabled)) {
            Files.move(newEnabled, newDisabled, StandardCopyOption.REPLACE_EXISTING);
            resultPath = newDisabled;
        }

        settingsService.setModDisabled(normalizedGameDirectory, instance, newBase, wasDisabled);
        if (!oldBase.equals(newBase)) {
            settingsService.setModDisabled(normalizedGameDirectory, instance, oldBase, false);
        }
        return new ModDownloadResult(resultPath, latestFile);
    }

    private static String displayVersion(String version) {
        return version == null || version.isBlank() ? "?" : version;
    }

    private static boolean sameVersion(String current, String latest, String latestSha1, String hash) {
        if (latestSha1 != null && !latestSha1.isBlank()
                && latestSha1.equalsIgnoreCase(hash)) {
            return true;
        }
        if (current == null || current.isBlank()) {
            return false;
        }
        return latest != null && current.equalsIgnoreCase(latest);
    }

    private static String normalizeModFileName(String fileName) {
        String value = Objects.requireNonNull(fileName, "fileName").trim();
        if (value.endsWith(".disabled")) {
            value = value.substring(0, value.length() - ".disabled".length());
        }
        if (!value.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
            throw new IllegalArgumentException("不是有效的 Mod jar: " + fileName);
        }
        return value;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    public AuthSession loginOffline(String username) {
        return new OfflineAccount(username).login();
    }

    /** 判断目录是否存在，供前端保存“游戏位置”前做校验。 */
    public boolean directoryExists(Path directory) {
        return directory != null && Files.isDirectory(directory);
    }

    // ------------------------------------------------------------------ 账号

    /** 账号列表（不含 token）。 */
    public List<AccountSummary> listAccounts() throws IOException {
        return accountService.list();
    }

    /** 全局默认账号；没有设置时返回空。 */
    public Optional<AccountSummary> defaultAccount() throws IOException {
        String id = settingsService.defaultAccountId().orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        return accountService.find(id);
    }

    public void setDefaultAccountId(String accountId) throws IOException {
        accountService.setDefaultAccountId(accountId);
    }

    /** 使用用户名 + 密码登录第三方 Yggdrasil 认证服务器。 */
    public AccountSummary loginYggdrasil(String apiRoot, String username, String password,
                                         boolean remember)
            throws IOException, InterruptedException {
        return accountService.loginYggdrasil(apiRoot, username, password, remember);
    }

    /** 新增离线账号记录。 */
    public AccountSummary addOfflineAccount(String username) throws IOException {
        return accountService.addOfflineAccount(username);
    }

    public void removeAccount(String accountId) throws IOException {
        accountService.remove(accountId);
    }

    /** 拉取认证服务器 metadata（同时校验地址是否为有效 Yggdrasil 根）。 */
    public AuthServerInfo fetchAuthServer(String apiRoot) throws IOException, InterruptedException {
        return accountService.fetchServer(apiRoot);
    }

    /** 内置认证服务器地址列表。 */
    public List<String> authServerPresets() {
        return accountService.presetApiRoots();
    }

    /** 内置认证服务器地址 → 昵称。 */
    public java.util.Map<String, String> authServerPresetNames() {
        return accountService.presetServerNames();
    }

    /** 某个实例单独指定的账号；没有单独设置时返回空。 */
    public Optional<String> instanceAccountId(Path gameDirectory, String instanceId)
            throws IOException {
        return accountService.instanceAccountId(gameDirectory, instanceId);
    }

    public void setInstanceAccountId(Path gameDirectory, String instanceId, String accountId)
            throws IOException {
        accountService.setInstanceAccountId(gameDirectory, instanceId, accountId);
    }

    /**
     * 检测“实例账号绑定留在旧游戏位置”的情况。
     *
     * @param gameDirectory    当前游戏目录
     * @param currentInstances 当前目录里已安装的实例
     * @return 没有需要提示的情况时返回空
     */
    public Optional<InstanceAccountMismatch> staleInstanceAccounts(
            Path gameDirectory, List<String> currentInstances) throws IOException {
        return accountService.detectInstanceAccountMismatch(gameDirectory, currentInstances);
    }

    /** 把旧游戏位置下的实例账号绑定套用到当前目录，返回实际套用的数量。 */
    public int reapplyInstanceAccounts(Path gameDirectory, List<String> currentInstances)
            throws IOException {
        return accountService.reapplyInstanceAccounts(gameDirectory, currentInstances);
    }

    public Optional<Path> authlibInjectorPath() throws IOException {
        return settingsService.authlibInjectorPath();
    }

    public void setAuthlibInjectorPath(Path jar) throws IOException {
        settingsService.setAuthlibInjectorPath(jar);
        accountService.setInjectorOverride(jar);
    }

    /**
     * 把第三方账号切换到另一个角色。
     *
     * <p>账号 id 不变，只改变下次启动使用的 UUID 与角色名；切换失败（凭据过期）时
     * 返回结构化错误，前端据此提示重新登录。</p>
     */
    public AccountSummary switchAccountProfile(String accountId, String profileId)
            throws IOException, InterruptedException {
        return accountService.switchProfile(accountId, profileId);
    }

    public LaunchPlan createLaunchPlan(
            InstalledVersion instance,
            AuthSession auth,
            org.chs.camlauncher.backend.runtime.JavaRuntime java,
            LaunchOptions options
    ) {
        return launchPlanner.create(instance, auth, java, options);
    }

    public ClasspathBuilder classpathBuilder() {
        return classpathBuilder;
    }

    public JvmArgumentsBuilder jvmArgumentsBuilder() {
        return jvmArgumentsBuilder;
    }

    public GameArgumentsBuilder gameArgumentsBuilder() {
        return gameArgumentsBuilder;
    }

    public NativeExtractor nativeExtractor() {
        return nativeExtractor;
    }

    public VirtualAssetInstaller virtualAssetInstaller() {
        return virtualAssetInstaller;
    }

    public LaunchCommandBuilder launchCommandBuilder() {
        return launchCommandBuilder;
    }

    public ProcessLauncher processLauncher() {
        return processLauncher;
    }

    public GameLaunchService gameLaunchService() {
        return gameLaunchService;
    }

    /**
     * 使用后端配置中的默认游戏目录启动游戏。
     */
    public GameLaunch launchGame(
            String versionId,
            String username,
            LaunchOptions options,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launchGame(
                versionId,
                config.gameDirectory(),
                username,
                options,
                VersionIsolationMode.AUTO,
                downloadListener,
                processListener
        );
    }

    /**
     * 使用指定游戏目录启动游戏。
     *
     * <p>游戏目录由前端“游戏位置”界面选择，但目录创建、版本安装、Java 选择、
     * 进程启动等所有磁盘与进程操作仍然全部在后端完成。</p>
     */
    public GameLaunch launchGame(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launchGame(
                versionId,
                gameDirectory,
                username,
                options,
                VersionIsolationMode.AUTO,
                downloadListener,
                processListener
        );
    }

    /**
     * 使用指定游戏目录和版本隔离策略启动游戏。
     */
    public GameLaunch launchGame(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        gameLaunchService.configureGameLanguage(
                autoGameLanguage, languageService.activeLocale());
        return gameLaunchService.launch(
                versionId,
                gameDirectory,
                username,
                options,
                isolationMode,
                downloadListener,
                processListener
        );
    }

    /**
     * 使用已保存的账号启动游戏。
     *
     * <p>与 {@code launchGame(..., String username, ...)} 区分：这里的 {@code accountId}
     * 是账号索引中的 id，而不是离线用户名。</p>
     *
     * @param accountId 账号 id；为 {@code null} 时按“实例账号 → 全局账号”解析
     */
    public GameLaunch launchGameWithAccount(
            String versionId,
            Path gameDirectory,
            String accountId,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        gameLaunchService.configureGameLanguage(
                autoGameLanguage, languageService.activeLocale());
        AccountService.ResolvedAccount resolved = accountId == null
                ? accountService.resolve(gameDirectory, versionId)
                : accountService.resolveById(accountId);
        if (resolved == null) {
            throw new BackendException(BackendError.of("auth.error.noAccount"));
        }
        return gameLaunchService.launch(
                versionId,
                gameDirectory,
                resolved.session(),
                resolved.injector(),
                options,
                isolationMode,
                downloadListener,
                processListener
        );
    }

    @Override
    public void close() {
        if (started) {
            started = false;
            LOGGER.info("CAML backend stopped");
        }
        latestLogService.close();
    }

    private record CachedVersionManifest(VersionManifest manifest, Instant expiresAt) {
    }
}