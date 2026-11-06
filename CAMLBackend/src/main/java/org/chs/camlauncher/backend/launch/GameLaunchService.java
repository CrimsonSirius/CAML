package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.GameDataLayout;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class GameLaunchService {
    /** 未显式指定游戏目录时使用的默认目录。 */
    private final Path defaultGameDirectory;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private final VersionInstaller versionInstaller;
    private final JavaLocator javaLocator;
    private final LaunchPlanner launchPlanner;
    private final ClasspathBuilder classpathBuilder;
    private final NativeExtractor nativeExtractor;
    private final VirtualAssetInstaller virtualAssetInstaller;
    private final ProcessLauncher processLauncher;

    public GameLaunchService(
            Path gameDirectory,
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
        this.defaultGameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory");
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

    /**
     * 使用默认游戏目录启动游戏。
     */
    public GameLaunch launch(
            String versionId,
            String username,
            LaunchOptions options,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
        return launch(
                versionId,
                defaultGameDirectory,
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
                VersionIsolationMode.AUTO,
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
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(isolationMode, "isolationMode");
        InstalledVersion installed = versionInstaller.install(
                versionId,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                downloadListener
        );
        AuthSession auth = new OfflineAccount(username).login();

        JavaRequirement requirement = JavaRequirement.from(installed.manifest());
        JavaRuntime java = javaLocator.findSuitable(requirement).orElseThrow(() ->
                new LaunchException(
                        LaunchFailure.EXECUTABLE_NOT_FOUND,
                        "找不到满足版本要求的 Java " + requirement.majorVersion() + "，请安装或选择 Java。",
                        null
                ));

        LaunchPlan plan = launchPlanner.create(
                installed,
                auth,
                java,
                options,
                GameDataLayout.resolve(
                        installed.gameDirectory(),
                        installed.versionDirectory(),
                        isolationMode
                )
        );

        plan = virtualAssetInstaller.prepare(plan);
        nativeExtractor.extract(plan);
        List<Path> classpath = classpathBuilder.build(plan);
        GameProcess process = processLauncher.launch(plan, classpath, processListener);

        return new GameLaunch(installed, auth, java, plan, classpath, process);
    }
}
