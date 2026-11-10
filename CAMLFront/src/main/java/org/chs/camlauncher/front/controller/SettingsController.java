package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.runtime.JavaCandidate;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 设置页控制器：持久化设置、游戏位置检查和系统内存监视。 */
public final class SettingsController {
    private final ControllerContext context;
    private final ScheduledExecutorService memoryExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "caml-memory-monitor");
                thread.setDaemon(true);
                return thread;
            });

    private volatile Consumer<SystemMemoryInfo> onSystemMemoryUpdated;
    private volatile Runnable onDownloadSourceSaved;
    private ScheduledFuture<?> memoryMonitorFuture;

    SettingsController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public Path defaultGameDirectory() {
        return context.backend().status().gameDirectory();
    }

    public int downloadConcurrency() {
        return context.backend().downloadConcurrency();
    }

    public DownloadSourceMode downloadSourceMode() {
        return context.backend().downloadSourceMode();
    }

    public String customDownloadApi() {
        return context.backend().customDownloadApi();
    }

    public FrontTask<List<JavaCandidate>> scanJavaCandidates(
            Consumer<List<JavaCandidate>> onLoaded, Consumer<String> onError) {
        return context.runJava("扫描 Java",
                () -> context.backend().listJavaCandidates(), onLoaded, onError);
    }

    public FrontTask<JavaCandidate> addManualJava(
            Path executable, Consumer<JavaCandidate> onAdded, Consumer<String> onError) {
        return context.runJava("添加手动 Java",
                () -> context.backend().addManualJavaPath(executable), onAdded, onError);
    }

    public void saveJavaSelectionAsync(JavaSelectionMode mode, Path selectedJavaPath) {
        context.executeSettings("保存 Java 选择",
                () -> context.backend().setJavaSelection(mode, selectedJavaPath));
    }

    public FrontTask<GameDirectoryInfo> inspectGameDirectory(
            Path directory, Consumer<GameDirectoryInfo> callback) {
        return context.inspectGameDirectory(directory, callback);
    }

    public FrontTask<GameDirectoryInfo> inspectGameDirectory(
            Path directory, Consumer<GameDirectoryInfo> callback, Consumer<String> onError) {
        return context.inspectGameDirectory(directory, callback, onError);
    }

    /** 异步保存最大内存，避免在 JavaFX Application Thread 上写文件。 */
    public void saveMaxMemoryMbAsync(int maxMemoryMb) {
        context.executeSettings("保存最大内存设置",
                () -> context.backend().setMaxMemoryMb(maxMemoryMb));
    }

    /** 异步保存游戏位置，避免在 JavaFX Application Thread 上写文件。 */
    public void saveGameDirectoryAsync(Path gameDirectory) {
        context.executeSettings("保存游戏位置",
                () -> context.backend().setGameDirectory(gameDirectory));
    }

    /** 异步保存版本隔离选择，避免在 JavaFX Application Thread 上写文件。 */
    public void saveVersionIsolationAsync(boolean fullVersionIsolation) {
        context.executeSettings("保存版本隔离设置",
                () -> context.backend().setVersionIsolationMode(fullVersionIsolation
                        ? VersionIsolationMode.FULL
                        : VersionIsolationMode.NONE));
    }

    /** 异步保存下载并发数，并立即更新后端下载器。 */
    public void saveDownloadConcurrencyAsync(int downloadConcurrency) {
        context.executeSettings("保存下载并发数",
                () -> context.backend().setDownloadConcurrency(downloadConcurrency));
    }

    public void saveDownloadSourceAsync(DownloadSourceMode mode, String customApi) {
        context.executeSettings("保存下载源设置", () -> {
            context.backend().setDownloadSource(mode, customApi);
            Runnable listener = onDownloadSourceSaved;
            if (listener != null) {
                Platform.runLater(listener);
            }
        });
    }

    /** 异步保存主题选择，确保关闭启动器时也能等待写入完成。 */
    public void saveThemeAsync(String themeId) {
        Objects.requireNonNull(themeId, "themeId");
        context.executeSettings("保存主题选择",
                () -> context.backend().selectTheme(themeId));
    }

    public void setOnDownloadSourceSaved(Runnable listener) {
        onDownloadSourceSaved = listener;
    }

    /** 每 500ms 读取一次系统内存，并立即在 JavaFX 线程刷新监听者。 */
    public void setOnSystemMemoryUpdated(Consumer<SystemMemoryInfo> listener) {
        onSystemMemoryUpdated = listener;
    }

    public synchronized void startMemoryMonitor() {
        if (memoryMonitorFuture != null && !memoryMonitorFuture.isCancelled()) {
            return;
        }
        memoryMonitorFuture = memoryExecutor.scheduleAtFixedRate(() -> {
            SystemMemoryInfo memory = context.backend().systemMemory();
            Consumer<SystemMemoryInfo> current = onSystemMemoryUpdated;
            if (current != null) {
                Platform.runLater(() -> current.accept(memory));
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public synchronized void stopMemoryMonitor() {
        if (memoryMonitorFuture != null) {
            memoryMonitorFuture.cancel(false);
            memoryMonitorFuture = null;
        }
    }

    void shutdown() {
        stopMemoryMonitor();
        memoryExecutor.shutdownNow();
    }
}
