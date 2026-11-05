package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;
import org.chs.camlauncher.backend.download.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.GameDataLayout;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.launch.LaunchPlanner;
import org.chs.camlauncher.backend.launch.LaunchCommandBuilder;
import org.chs.camlauncher.backend.launch.ProcessLauncher;
import org.chs.camlauncher.backend.launch.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.GameArgumentsBuilder;
import org.chs.camlauncher.backend.launch.GameLaunch;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.launch.JvmArgumentsBuilder;
import org.chs.camlauncher.backend.launch.NativeExtractor;
import org.chs.camlauncher.backend.launch.ProcessListener;
import org.chs.camlauncher.backend.launch.VirtualAssetInstaller;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifest;
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
        return gameLaunchService.launch(
                versionId,
                gameDirectory,
                username,
                options,
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
