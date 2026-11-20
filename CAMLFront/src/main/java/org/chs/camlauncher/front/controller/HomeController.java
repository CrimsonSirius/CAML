package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.api.InstanceDetails;
import org.chs.camlauncher.backend.api.InstanceModUpdate;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.launch.GameLaunch;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.front.text.AccountChoice;
import org.chs.camlauncher.front.text.UiText;

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
                                       Path gameDirectory, boolean fullVersionIsolation,
                                       String jvmArguments) {
    return startGame(version, username, AccountChoice.OFFLINE, memoryMb,
            gameDirectory, fullVersionIsolation, jvmArguments);
}

/**
 * 按账号选择启动游戏。
 *
 * @param accountChoice {@link AccountChoice#OFFLINE} 使用离线用户名；
 *                      {@link AccountChoice#DEFAULT} 走“实例账号 → 全局账号”。
 *                      其他值作为账号 id 直接使用
 */
public FrontTask<GameLaunch> startGame(String version, String username, String accountChoice,
                                       int memoryMb, Path gameDirectory,
                                       boolean fullVersionIsolation, String jvmArguments) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        LaunchOptions options = new LaunchOptions(
                null, memoryMb, null, null, false,
                context.backend().parseJvmArguments(jvmArguments), List.of(), Map.of()
        );

        DownloadListener downloadListener = new ThrottledDownloadLogger();
        ProcessListener processListener = createProcessListener();
        VersionIsolationMode isolationMode = fullVersionIsolation
                ? VersionIsolationMode.FULL
                : VersionIsolationMode.NONE;

        sendStatus(UiText.tr("status.starting", version), false);
        return context.runLaunch(UiText.tr("operation.launchGame", version),
                () -> launch(version, gameDirectory, username, accountChoice,
                        options, isolationMode, downloadListener, processListener),
                launch -> {
                    saveLastLaunchedInstance(version);
                    sendStatus(UiText.tr("status.started"), false);
                },
                message -> {
                    sendStatus(UiText.tr("status.launchFailedDetail", message), true);
                    notifyLaunchFinished();
                });
    }

    private GameLaunch launch(String version, Path gameDirectory, String username,
                              String accountChoice, LaunchOptions options,
                              VersionIsolationMode isolationMode,
                              DownloadListener downloadListener,
                              ProcessListener processListener)
            throws IOException, InterruptedException {
        if (accountChoice == null || AccountChoice.OFFLINE.equals(accountChoice)) {
            return context.backend().launchGame(version, gameDirectory, username, options,
                    isolationMode, downloadListener, processListener);
        }
        String accountId = AccountChoice.DEFAULT.equals(accountChoice) ? null : accountChoice;
        return context.backend().launchGameWithAccount(version, gameDirectory, accountId,
                options, isolationMode, downloadListener, processListener);
    }

    private ProcessListener createProcessListener() {
        return new ProcessListener() {
            @Override
            public void onLog(String line, boolean error) {
                context.sendLog(error ? "[ERR] " + line : line);
            }

            @Override
            public void onExit(int exitCode) {
                if (exitCode != 0) {
                    context.markFailure();
                    context.sendLog(UiText.tr("log.gameProcess.exitError", exitCode));
                } else {
                    context.sendLog(UiText.tr("log.gameProcess.exit", exitCode));
                }
                notifyLaunchFinished();
            }
        };
    }

    private void saveLastLaunchedInstance(String version) {
        try {
            context.backend().setLastLaunchedInstanceId(version);
        } catch (IOException | RuntimeException e) {
            context.reportError(UiText.tr("operation.saveLastInstance"), e, null);
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

    public FrontTask<InstanceDetails> inspectInstance(
            Path gameDirectory, String instanceId,
            Consumer<InstanceDetails> callback, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runCheck(UiText.tr("operation.inspectInstance"),
                () -> context.backend().inspectInstance(gameDirectory, instanceId),
                callback, onError);
    }

    public FrontTask<Void> openInstanceFolder(
            Path gameDirectory, String instanceId,
            Runnable onOpened, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runJava(UiText.tr("operation.openInstanceFolder"), () -> {
            context.backend().openInstanceFolder(gameDirectory, instanceId);
            return null;
        }, ignored -> {
            if (onOpened != null) {
                onOpened.run();
            }
        }, onError);
    }

    public FrontTask<byte[]> loadInstanceModIcon(
            Path gameDirectory, String instanceId, String fileName,
            Consumer<byte[]> onLoaded, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runIcon(UiText.tr("operation.loadInstanceModIcon"),
                () -> context.backend().loadInstanceModIcon(gameDirectory, instanceId, fileName),
                onLoaded, onError);
    }

    public FrontTask<Void> setModDisabled(
            Path gameDirectory, String instanceId, String fileName, boolean disabled,
            Runnable onSuccess, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.setModDisabled"), () -> {
            context.backend().setModDisabled(gameDirectory, instanceId, fileName, disabled);
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError);
    }

    public FrontTask<Void> setResourcePackDisabled(
            Path gameDirectory, String instanceId, String fileName, boolean disabled,
            Runnable onSuccess, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.setResourcePackDisabled"), () -> {
            context.backend().setResourcePackDisabled(
                    gameDirectory, instanceId, fileName, disabled);
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError);
    }

    public FrontTask<List<InstanceModUpdate>> checkInstanceModUpdates(
            Path gameDirectory, String instanceId,
            Consumer<List<InstanceModUpdate>> onLoaded, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runMetadata(UiText.tr("operation.checkModUpdates"),
                () -> context.backend().checkInstanceModUpdates(gameDirectory, instanceId),
                onLoaded, onError);
    }

    public FrontTask<ModDownloadResult> updateInstanceMod(
            Path gameDirectory, String instanceId, String currentFileName, ModFile latestFile,
            DownloadListener listener,
            Consumer<ModDownloadResult> onSuccess, Consumer<String> onError) {
        return context.runDownload(UiText.tr("operation.updateMod", currentFileName),
                () -> context.backend().updateInstanceMod(
                        gameDirectory, instanceId, currentFileName, latestFile, listener),
                onSuccess, onError);
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
                context.sendLog(UiText.tr("log.download.progress",
                        progress.label(), progress.downloaded(), progress.total()));
            } else {
                context.sendLog(UiText.tr("log.download.progressNoTotal",
                        progress.label(), progress.downloaded()));
            }
        }
    }
}
