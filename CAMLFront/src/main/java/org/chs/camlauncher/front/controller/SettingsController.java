package org.chs.camlauncher.front.controller;

import org.chs.camlauncher.front.task.AsyncTaskHandle;

import javafx.application.Platform;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.api.WindowState;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.language.LanguageSnapshot;
import org.chs.camlauncher.backend.runtime.JavaCandidate;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.backend.system.DeviceInfo;
import org.chs.camlauncher.backend.system.SystemInfo;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.front.text.UiText;

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
    private volatile Consumer<String> onSaveFailed;
    private volatile Runnable onSaveSucceeded;
    private volatile String failedSaveOperation;
    private ScheduledFuture<?> memoryMonitorFuture;

    SettingsController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public Path defaultGameDirectory() {
        return context.backend().status().gameDirectory();
    }

    public BackendStatus backendStatus() {
        return context.backend().status();
    }

    public SystemInfo systemInfo() {
        return context.backend().systemInfo();
    }

    public DeviceInfo deviceInfo() {
        return context.backend().deviceInfo();
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

    public AsyncTaskHandle<List<JavaCandidate>> scanJavaCandidates(
            Consumer<List<JavaCandidate>> onLoaded, Consumer<String> onError) {
        return context.runJava(UiText.tr("operation.scanJava"),
                () -> context.backend().listJavaCandidates(), onLoaded, onError);
    }

    public AsyncTaskHandle<JavaCandidate> addManualJava(
            Path executable, Consumer<JavaCandidate> onAdded, Consumer<String> onError) {
        return context.runJava(UiText.tr("operation.addManualJava"),
                () -> context.backend().addManualJavaPath(executable), onAdded, onError);
    }

    public void saveJavaSelectionAsync(JavaSelectionMode mode, Path selectedJavaPath) {
        save(UiText.tr("operation.saveJavaSelection"),
                () -> context.backend().setJavaSelection(mode, selectedJavaPath));
    }

    public AsyncTaskHandle<GameDirectoryInfo> inspectGameDirectory(
            Path directory, Consumer<GameDirectoryInfo> callback) {
        return context.inspectGameDirectory(directory, callback);
    }

    public AsyncTaskHandle<GameDirectoryInfo> inspectGameDirectory(
            Path directory, Consumer<GameDirectoryInfo> callback, Consumer<String> onError) {
        return context.inspectGameDirectory(directory, callback, onError);
    }

    public AsyncTaskHandle<Boolean> checkDirectory(
            Path directory, Consumer<Boolean> callback, Consumer<String> onError) {
        return context.checkDirectory(directory, callback, onError);
    }

    /** 异步保存最大内存，避免在 JavaFX Application Thread 上写文件。 */
    public void saveMaxMemoryMbAsync(int maxMemoryMb) {
        save(UiText.tr("operation.saveMaxMemory"),
                () -> context.backend().setMaxMemoryMb(maxMemoryMb));
    }

    public AsyncTaskHandle<String> saveJvmArgumentsAsync(
            String arguments, Consumer<String> onSaved, Consumer<String> onError) {
        String normalized = arguments == null ? "" : arguments.trim();
        return context.runSettings(UiText.tr("operation.saveJvmArguments"), () -> {
            context.backend().setJvmArguments(normalized);
            return normalized;
        }, onSaved, onError);
    }

    /** 异步保存游戏位置，避免在 JavaFX Application Thread 上写文件。 */
    public void saveGameDirectoryAsync(Path gameDirectory) {
        saveGameDirectoryAsync(gameDirectory, null);
    }

    public void saveGameDirectoryAsync(Path gameDirectory, Runnable onSaved) {
        save(UiText.tr("operation.saveGameDirectory"),
                () -> context.backend().setGameDirectory(gameDirectory), onSaved);
    }

    public void saveWindowStateAsync(WindowState state) {
        Objects.requireNonNull(state, "state");
        context.executeSettings(UiText.tr("operation.saveWindowState"),
                () -> context.backend().saveWindowState(state));
    }

    /** 异步保存版本隔离选择，避免在 JavaFX Application Thread 上写文件。 */
    public void saveVersionIsolationAsync(boolean fullVersionIsolation) {
        save(UiText.tr("operation.saveVersionIsolation"),
                () -> context.backend().setVersionIsolationMode(fullVersionIsolation
                        ? VersionIsolationMode.FULL
                        : VersionIsolationMode.NONE));
    }

    public void saveAutoGameLanguageAsync(boolean enabled) {
        save(UiText.tr("operation.saveAutoGameLanguage"),
                () -> context.backend().setAutoGameLanguage(enabled));
    }

    /** 异步保存下载并发数，并立即更新后端下载器。 */
    public void saveDownloadConcurrencyAsync(int downloadConcurrency) {
        save(UiText.tr("operation.saveDownloadConcurrency"),
                () -> context.backend().setDownloadConcurrency(downloadConcurrency));
    }

    public void saveDownloadSourceAsync(DownloadSourceMode mode, String customApi) {
        save(UiText.tr("operation.saveDownloadSource"),
                () -> context.backend().setDownloadSource(mode, customApi),
                () -> {
                    Runnable listener = onDownloadSourceSaved;
                    if (listener != null) {
                        listener.run();
                    }
                });
    }

    /** 异步保存主题选择，确保关闭启动器时也能等待写入完成。 */
    public void saveThemeAsync(String themeId) {
        Objects.requireNonNull(themeId, "themeId");
        save(UiText.tr("operation.saveTheme"),
                () -> context.backend().selectTheme(themeId));
    }

    /** 在设置线程完成语言持久化与重载，成功后把快照切回 JavaFX 线程。 */
    public AsyncTaskHandle<LanguageSnapshot> switchLanguageAsync(
            LanguageSelectionMode mode, String locale,
            Consumer<LanguageSnapshot> onLoaded, Consumer<String> onError) {
        String operation = UiText.tr("operation.saveLanguage");
        return context.runSettings(operation,
                () -> context.backend().setLanguage(mode, locale),
                snapshot -> {
                    notifySaveSucceeded(operation);
                    if (onLoaded != null) {
                        onLoaded.accept(snapshot);
                    }
                },
                message -> {
                    notifySaveFailed(operation, message);
                    if (onError != null) {
                        onError.accept(message);
                    }
                });
    }

    public AsyncTaskHandle<LanguageSnapshot> switchLanguageFallbackAsync(
            LanguageFallbackMode mode, String locale,
            Consumer<LanguageSnapshot> onLoaded, Consumer<String> onError) {
        String operation = UiText.tr("operation.saveLanguageFallback");
        return context.runSettings(operation,
                () -> context.backend().setLanguageFallback(mode, locale),
                snapshot -> {
                    notifySaveSucceeded(operation);
                    if (onLoaded != null) {
                        onLoaded.accept(snapshot);
                    }
                },
                message -> {
                    notifySaveFailed(operation, message);
                    if (onError != null) {
                        onError.accept(message);
                    }
                });
    }

    public void setOnDownloadSourceSaved(Runnable listener) {
        onDownloadSourceSaved = listener;
    }

    public void setOnSaveFailed(Consumer<String> listener) {
        onSaveFailed = listener;
    }

    public void setOnSaveSucceeded(Runnable listener) {
        onSaveSucceeded = listener;
    }

    private void save(String operation, ControllerContext.IoAction action) {
        save(operation, action, null);
    }

    private void save(String operation, ControllerContext.IoAction action, Runnable onSaved) {
        context.executeSettings(operation, action,
                () -> {
                    notifySaveSucceeded(operation);
                    if (onSaved != null) {
                        onSaved.run();
                    }
                },
                message -> notifySaveFailed(operation, message));
    }

    private void notifySaveSucceeded(String operation) {
        if (!Objects.equals(failedSaveOperation, operation)) {
            return;
        }
        failedSaveOperation = null;
        Runnable listener = onSaveSucceeded;
        if (listener != null) {
            listener.run();
        }
    }

    private void notifySaveFailed(String operation, String message) {
        failedSaveOperation = operation;
        Consumer<String> listener = onSaveFailed;
        if (listener != null) {
            listener.accept(UiText.tr("log.error.operationFailed", operation, message));
        }
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
