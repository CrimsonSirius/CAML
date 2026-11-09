package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.GameDataLayout;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceNameCheck;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricService;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.forge.ForgeService;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeService;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineInstaller;
import org.chs.camlauncher.backend.loader.optifine.OptiFineService;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.launch.GameLaunch;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.launch.LaunchPlanner;
import org.chs.camlauncher.backend.launch.classpath.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.command.GameArgumentsBuilder;
import org.chs.camlauncher.backend.launch.command.JvmArgumentsBuilder;
import org.chs.camlauncher.backend.launch.command.LaunchCommandBuilder;
import org.chs.camlauncher.backend.launch.prepare.NativeExtractor;
import org.chs.camlauncher.backend.launch.prepare.VirtualAssetInstaller;
import org.chs.camlauncher.backend.launch.process.ProcessLauncher;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.backend.system.SystemMemoryService;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.backend.theme.ThemeService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
    private final FabricService fabricService;
    private final FabricInstaller fabricInstaller;
    private final ForgeService forgeService;
    private final NeoForgeService neoForgeService;
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
    private final SystemMemoryService systemMemoryService;
    private volatile int downloadConcurrency = DownloadConcurrency.DEFAULT;
    private volatile DownloadSourceMode downloadSourceMode = SettingsService.DEFAULT_DOWNLOAD_SOURCE;
    private volatile String customDownloadApi = "";
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
        this.systemMemoryService = new SystemMemoryService();
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
    }

    public void start() {
        if (started) {
            return;
        }

        try {
            BackendPaths.ensure(config);
            themeService.ensureDefaultTheme();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare CAML directories", e);
        }
        try {
            downloadConcurrency = settingsService.downloadConcurrency();
        } catch (IOException e) {
            LOGGER.warn("无法读取下载并发设置，使用默认值 {}", DownloadConcurrency.DEFAULT, e);
            downloadConcurrency = DownloadConcurrency.DEFAULT;
        }
        versionInstaller.setDownloadConcurrency(downloadConcurrency);
        try {
            downloadSourceMode = settingsService.downloadSourceMode();
            customDownloadApi = settingsService.customDownloadApi();
        } catch (IOException e) {
            LOGGER.warn("无法读取下载源设置，使用默认值 {}", SettingsService.DEFAULT_DOWNLOAD_SOURCE, e);
            downloadSourceMode = SettingsService.DEFAULT_DOWNLOAD_SOURCE;
            customDownloadApi = "";
        }
        applyDownloadSources();

        started = true;
        LOGGER.info("CAML backend {} started", VERSION);
        LOGGER.info("Data directory: {}", config.dataDirectory());
        LOGGER.info("Game directory: {}", config.gameDirectory());
        LOGGER.info("Themes directory: {}", themeService.themesDirectory());
    }

    public BackendStatus status() {
        return new BackendStatus(started, VERSION, config.dataDirectory(), config.gameDirectory());
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

    public int downloadConcurrency() {
        return downloadConcurrency;
    }

    public void setDownloadConcurrency(int value) throws IOException {
        settingsService.setDownloadConcurrency(value);
        downloadConcurrency = value;
        versionInstaller.setDownloadConcurrency(value);
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
        neoForgeService.setSources(sources);
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
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        InstanceNameRules.ensureNewInstance(gameDirectory, instanceId);
        return versionInstaller.install(
                versionId,
                instanceId,
                gameDirectory,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json"),
                listener
        );
    }

    public InstanceNameCheck checkInstanceName(Path gameDirectory, String instanceId) {
        return InstanceNameRules.check(gameDirectory, instanceId);
    }

    public List<FabricLoaderVersion> fetchFabricLoaderVersions(String gameVersion)
            throws IOException, InterruptedException {
        return fabricService.fetchLoaderVersions(gameVersion);
    }

    public List<FabricApiVersion> fetchFabricApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        return fabricService.fetchApiVersions(gameVersion);
    }

    public InstalledVersion installFabricVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return fabricInstaller.install(
                gameVersion,
                instanceId,
                loaderVersion,
                apiVersion,
                gameDirectory,
                listener
        );
    }

    public List<ForgeVersion> fetchForgeVersions(String gameVersion)
            throws IOException, InterruptedException {
        return forgeService.fetchVersions(gameVersion);
    }

    public List<NeoForgeVersion> fetchNeoForgeVersions(String gameVersion)
            throws IOException, InterruptedException {
        return neoForgeService.fetchVersions(gameVersion);
    }

    public InstalledVersion installForgeVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion version,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        return externalLoaderInstaller.install(
                "Forge",
                gameVersion,
                instanceId,
                version.installerUri(),
                gameDirectory,
                listener
        );
    }

    public InstalledVersion installNeoForgeVersion(
            String gameVersion,
            String instanceId,
            NeoForgeVersion version,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        return externalLoaderInstaller.install(
                "NeoForge",
                gameVersion,
                instanceId,
                version.installerUri(),
                gameDirectory,
                listener
        );
    }

    public List<OptiFineVersion> fetchOptiFineVersions(String gameVersion)
            throws IOException, InterruptedException {
        return optiFineService.fetchVersions(gameVersion);
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
        Objects.requireNonNull(forgeVersion, "forgeVersion");
        Objects.requireNonNull(optiFineVersion, "optiFineVersion");
        return optiFineInstaller.install(
                gameVersion,
                instanceId,
                forgeVersion,
                optiFineVersion,
                gameDirectory,
                listener
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

    public AuthSession loginOffline(String username) {
        return new OfflineAccount(username).login();
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

    @Override
    public void close() {
        if (started) {
            started = false;
            LOGGER.info("CAML backend stopped");
        }
    }
}
