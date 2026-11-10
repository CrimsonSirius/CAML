package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.launch.GameLaunch;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.language.Language;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 主页控制器：启动游戏、日志/状态回调和实例目录扫描。 */
public final class HomeController {
    private final ControllerContext context;

    private BiConsumer<String, Boolean> onStatusChanged;
    private Runnable onLaunchFinished;
    private Consumer<GameDirectoryInfo> onGameDirectoryInspected;

    HomeController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void setOnLogReceived(Consumer<String> onLogReceived) {
        context.setLogSink(onLogReceived);
    }

    public void setOnStatusChanged(BiConsumer<String, Boolean> onStatusChanged) {
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
    public FrontTask<GameLaunch> startGame(String version, String username, int memoryMb,
                                           Path gameDirectory, boolean fullVersionIsolation) {
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
                if (exitCode != 0) {
                    context.markFailure();
                    context.sendLog(Language.tr("log.gameProcess.exitError", exitCode));
                } else {
                    context.sendLog(Language.tr("log.gameProcess.exit", exitCode));
                }
                notifyLaunchFinished();
            }
        };

        sendStatus(Language.tr("status.starting", version), false);
        return context.runLaunch(Language.tr("operation.launchGame", version),
                () -> context.backend().launchGame(version, gameDirectory, username, options,
                        fullVersionIsolation
                                ? VersionIsolationMode.FULL
                                : VersionIsolationMode.NONE,
                        downloadListener, processListener),
                launch -> {
                    saveLastLaunchedInstance(version);
                    sendStatus(Language.tr("status.started"), false);
                },
                message -> {
                    sendStatus(Language.tr("status.launchFailedDetail", message), true);
                    notifyLaunchFinished();
                });
    }

    private void saveLastLaunchedInstance(String version) {
        try {
            context.backend().setLastLaunchedInstanceId(version);
        } catch (IOException | RuntimeException e) {
            context.reportError(Language.tr("operation.saveLastInstance"), e, null);
        }
    }

    public FrontTask<GameDirectoryInfo> inspectGameDirectory(Path directory) {
        return inspectGameDirectory(directory, onGameDirectoryInspected);
    }

    public FrontTask<GameDirectoryInfo> inspectGameDirectory(
            Path directory, Consumer<GameDirectoryInfo> callback) {
        return context.inspectGameDirectory(directory, callback);
    }

    public FrontTask<GameDirectoryInfo> inspectGameDirectory(
            Path directory, Consumer<GameDirectoryInfo> callback, Consumer<String> onError) {
        return context.inspectGameDirectory(directory, callback, onError);
    }

    private void notifyLaunchFinished() {
        if (onLaunchFinished != null) {
            Platform.runLater(onLaunchFinished);
        }
    }

    private void sendStatus(String status, boolean error) {
        if (onStatusChanged != null) {
            Platform.runLater(() -> onStatusChanged.accept(status, error));
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
                context.sendLog(Language.tr("log.download.progress",
                        progress.label(), progress.downloaded(), progress.total()));
            } else {
                context.sendLog(Language.tr("log.download.progressNoTotal",
                        progress.label(), progress.downloaded()));
            }
        }
    }
}
