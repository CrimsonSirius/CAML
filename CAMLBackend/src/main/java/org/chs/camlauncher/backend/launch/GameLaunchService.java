package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.api.JavaCompatibilityCheck;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjector;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.GameDataLayout;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.launch.classpath.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.patch.NeoForge262VulkanFix;
import org.chs.camlauncher.backend.launch.prepare.NativeExtractor;
import org.chs.camlauncher.backend.launch.prepare.VirtualAssetInstaller;
import org.chs.camlauncher.backend.launch.process.GameProcess;
import org.chs.camlauncher.backend.launch.process.ProcessLauncher;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.backend.modpack.ModpackLayout;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.language.contract.LanguageConstants;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GameLaunchService {
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private final VersionInstaller versionInstaller;
    private final JavaLocator javaLocator;
    private final LaunchPlanner launchPlanner;
    private final ClasspathBuilder classpathBuilder;
    private final NativeExtractor nativeExtractor;
    private final VirtualAssetInstaller virtualAssetInstaller;
    private final ProcessLauncher processLauncher;
    private volatile JavaSelectionMode javaSelectionMode = JavaSelectionMode.AUTO;
    private volatile Path selectedJavaPath;
    private volatile boolean autoGameLanguage;
    private volatile String launcherLocale = LanguageConstants.DEFAULT_LOCALE;

    public GameLaunchService(
            URI manifestUri,
            Path manifestCacheFile,
            VersionInstaller versionInstaller,
            JavaLocator javaLocator,
            LaunchPlanner launchPlanner,
            ClasspathBuilder classpathBuilder,
            NativeExtractor nativeExtractor,
            VirtualAssetInstaller virtualAssetInstaller,
            ProcessLauncher processLauncher
    ) {
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
        this.launchPlanner = Objects.requireNonNull(launchPlanner, "launchPlanner");
        this.classpathBuilder = Objects.requireNonNull(classpathBuilder, "classpathBuilder");
        this.nativeExtractor = Objects.requireNonNull(nativeExtractor, "nativeExtractor");
        this.virtualAssetInstaller = Objects.requireNonNull(virtualAssetInstaller, "virtualAssetInstaller");
        this.processLauncher = Objects.requireNonNull(processLauncher, "processLauncher");
    }

    public void setJavaSelection(JavaSelectionMode mode, Path selectedJavaPath) {
        this.javaSelectionMode = Objects.requireNonNull(mode, "mode");
        this.selectedJavaPath = selectedJavaPath == null
                ? null
                : selectedJavaPath.toAbsolutePath().normalize();
    }

    public void configureGameLanguage(boolean autoGameLanguage, String launcherLocale) {
        this.autoGameLanguage = autoGameLanguage;
        this.launcherLocale = launcherLocale == null || launcherLocale.isBlank()
                ? LanguageConstants.DEFAULT_LOCALE
                : launcherLocale;
    }

    /**
     * 使用指定游戏目录启动游戏。
     *
     * <p>目录由前端选择，但安装、登录、Java 选择、natives 解压和进程启动都在后端完成。</p>
     */
    public GameLaunch launch(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launch(
                versionId,
                gameDirectory,
                username,
                options,
                VersionIsolationMode.FULL,
                downloadListener,
                processListener
        );
    }

    /**
     * 使用指定游戏目录和隔离策略启动游戏。
     */
    public GameLaunch launch(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launch(versionId, gameDirectory, new OfflineAccount(username).login(),
                Optional.empty(), options, isolationMode, false,
                downloadListener, processListener);
    }

    public GameLaunch launch(
            String versionId,
            Path gameDirectory,
            String username,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            boolean allowIncompatibleJava,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launch(versionId, gameDirectory, new OfflineAccount(username).login(),
                Optional.empty(), options, isolationMode, allowIncompatibleJava,
                downloadListener, processListener);
    }

    /**
     * 使用已经解析好的账号启动游戏。
     *
     * <p>{@code injector} 非空时会在 JVM 参数中注入 authlib-injector，
     * 登录本身由调用方（{@code AccountService}）完成；这里只负责启动编排。</p>
     */
    public GameLaunch launch(
            String versionId,
            Path gameDirectory,
            AuthSession auth,
            Optional<AuthlibInjector> injector,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launch(versionId, gameDirectory, auth, injector, options, isolationMode, false,
                downloadListener, processListener);
    }

    public GameLaunch launch(
            String versionId,
            Path gameDirectory,
            AuthSession auth,
            Optional<AuthlibInjector> injector,
            LaunchOptions options,
            VersionIsolationMode isolationMode,
            boolean allowIncompatibleJava,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(isolationMode, "isolationMode");
        InstalledVersion installed = versionInstaller.install(
                versionId,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                downloadListener
        );

        JavaRuntime java = selectJava(installed, allowIncompatibleJava);

        VersionIsolationMode effectiveIsolation = ModpackLayout.isModpack(installed.versionDirectory())
                ? VersionIsolationMode.FULL
                : isolationMode;
        Path gameDataDirectory = GameDataLayout.resolve(
                installed.gameDirectory(),
                installed.versionDirectory(),
                effectiveIsolation
        );
        if (autoGameLanguage) {
            GameLanguageInitializer.apply(gameDataDirectory, launcherLocale);
        }
        NeoForge262VulkanFix.apply(installed.manifest(), gameDataDirectory);

        LaunchPlan plan = launchPlanner.create(
                installed,
                auth,
                java,
                options,
                gameDataDirectory,
                allowIncompatibleJava
        ).withInjector(injector);

        plan = virtualAssetInstaller.prepare(plan);
        nativeExtractor.extract(plan);
        List<Path> classpath = classpathBuilder.build(plan);
        GameProcess process = processLauncher.launch(plan, classpath, processListener);

        return new GameLaunch(installed, auth, java, plan, classpath, process);
    }

    public JavaCompatibilityCheck inspectJava(InstalledVersion installed) throws LaunchException {
        JavaRequirement requirement = JavaRequirement.from(installed.manifest());
        JavaRuntime runtime;
        if (javaSelectionMode == JavaSelectionMode.MANUAL) {
            Path path = selectedJavaPath;
            if (path == null) {
                throw new LaunchException(
                        LaunchFailure.EXECUTABLE_NOT_FOUND,
                        BackendError.of("launch.manualJavaNotConfigured"),
                        null
                );
            }
            runtime = javaLocator.inspect(path).orElseThrow(() -> new LaunchException(
                    LaunchFailure.EXECUTABLE_NOT_FOUND,
                    BackendError.of("launch.manualJavaUnavailable", path),
                    null
            ));
        } else {
            runtime = javaLocator.findSuitable(requirement)
                    .or(() -> javaLocator.findClosest(requirement))
                    .orElseThrow(() -> javaRequirementNotFound(requirement));
        }
        return new JavaCompatibilityCheck(
                requirement.majorVersion(),
                runtime.majorVersion(),
                runtime.executable(),
                requirement.isSatisfiedBy(runtime)
        );
    }

    private JavaRuntime selectJava(InstalledVersion installed, boolean allowIncompatibleJava)
            throws LaunchException {
        JavaRequirement requirement = JavaRequirement.from(installed.manifest());
        return selectedOrClosestJava(requirement, allowIncompatibleJava);
    }

    private JavaRuntime selectedOrClosestJava(JavaRequirement requirement,
                                              boolean allowIncompatibleJava)
            throws LaunchException {
        if (javaSelectionMode == JavaSelectionMode.MANUAL) {
            Path path = selectedJavaPath;
            if (path == null) {
                throw new LaunchException(
                        LaunchFailure.EXECUTABLE_NOT_FOUND,
                        BackendError.of("launch.manualJavaNotConfigured"),
                        null
                );
            }
            JavaRuntime runtime = javaLocator.inspect(path).orElseThrow(() -> new LaunchException(
                    LaunchFailure.EXECUTABLE_NOT_FOUND,
                    BackendError.of("launch.manualJavaUnavailable", path),
                    null
            ));
            if (!allowIncompatibleJava && !requirement.isSatisfiedBy(runtime)) {
                throw javaRequirementNotFound(requirement);
            }
            return runtime;
        }

        Optional<JavaRuntime> exact = javaLocator.findSuitable(requirement);
        if (exact.isPresent()) {
            return exact.get();
        }
        if (allowIncompatibleJava) {
            return javaLocator.findClosest(requirement)
                    .orElseThrow(() -> javaRequirementNotFound(requirement));
        }
        throw javaRequirementNotFound(requirement);
    }

    private static LaunchException javaRequirementNotFound(JavaRequirement requirement) {
        return new LaunchException(
                LaunchFailure.EXECUTABLE_NOT_FOUND,
                BackendError.of("launch.javaRequirementNotFound", requirement.majorVersion()),
                null
        );
    }
}
