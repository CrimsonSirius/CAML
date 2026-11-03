package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.InstalledVersion;
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
    private final Path gameDirectory;
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
        this.gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory");
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

    public GameLaunch launch(
            String versionId,
            String username,
            LaunchOptions options,
            DownloadListener downloadListener,
            ProcessListener processListener
    ) throws IOException, InterruptedException {
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

        LaunchPlan plan = launchPlanner.create(installed, auth, java, options);
        plan = virtualAssetInstaller.prepare(plan);
        nativeExtractor.extract(plan);
        List<Path> classpath = classpathBuilder.build(plan);
        GameProcess process = processLauncher.launch(plan, classpath, processListener);

        return new GameLaunch(installed, auth, java, plan, classpath, process);
    }
}
