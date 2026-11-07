package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.ProcessListener;
import org.chs.camlauncher.backend.metadata.VersionManifest;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 前端主控制器。
 *
 * <p>职责边界（对应 design/architecture.md）：</p>
 * <ul>
 *     <li>只负责把界面事件转发给 {@link CAMLBackend} facade，不直接读写文件、不发 HTTP、不启动进程。</li>
 *     <li>磁盘、网络和进程操作一律在后台线程执行，绝不占用 JavaFX Application Thread。</li>
 *     <li>后端结果通过回调 + {@link Platform#runLater(Runnable)} 回到 UI 线程更新界面。</li>
 * </ul>
 */
public final class MainController {
    private static final Logger LOGGER = LogManager.getLogger(MainController.class);

    private final CAMLBackend backend;

    /**
     * 专用 I/O 线程。
     *
     * <p>目录探测、下载等操作会阻塞，不能放在 JavaFX Application Thread，
     * 也不适合占用 ForkJoinPool 公共线程池，因此使用单线程守护线程池。</p>
     */
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "caml-io-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final ScheduledExecutorService memoryExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "caml-memory-monitor");
                thread.setDaemon(true);
                return thread;
            });

    private volatile Consumer<SystemMemoryInfo> onSystemMemoryUpdated;
    private boolean memoryMonitorStarted;

    /** UI 回调：收到一行日志。 */
    private Consumer<String> onLogReceived;
    /** UI 回调：启动状态变化。 */
    private Consumer<String> onStatusChanged;
    /** UI 回调：一次启动流程结束，用于恢复按钮。 */
    private Runnable onLaunchFinished;
    /** UI 回调：游戏目录探测结果。 */
    private Consumer<GameDirectoryInfo> onGameDirectoryInspected;

    public MainController(CAMLBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public void setOnLogReceived(Consumer<String> onLogReceived) {
        this.onLogReceived = onLogReceived;
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
     * @return 后端配置的默认游戏目录，供“游戏位置”下拉框初始化使用
     */
    public Path defaultGameDirectory() {
        return backend.status().gameDirectory();
    }

    /**
     * @return 后端状态 DTO，供界面展示版本、数据目录等信息
     */
    public BackendStatus status() {
        return backend.status();
    }

    public int downloadConcurrency() {
        return backend.downloadConcurrency();
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

        // 仅设置最大内存，其余选项保持默认。
        LaunchOptions options = new LaunchOptions(
                null, memoryMb, null, null, false,
                List.of(), List.of(), Map.of()
        );

        DownloadListener downloadListener = new ThrottledDownloadLogger();

        ProcessListener processListener = new ProcessListener() {
            @Override
            public void onLog(String line, boolean error) {
                sendLog((error ? "[ERR] " : "[OUT] ") + line);
            }

            @Override
            public void onExit(int exitCode) {
                sendLog("游戏进程已退出，代码: " + exitCode);
                notifyLaunchFinished();
            }
        };

        // 启动链路包含网络、磁盘和本地程序操作，必须离开 JavaFX Application Thread。
        Thread worker = new Thread(() -> {
            try {
                sendStatus("正在启动 " + version + "...");
                backend.launchGame(version, gameDirectory, username, options,
                        fullVersionIsolation
                                ? VersionIsolationMode.FULL
                                : VersionIsolationMode.NONE,
                        downloadListener, processListener);
                saveLastLaunchedInstance(version);
                sendStatus("启动指令已发送");
            } catch (Exception ex) {
                sendLog("[FATAL] " + ex.getMessage());
                sendStatus("启动失败！");
                notifyLaunchFinished();
            }
        }, "caml-launch-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveLastLaunchedInstance(String version) {
        try {
            backend.setLastLaunchedInstanceId(version);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("保存最后启动实例失败: {}", version, e);
        }
    }

    /**
     * 异步探测游戏目录。
     *
     * <p>前端不直接读取文件系统，磁盘扫描交给后端
     * {@link CAMLBackend#inspectGameDirectory(Path)} 完成。结果通过
     * {@link CompletableFuture} 回到 UI 线程回调。</p>
     */
    public void inspectGameDirectory(Path directory) {
        inspectGameDirectory(directory, onGameDirectoryInspected);
    }

    /** 异步探测目录，结果交给指定回调；回调始终在 JavaFX 线程执行。 */
    public void inspectGameDirectory(Path directory, Consumer<GameDirectoryInfo> callback) {
        if (directory == null) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> backend.inspectGameDirectory(directory), ioExecutor)
                .thenAccept(info -> Platform.runLater(() -> {
                    if (callback != null) {
                        callback.accept(info);
                    }
                }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    sendLog("[目录] 检查失败: " + cause.getMessage());
                    return null;
                });
    }

    /**
     * 异步获取官方版本清单中的正式版，结果和错误都回到 JavaFX 线程。
     */
    public void fetchReleaseVersions(Consumer<List<VersionManifest.Version>> onLoaded,
                                     Consumer<String> onError) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return backend.fetchVersionManifest();
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(manifest -> {
                    List<VersionManifest.Version> releases = manifest.releasesNewestFirst();
                    Platform.runLater(() -> onLoaded.accept(releases));
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() == null
                            ? cause.getClass().getSimpleName()
                            : cause.getMessage();
                    LOGGER.warn("获取官方版本列表失败", cause);
                    sendLog("[版本列表] 获取失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    /** 异步下载官方原版实例；完成和失败回调都切回 JavaFX 线程。 */
    public void downloadVersion(String versionId, String instanceId, Path gameDirectory,
                                Runnable onSuccess, Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return backend.installVersion(
                                versionId, instanceId, gameDirectory, DownloadListener.NONE);
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(installed -> {
                    LOGGER.info("官方原版实例安装完成: {} / {} -> {}",
                            instanceId, versionId, installed.gameDirectory());
                    sendLog("[下载] 已安装实例 " + instanceId + "（原版 " + versionId + "）");
                    Platform.runLater(onSuccess);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() == null
                            ? cause.getClass().getSimpleName()
                            : cause.getMessage();
                    LOGGER.warn("下载官方原版实例失败: {} / {}", instanceId, versionId, cause);
                    sendLog("[下载] 实例 " + instanceId + "（原版 " + versionId + "）失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void fetchFabricLoaderVersions(String gameVersion,
                                          Consumer<List<FabricLoaderVersion>> onLoaded,
                                          Consumer<String> onError) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return backend.fetchFabricLoaderVersions(gameVersion);
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(versions -> Platform.runLater(() -> onLoaded.accept(versions)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = messageOf(cause);
                    LOGGER.warn("获取 Fabric Loader 版本失败: {}", gameVersion, cause);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void fetchFabricApiVersions(String gameVersion,
                                       Consumer<List<FabricApiVersion>> onLoaded,
                                       Consumer<String> onError) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return backend.fetchFabricApiVersions(gameVersion);
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(versions -> Platform.runLater(() -> onLoaded.accept(versions)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = messageOf(cause);
                    LOGGER.warn("获取 Fabric API 版本失败: {}", gameVersion, cause);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void downloadFabricVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            boolean fullVersionIsolation,
            Runnable onSuccess,
            Consumer<String> onError
    ) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return backend.installFabricVersion(
                                gameVersion,
                                instanceId,
                                loaderVersion,
                                apiVersion,
                                gameDirectory,
                                fullVersionIsolation,
                                DownloadListener.NONE
                        );
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(installed -> {
                    LOGGER.info("Fabric 实例安装完成: {} / {} -> {}",
                            instanceId, loaderVersion, installed.gameDirectory());
                    sendLog("[下载] 已安装 Fabric 实例 " + instanceId + "（Loader " + loaderVersion + "）");
                    Platform.runLater(onSuccess);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = messageOf(cause);
                    LOGGER.warn("Fabric 实例安装失败: {} / {}", instanceId, loaderVersion, cause);
                    sendLog("[下载] Fabric 实例 " + instanceId + " 安装失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void fetchForgeVersions(String gameVersion,
                                   Consumer<List<ForgeVersion>> onLoaded,
                                   Consumer<String> onError) {
        fetchLoaderVersions("Forge", () -> backend.fetchForgeVersions(gameVersion), onLoaded, onError);
    }

    public void fetchNeoForgeVersions(String gameVersion,
                                      Consumer<List<NeoForgeVersion>> onLoaded,
                                      Consumer<String> onError) {
        fetchLoaderVersions("NeoForge", () -> backend.fetchNeoForgeVersions(gameVersion), onLoaded, onError);
    }

    private <T> void fetchLoaderVersions(String name, LoaderSupplier<T> supplier,
                                         Consumer<List<T>> onLoaded, Consumer<String> onError) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return supplier.get();
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(versions -> Platform.runLater(() -> onLoaded.accept(versions)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = messageOf(cause);
                    LOGGER.warn("获取 {} 版本失败", name, cause);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void downloadForgeVersion(String gameVersion, String instanceId, ForgeVersion version,
                                     Path gameDirectory, Runnable onSuccess,
                                     Consumer<String> onError) {
        downloadExternalLoaderVersion("Forge", gameVersion, instanceId, gameDirectory,
                () -> backend.installForgeVersion(gameVersion, instanceId, version, gameDirectory,
                        DownloadListener.NONE),
                onSuccess, onError);
    }

    public void downloadNeoForgeVersion(String gameVersion, String instanceId, NeoForgeVersion version,
                                        Path gameDirectory, Runnable onSuccess,
                                        Consumer<String> onError) {
        downloadExternalLoaderVersion("NeoForge", gameVersion, instanceId, gameDirectory,
                () -> backend.installNeoForgeVersion(gameVersion, instanceId, version, gameDirectory,
                        DownloadListener.NONE),
                onSuccess, onError);
    }

    private void downloadExternalLoaderVersion(
            String loaderName,
            String gameVersion,
            String instanceId,
            Path gameDirectory,
            LoaderInstallSupplier supplier,
            Runnable onSuccess,
            Consumer<String> onError
    ) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return supplier.install();
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new CompletionException(e);
                    }
                }, ioExecutor)
                .thenAccept(installed -> {
                    LOGGER.info("{} 实例安装完成: {} -> {}", loaderName, instanceId, gameVersion);
                    sendLog("[下载] 已安装 " + loaderName + " 实例 " + instanceId);
                    Platform.runLater(onSuccess);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = messageOf(cause);
                    LOGGER.warn("{} 实例安装失败: {}", loaderName, instanceId, cause);
                    sendLog("[下载] " + loaderName + " 实例 " + instanceId + " 安装失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    @FunctionalInterface
    private interface LoaderSupplier<T> {
        List<T> get() throws IOException, InterruptedException;
    }

    @FunctionalInterface
    private interface LoaderInstallSupplier {
        Object install() throws IOException, InterruptedException;
    }

    private static String messageOf(Throwable cause) {
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    /** 关闭后台线程池，由应用退出流程调用。 */
    public void shutdown() {
        ioExecutor.shutdownNow();
        memoryExecutor.shutdownNow();
    }

    /** 异步保存最大内存，避免在 JavaFX Application Thread 上写文件。 */
    public void saveMaxMemoryMbAsync(int maxMemoryMb) {
        try {
            ioExecutor.execute(() -> {
                try {
                    backend.setMaxMemoryMb(maxMemoryMb);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("保存最大内存设置失败: {}", maxMemoryMb, e);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // 应用正在退出，不再接受新的持久化任务。
        }
    }

    /** 异步保存游戏位置，避免在 JavaFX Application Thread 上写文件。 */
    public void saveGameDirectoryAsync(Path gameDirectory) {
        try {
            ioExecutor.execute(() -> {
                try {
                    backend.setGameDirectory(gameDirectory);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("保存游戏位置失败: {}", gameDirectory, e);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // 应用正在退出，不再接受新的持久化任务。
        }
    }

    /** 异步保存版本隔离选择，避免在 JavaFX Application Thread 上写文件。 */
    public void saveVersionIsolationAsync(boolean fullVersionIsolation) {
        try {
            ioExecutor.execute(() -> {
                try {
                    backend.setVersionIsolationMode(fullVersionIsolation
                            ? VersionIsolationMode.FULL
                            : VersionIsolationMode.NONE);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("保存版本隔离设置失败: {}", fullVersionIsolation, e);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // 应用正在退出，不再接受新的持久化任务。
        }
    }

    /** 异步保存下载并发数，并立即更新后端下载器。 */
    public void saveDownloadConcurrencyAsync(int downloadConcurrency) {
        try {
            ioExecutor.execute(() -> {
                try {
                    backend.setDownloadConcurrency(downloadConcurrency);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("保存下载并发数失败: {}", downloadConcurrency, e);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // 应用正在退出，不再接受新的持久化任务。
        }
    }

    /**
     * 每 500ms 读取一次系统内存，并立即在 JavaFX 线程刷新监听者。
     */
    public void setOnSystemMemoryUpdated(Consumer<SystemMemoryInfo> listener) {
        onSystemMemoryUpdated = listener;
        if (memoryMonitorStarted) {
            return;
        }
        memoryMonitorStarted = true;
        memoryExecutor.scheduleAtFixedRate(() -> {
            SystemMemoryInfo memory = backend.systemMemory();
            Consumer<SystemMemoryInfo> current = onSystemMemoryUpdated;
            if (current != null) {
                Platform.runLater(() -> current.accept(memory));
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    /** 恢复启动按钮；无论回调来自哪个线程，都切回 UI 线程。 */
    private void notifyLaunchFinished() {
        if (onLaunchFinished != null) {
            Platform.runLater(onLaunchFinished);
        }
    }

    private void sendLog(String log) {
        if (onLogReceived != null) {
            Platform.runLater(() -> onLogReceived.accept(log));
        }
    }

    private void sendStatus(String status) {
        if (onStatusChanged != null) {
            Platform.runLater(() -> onStatusChanged.accept(status));
        }
    }

    /**
     * 下载进度节流器。
     *
     * <p>下载器每写入 8KB 就回调一次，如果全部写进日志框会瞬间刷屏。
     * 这里按“文件切换”和“百分比变化”两个条件输出，最后一帧必定输出。</p>
     */
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
                // 同一文件内每 10% 才输出一次。
                if (percent < 0 || percent < lastPercent + 10) {
                    return;
                }
                lastPercent = percent;
            }

            if (progress.total() > 0) {
                sendLog(String.format("[下载] %s (%d/%d)",
                        progress.label(), progress.downloaded(), progress.total()));
            } else {
                sendLog(String.format("[下载] %s (%d)", progress.label(), progress.downloaded()));
            }
        }
    }
}
