package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.runtime.JavaCandidate;
import org.chs.camlauncher.backend.runtime.JavaDistributionOption;
import org.chs.camlauncher.backend.runtime.JavaDownloadOption;
import org.chs.camlauncher.backend.runtime.JavaDownloadService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaPackageType;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.backend.runtime.ManagedJavaRepository;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.backend.system.DirectoryOpener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Java 运行时发现、安装和选择入口。 */
public final class BackendJava {
    private static final Logger LOGGER = LogManager.getLogger(BackendJava.class);

    private final JavaLocator javaLocator;
    private final ManagedJavaRepository managedJavaRepository;
    private final JavaDownloadService javaDownloadService;
    private final SettingsService settingsService;
    private final GameLaunchService gameLaunchService;

    BackendJava(
            JavaLocator javaLocator,
            ManagedJavaRepository managedJavaRepository,
            JavaDownloadService javaDownloadService,
            SettingsService settingsService,
            GameLaunchService gameLaunchService
    ) {
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
        this.managedJavaRepository = Objects.requireNonNull(
                managedJavaRepository, "managedJavaRepository");
        this.javaDownloadService = Objects.requireNonNull(
                javaDownloadService, "javaDownloadService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.gameLaunchService = Objects.requireNonNull(gameLaunchService, "gameLaunchService");
    }

    void initialize() {
        try {
            gameLaunchService.setJavaSelection(
                    settingsService.javaSelectionMode(),
                    settingsService.selectedJavaPath().orElse(null)
            );
        } catch (IOException e) {
            LOGGER.warn("无法读取 Java 选择设置，使用自动选择", e);
            gameLaunchService.setJavaSelection(JavaSelectionMode.AUTO, null);
        }
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
        Map<String, JavaCandidate> candidates = new LinkedHashMap<>();
        for (JavaCandidate candidate : javaLocator.discoverCandidates(
                settingsService.manualJavaPaths(),
                settingsService.selectedJavaPath().orElse(null)
        )) {
            candidates.put(candidate.executable().toAbsolutePath().normalize().toString(), candidate);
        }
        for (JavaCandidate candidate : managedJavaRepository.listCandidates()) {
            candidates.putIfAbsent(
                    candidate.executable().toAbsolutePath().normalize().toString(), candidate);
        }
        List<JavaCandidate> result = new ArrayList<>(candidates.values());
        result.sort(Comparator
                .comparing(JavaCandidate::available).reversed()
                .thenComparing(Comparator.comparingInt(JavaCandidate::majorVersion).reversed())
                .thenComparing(candidate -> candidate.executable().toString()));
        return List.copyOf(result);
    }

    public List<JavaDistributionOption> javaDownloadDistributions() {
        return javaDownloadService.distributions();
    }

    public List<JavaDownloadOption> listJavaDownloadOptions(
            String distributionId, JavaPackageType packageType)
            throws IOException, InterruptedException {
        return javaDownloadService.listOptions(distributionId, packageType);
    }

    public JavaCandidate downloadAndInstallJava(
            JavaDownloadOption option, String name,
            DownloadListener listener, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        return javaDownloadService.downloadAndInstall(option, name, listener, token);
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

    public void openJavaFolder(Path executable) throws IOException {
        Path normalized = Objects.requireNonNull(executable, "executable")
                .toAbsolutePath()
                .normalize();
        Path folder = normalized.getParent();
        if (folder == null) {
            throw new IOException("无法确定 Java 所在目录: " + normalized);
        }
        DirectoryOpener.open(folder);
    }
}
