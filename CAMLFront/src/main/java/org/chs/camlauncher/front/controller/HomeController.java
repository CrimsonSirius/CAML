package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.process.ProcessListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** 主页控制器：启动游戏、日志/状态回调和实例目录扫描。 */
public final class HomeController {
    private static final Logger LOGGER = LogManager.getLogger(HomeController.class);

    private final ControllerContext context;

    private Consumer<String> onStatusChanged;
    private Runnable onLaunchFinished;
    private Consumer<GameDirectoryInfo> onGameDirectoryInspected;

    HomeController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void setOnLogReceived(Consumer<String> onLogReceived) {
        context.setLogSink(onLogReceived);
    }

    public void setOnStatusChanged(Consumer<String> onStatusChanged) {
        this.onStatusChanged = onStatusChanged;
    }

    public void setOnLaunchFinished(Runnable onLaunchFinished) {
        this.onLaunchFinished = onLaunchFinished;
    }

    public void setOnGameDirectoryInspected(Consumer<GameDirectoryInfo> onGameDirectoryInspected) {
        this.onGameDirectoryInspected = onGameDirectoryInspected;
    }

    /**
     * 在指定游戏目录启动游戏。
     *
     * <p>这里只负责组装参数和派发后台任务；真正的安装、登录、Java 选择和进程启动
     * 都由 {@code CAMLBackend#launchGame} 在后端完成。</p>
     */
    public void startGame(String version, String username, int memoryMb, Path gameDirectory,
                          boolean fullVersionIsolation) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");

        LaunchOptions options = new LaunchOptions(
                null, memoryMb, null, null, false,
                List.of(), List.of(), Map.of()
        );

        DownloadListener downloadListener = new ThrottledDownloadLogger();

        ProcessListener processListener = new ProcessListener() {
            @Override
            public void onLog(String line, boolean error) {
                context.sendLog(error ? "[ERR] " + line : line);
            }

            @Override
            public void onExit(int exitCode) {
                context.sendLog("游戏进程已退出，代码: " + exitCode);
                notifyLaunchFinished();
            }
        };

        Thread worker = new Thread(() -> {
            try {
                sendStatus("正在启动 " + version + "...");
                context.backend().launchGame(version, gameDirectory, username, options,
                        fullVersionIsolation
                                ? VersionIsolationMode.FULL
                                : VersionIsolationMode.NONE,
                        downloadListener, processListener);
                saveLastLaunchedInstance(version);
                sendStatus("启动指令已发送");
            } catch (Exception ex) {
                context.sendLog("[FATAL] " + ex.getMessage());
                sendStatus("启动失败！");
                notifyLaunchFinished();
            }
        }, "caml-launch-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveLastLaunchedInstance(String version) {
        try {
            context.backend().setLastLaunchedInstanceId(version);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("保存最后启动实例失败: {}", version, e);
        }
    }

    public void inspectGameDirectory(Path directory) {
        inspectGameDirectory(directory, onGameDirectoryInspected);
    }

    public void inspectGameDirectory(Path directory, Consumer<GameDirectoryInfo> callback) {
        context.inspectGameDirectory(directory, callback);
    }

    private void notifyLaunchFinished() {
        if (onLaunchFinished != null) {
            Platform.runLater(onLaunchFinished);
        }
    }

    private void sendStatus(String status) {
        if (onStatusChanged != null) {
            Platform.runLater(() -> onStatusChanged.accept(status));
        }
    }

    /** 下载进度节流器，避免把每个 8KB 写入都刷进日志框。 */
    private final class ThrottledDownloadLogger implements DownloadListener {
        private String lastLabel;
        private int lastPercent = -1;

        @Override
        public synchronized void onProgress(DownloadProgress progress) {
            boolean finished = progress.total() > 0 && progress.downloaded() >= progress.total();
            boolean labelChanged = lastLabel == null || !lastLabel.equals(progress.label());

            if (labelChanged) {
                lastLabel = progress.label();
                lastPercent = -1;
            } else if (!finished) {
                int percent = progress.total() > 0
                        ? (int) (progress.downloaded() * 100 / progress.total())
                        : -1;
                if (percent < 0 || percent < lastPercent + 10) {
                    return;
                }
                lastPercent = percent;
            }

            if (progress.total() > 0) {
                context.sendLog(String.format("[下载] %s (%d/%d)",
                        progress.label(), progress.downloaded(), progress.total()));
            } else {
                context.sendLog(String.format("[下载] %s (%d)",
                        progress.label(), progress.downloaded()));
            }
        }
    }
}
