package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AccountService;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjectorService;
import org.chs.camlauncher.backend.auth.store.AccountStore;
import org.chs.camlauncher.backend.auth.store.AuthServerStore;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerService;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;
import org.chs.camlauncher.backend.config.SingleInstanceLock;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.launch.LaunchCompatibilityService;
import org.chs.camlauncher.backend.launch.LaunchPlanner;
import org.chs.camlauncher.backend.launch.classpath.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.command.GameArgumentsBuilder;
import org.chs.camlauncher.backend.launch.command.JvmArgumentsBuilder;
import org.chs.camlauncher.backend.launch.command.LaunchCommandBuilder;
import org.chs.camlauncher.backend.launch.prepare.NativeExtractor;
import org.chs.camlauncher.backend.launch.prepare.VirtualAssetInstaller;
import org.chs.camlauncher.backend.launch.process.ProcessLauncher;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricService;
import org.chs.camlauncher.backend.loader.forge.ForgeService;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeService;
import org.chs.camlauncher.backend.loader.optifine.OptiFineInstaller;
import org.chs.camlauncher.backend.loader.optifine.OptiFineService;
import org.chs.camlauncher.backend.loader.quilt.QuiltInstaller;
import org.chs.camlauncher.backend.loader.quilt.QuiltService;
import org.chs.camlauncher.backend.log.LatestLogService;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.mod.service.ModIconService;
import org.chs.camlauncher.backend.mod.service.ModService;
import org.chs.camlauncher.backend.mod.source.CurseForgeClient;
import org.chs.camlauncher.backend.mod.source.ModrinthClient;
import org.chs.camlauncher.backend.modpack.ModpackInstaller;
import org.chs.camlauncher.backend.runtime.JavaDownloadService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.ManagedJavaRepository;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.backend.system.DeviceInfoService;
import org.chs.camlauncher.backend.system.ProjectDependency;
import org.chs.camlauncher.backend.system.ProjectDependencyService;
import org.chs.camlauncher.backend.system.SystemInfo;
import org.chs.camlauncher.backend.system.SystemInfoService;
import org.chs.camlauncher.backend.system.SystemMemoryService;
import org.chs.camlauncher.backend.theme.ThemeService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/** 后端组合根：创建领域服务，暴露稳定 facade，并管理生命周期。 */
public final class CAMLBackend implements AutoCloseable {
    public static final String VERSION = "1.0-SNAPSHOT";

    private static final Logger LOGGER = LogManager.getLogger(CAMLBackend.class);

    private final BackendConfig config;
    private final BackendSettings settings;
    private final BackendJava java;
    private final BackendAccounts accounts;
    private final BackendDownloads downloads;
    private final BackendInstances instances;
    private final BackendLaunch launch;
    private final LatestLogService latestLogService;
    private SingleInstanceLock instanceLock;
    private boolean started;

    public CAMLBackend(BackendConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        JsonService json = new JsonService();
        MetadataService metadata = new MetadataService();
        LaunchCompatibilityService launchCompatibilityService = new LaunchCompatibilityService(metadata);
        JavaLocator javaLocator = new JavaLocator();
        ManagedJavaRepository managedJavaRepository = new ManagedJavaRepository(
                config.dataDirectory(), javaLocator);
        VersionManifestService versionManifestService = new VersionManifestService();
        DownloadPlanner downloadPlanner = new DownloadPlanner();
        AssetDownloadPlanner assetDownloadPlanner = new AssetDownloadPlanner();
        FileDownloader fileDownloader = new FileDownloader();
        JavaDownloadService javaDownloadService = new JavaDownloadService(
                managedJavaRepository, fileDownloader);
        CurseForgeClient curseForgeClient = new CurseForgeClient();
        ModService modService = new ModService(
                new ModrinthClient(), curseForgeClient, fileDownloader);
        ModIconService modIconService = new ModIconService(config.dataDirectory());
        VersionInstaller versionInstaller = new VersionInstaller(
                metadata, versionManifestService, downloadPlanner,
                assetDownloadPlanner, fileDownloader);
        FabricService fabricService = new FabricService();
        FabricInstaller fabricInstaller = new FabricInstaller(
                versionInstaller, fabricService, fileDownloader,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                manifestCache());
        ForgeService forgeService = new ForgeService();
        NeoForgeService neoForgeService = new NeoForgeService();
        QuiltService quiltService = new QuiltService();
        QuiltInstaller quiltInstaller = new QuiltInstaller(
                versionInstaller, quiltService, fileDownloader,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                manifestCache());
        OptiFineService optiFineService = new OptiFineService();
        ExternalLoaderInstaller externalLoaderInstaller = new ExternalLoaderInstaller(
                versionInstaller, javaLocator, fileDownloader, metadata,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                manifestCache());
        ModpackInstaller modpackInstaller = new ModpackInstaller(
                versionInstaller, fabricInstaller, quiltInstaller, externalLoaderInstaller,
                fileDownloader, curseForgeClient,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                manifestCache());
        OptiFineInstaller optiFineInstaller = new OptiFineInstaller(
                versionInstaller, externalLoaderInstaller, javaLocator, fileDownloader,
                VersionManifestService.DEFAULT_MANIFEST_URI, manifestCache(), optiFineService);
        LaunchPlanner launchPlanner = new LaunchPlanner();
        ClasspathBuilder classpathBuilder = new ClasspathBuilder();
        JvmArgumentsBuilder jvmArgumentsBuilder = new JvmArgumentsBuilder();
        GameArgumentsBuilder gameArgumentsBuilder = new GameArgumentsBuilder();
        NativeExtractor nativeExtractor = new NativeExtractor();
        VirtualAssetInstaller virtualAssetInstaller = new VirtualAssetInstaller();
        LaunchCommandBuilder launchCommandBuilder = new LaunchCommandBuilder();
        ProcessLauncher processLauncher = new ProcessLauncher();
        ThemeService themeService = new ThemeService(config.dataDirectory());
        SettingsService settingsService = new SettingsService(config.dataDirectory());
        LanguageService languageService = new LanguageService(config.dataDirectory(), settingsService);
        SystemMemoryService systemMemoryService = new SystemMemoryService();
        SystemInfo systemInfo = new SystemInfoService().read();
        var deviceInfo = new DeviceInfoService().read();
        List<ProjectDependency> projectDependencies = new ProjectDependencyService().dependencies();
        latestLogService = new LatestLogService(
                config.dataDirectory().resolve("logs").resolve("latest.log"));
        GameLaunchService gameLaunchService = new GameLaunchService(
                VersionManifestService.DEFAULT_MANIFEST_URI, manifestCache(), versionInstaller,
                javaLocator, launchPlanner, classpathBuilder, nativeExtractor,
                virtualAssetInstaller, processLauncher);
        AccountStore accountStore = new AccountStore(config.dataDirectory(), json);
        AuthServerService authServerService = new AuthServerService(
                new AuthServerStore(config.dataDirectory(), json));
        AuthlibInjectorService authlibInjectorService = new AuthlibInjectorService(
                config.dataDirectory(), fileDownloader);
        AccountService accountService = new AccountService(
                accountStore, authServerService, authlibInjectorService, settingsService);

        settings = new BackendSettings(
                themeService, settingsService, languageService, systemMemoryService,
                systemInfo, deviceInfo, projectDependencies);
        java = new BackendJava(
                javaLocator, managedJavaRepository, javaDownloadService,
                settingsService, gameLaunchService);
        accounts = new BackendAccounts(
                accountService, authlibInjectorService, settingsService);
        downloads = new BackendDownloads(
                config.dataDirectory(), settingsService, versionManifestService,
                downloadPlanner, assetDownloadPlanner, modService, modpackInstaller,
                modIconService, versionInstaller, fabricService, fabricInstaller,
                forgeService, neoForgeService, quiltService, quiltInstaller,
                optiFineService, optiFineInstaller, externalLoaderInstaller);
        instances = new BackendInstances(settingsService, metadata, modService);
        launch = new BackendLaunch(
                gameLaunchService, accountService, versionInstaller,
                launchCompatibilityService, launchPlanner, settingsService,
                settings, config.dataDirectory());
    }

    public void start() {
        if (started) {
            return;
        }

        try {
            BackendPaths.ensure(config);
            instanceLock = SingleInstanceLock.tryAcquire(config.dataDirectory())
                    .orElseThrow(() -> new IOException("CAML 已在运行"));
            settings.ensureThemes();
            latestLogService.start();
            settings.initializeLanguage();
        } catch (IOException e) {
            closeInstanceLock();
            LOGGER.error("后端初始化失败，无法准备目录、主题、日志或语言系统", e);
            throw new UncheckedIOException("Failed to prepare CAML directories", e);
        }
        settings.initializeAutoGameLanguage();
        downloads.initialize();
        java.initialize();
        accounts.initialize();

        started = true;
        LOGGER.info("CAML backend {} started", VERSION);
        LOGGER.info("Data directory: {}", config.dataDirectory());
        LOGGER.info("Game directory: {}", config.gameDirectory());
        LOGGER.info("Themes directory: {}", settings.themesDirectory());
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

    public BackendSettings settings() {
        return settings;
    }

    public BackendJava java() {
        return java;
    }

    public BackendAccounts accounts() {
        return accounts;
    }

    public BackendDownloads downloads() {
        return downloads;
    }

    public BackendInstances instances() {
        return instances;
    }

    public BackendLaunch launch() {
        return launch;
    }

    @Override
    public void close() {
        if (started) {
            started = false;
            LOGGER.info("CAML backend stopped");
        }
        latestLogService.close();
        closeInstanceLock();
    }

    private java.nio.file.Path manifestCache() {
        return config.dataDirectory().resolve("cache").resolve("version_manifest_v2.json");
    }

    private void closeInstanceLock() {
        if (instanceLock == null) {
            return;
        }
        try {
            instanceLock.close();
        } catch (IOException e) {
            LOGGER.warn("释放单实例锁失败", e);
        } finally {
            instanceLock = null;
        }
    }
}
