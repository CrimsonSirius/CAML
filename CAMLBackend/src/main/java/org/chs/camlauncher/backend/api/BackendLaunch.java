package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AccountService;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.launch.GameLaunch;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.launch.LaunchCompatibilityService;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.launch.LaunchPlanner;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.runtime.JavaCompatibilityCheck;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.chs.camlauncher.backend.settings.SettingsService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** 启动前检查、启动计划和游戏进程入口。 */
public final class BackendLaunch {
    private static final Logger LOGGER = LogManager.getLogger(BackendLaunch.class);

    private final GameLaunchService gameLaunchService;
    private final AccountService accountService;
    private final VersionInstaller versionInstaller;
    private final LaunchCompatibilityService launchCompatibilityService;
    private final LaunchPlanner launchPlanner;
    private final SettingsService settingsService;
    private final BackendSettings settings;
    private final Path dataDirectory;

    BackendLaunch(
            GameLaunchService gameLaunchService,
            AccountService accountService,
            VersionInstaller versionInstaller,
            LaunchCompatibilityService launchCompatibilityService,
            LaunchPlanner launchPlanner,
            SettingsService settingsService,
            BackendSettings settings,
            Path dataDirectory
    ) {
        this.gameLaunchService = Objects.requireNonNull(gameLaunchService, "gameLaunchService");
        this.accountService = Objects.requireNonNull(accountService, "accountService");
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.launchCompatibilityService = Objects.requireNonNull(
                launchCompatibilityService, "launchCompatibilityService");
        this.launchPlanner = Objects.requireNonNull(launchPlanner, "launchPlanner");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    }

    public LaunchPlan createLaunchPlan(
            InstalledVersion instance,
            AuthSession auth,
            JavaRuntime java,
            LaunchOptions options
    ) {
        return launchPlanner.create(instance, auth, java, options);
    }

    public boolean needsXrandrWarning(String versionId, Path gameDirectory) {
        try {
            return launchCompatibilityService.needsXrandrWarning(gameDirectory, versionId);
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("无法完成 Linux 启动环境预检: {}", versionId, e);
            return false;
        }
    }

    public GameLaunch launchGame(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launchGame(versionId, gameDirectory, username, options,
                settingsService.versionIsolationMode(), downloadListener, processListener);
    }

    public GameLaunch launchGame(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launchGame(versionId, gameDirectory, username, options, isolationMode, false,
                downloadListener, processListener);
    }

    public GameLaunch launchGame(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            boolean allowIncompatibleJava,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        gameLaunchService.configureGameLanguage(
                settings.autoGameLanguageValue(), settings.languageService().activeLocale());
        return gameLaunchService.launch(
                versionId,
                gameDirectory,
                username,
                options,
                isolationMode,
                allowIncompatibleJava,
                downloadListener,
                processListener
        );
    }

    public GameLaunch launchGameWithAccount(
            String versionId,
            Path gameDirectory,
            String accountId,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launchGameWithAccount(versionId, gameDirectory, accountId, options, isolationMode,
                false, downloadListener, processListener);
    }

    public GameLaunch launchGameWithAccount(
            String versionId,
            Path gameDirectory,
            String accountId,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            boolean allowIncompatibleJava,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        gameLaunchService.configureGameLanguage(
                settings.autoGameLanguageValue(), settings.languageService().activeLocale());
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
                allowIncompatibleJava,
                downloadListener,
                processListener
        );
    }

    public JavaCompatibilityCheck checkJavaCompatibility(String versionId, Path gameDirectory)
            throws IOException, InterruptedException {
        InstalledVersion installed = versionInstaller.install(
                versionId,
                gameDirectory,
                VersionManifestService.DEFAULT_MANIFEST_URI,
                dataDirectory.resolve("cache").resolve("version_manifest_v2.json"),
                DownloadListener.NONE,
                new DownloadCancellationToken()
        );
        return gameLaunchService.inspectJava(installed);
    }
}
