package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 设置页控制器：持久化设置、游戏位置检查和系统内存监视。 */
public final class SettingsController {
    private static final Logger LOGGER = LogManager.getLogger(SettingsController.class);

    private final ControllerContext context;
    private final ScheduledExecutorService memoryExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "caml-memory-monitor");
                thread.setDaemon(true);
                return thread;
            });

    private volatile Consumer<SystemMemoryInfo> onSystemMemoryUpdated;
    private boolean memoryMonitorStarted;

    SettingsController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public Path defaultGameDirectory() {
        return context.backend().status().gameDirectory();
    }

    public int downloadConcurrency() {
        return context.backend().downloadConcurrency();
    }

    public void inspectGameDirectory(Path directory, Consumer<GameDirectoryInfo> callback) {
        context.inspectGameDirectory(directory, callback);
    }

    /** 异步保存最大内存，避免在 JavaFX Application Thread 上写文件。 */
    public void saveMaxMemoryMbAsync(int maxMemoryMb) {
        context.executeIo(() -> {
            try {
                context.backend().setMaxMemoryMb(maxMemoryMb);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("保存最大内存设置失败: {}", maxMemoryMb, e);
            }
        });
    }

    /** 异步保存游戏位置，避免在 JavaFX Application Thread 上写文件。 */
    public void saveGameDirectoryAsync(Path gameDirectory) {
        context.executeIo(() -> {
            try {
                context.backend().setGameDirectory(gameDirectory);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("保存游戏位置失败: {}", gameDirectory, e);
            }
        });
    }

    /** 异步保存版本隔离选择，避免在 JavaFX Application Thread 上写文件。 */
    public void saveVersionIsolationAsync(boolean fullVersionIsolation) {
        context.executeIo(() -> {
            try {
                context.backend().setVersionIsolationMode(fullVersionIsolation
                        ? VersionIsolationMode.FULL
                        : VersionIsolationMode.NONE);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("保存版本隔离设置失败: {}", fullVersionIsolation, e);
            }
        });
    }

    /** 异步保存下载并发数，并立即更新后端下载器。 */
    public void saveDownloadConcurrencyAsync(int downloadConcurrency) {
        context.executeIo(() -> {
            try {
                context.backend().setDownloadConcurrency(downloadConcurrency);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("保存下载并发数失败: {}", downloadConcurrency, e);
            }
        });
    }

    /** 每 500ms 读取一次系统内存，并立即在 JavaFX 线程刷新监听者。 */
    public void setOnSystemMemoryUpdated(Consumer<SystemMemoryInfo> listener) {
        onSystemMemoryUpdated = listener;
        if (memoryMonitorStarted) {
            return;
        }
        memoryMonitorStarted = true;
        memoryExecutor.scheduleAtFixedRate(() -> {
            SystemMemoryInfo memory = context.backend().systemMemory();
            Consumer<SystemMemoryInfo> current = onSystemMemoryUpdated;
            if (current != null) {
                Platform.runLater(() -> current.accept(memory));
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    void shutdown() {
        memoryExecutor.shutdownNow();
    }
}
