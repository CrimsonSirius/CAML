package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** 下载页控制器：版本列表和原版/加载器实例安装。 */
public final class DownloadsController {
    private static final Logger LOGGER = LogManager.getLogger(DownloadsController.class);

    private final ControllerContext context;

    DownloadsController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public String downloadSourceLabel() {
        DownloadSourceMode mode = context.backend().downloadSourceMode();
        return switch (mode) {
            case MIRROR -> "镜像源";
            case OFFICIAL -> "官方源";
            case CUSTOM -> DownloadSourceFactory.isValidCustomApi(
                    context.backend().customDownloadApi()) ? "自定义源" : "官方源";
        };
    }

    /** 异步获取官方版本清单中的正式版。 */
    public void fetchReleaseVersions(Consumer<List<VersionManifest.Version>> onLoaded,
                                     Consumer<String> onError) {
        fetchVersions("正式版", VersionManifest::releasesNewestFirst, onLoaded, onError);
    }

    /** 异步获取官方版本清单中的快照版。 */
    public void fetchSnapshotVersions(Consumer<List<VersionManifest.Version>> onLoaded,
                                      Consumer<String> onError) {
        fetchVersions("快照版", VersionManifest::snapshotsNewestFirst, onLoaded, onError);
    }

    /** 异步获取官方版本清单中的远古版（Alpha/Beta）。 */
    public void fetchOldVersions(Consumer<List<VersionManifest.Version>> onLoaded,
                                 Consumer<String> onError) {
        fetchVersions("远古版", VersionManifest::oldVersionsNewestFirst, onLoaded, onError);
    }

    private void fetchVersions(String category,
                               Function<VersionManifest, List<VersionManifest.Version>> selector,
                               Consumer<List<VersionManifest.Version>> onLoaded,
                               Consumer<String> onError) {
        context.supplyAsync(() -> context.backend().fetchVersionManifest())
                .thenAccept(manifest -> {
                    List<VersionManifest.Version> versions = selector.apply(manifest);
                    Platform.runLater(() -> onLoaded.accept(versions));
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
                    LOGGER.warn("获取官方{}列表失败", category, cause);
                    context.sendLog("[版本列表] 获取" + category + "失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    /** 异步下载官方原版实例。 */
    public void downloadVersion(String versionId, String instanceId, Path gameDirectory,
                                Runnable onSuccess, Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        context.supplyAsync(() -> context.backend().installVersion(
                        versionId, instanceId, gameDirectory, DownloadListener.NONE))
                .thenAccept(installed -> {
                    LOGGER.info("官方原版实例安装完成: {} / {} -> {}",
                            instanceId, versionId, installed.gameDirectory());
                    context.sendLog("[下载] 已安装实例 " + instanceId
                            + "（原版 " + versionId + "）");
                    Platform.runLater(onSuccess);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
                    LOGGER.warn("下载官方原版实例失败: {} / {}", instanceId, versionId, cause);
                    context.sendLog("[下载] 实例 " + instanceId
                            + "（原版 " + versionId + "）失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void fetchFabricLoaderVersions(String gameVersion,
                                          Consumer<List<FabricLoaderVersion>> onLoaded,
                                          Consumer<String> onError) {
        context.supplyAsync(() -> context.backend().fetchFabricLoaderVersions(gameVersion))
                .thenAccept(versions -> Platform.runLater(() -> onLoaded.accept(versions)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
                    LOGGER.warn("获取 Fabric Loader 版本失败: {}", gameVersion, cause);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void fetchFabricApiVersions(String gameVersion,
                                       Consumer<List<FabricApiVersion>> onLoaded,
                                       Consumer<String> onError) {
        context.supplyAsync(() -> context.backend().fetchFabricApiVersions(gameVersion))
                .thenAccept(versions -> Platform.runLater(() -> onLoaded.accept(versions)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
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
        context.supplyAsync(() -> context.backend().installFabricVersion(
                        gameVersion,
                        instanceId,
                        loaderVersion,
                        apiVersion,
                        gameDirectory,
                        fullVersionIsolation,
                        DownloadListener.NONE
                ))
                .thenAccept(installed -> {
                    LOGGER.info("Fabric 实例安装完成: {} / {} -> {}",
                            instanceId, loaderVersion, installed.gameDirectory());
                    context.sendLog("[下载] 已安装 Fabric 实例 " + instanceId
                            + "（Loader " + loaderVersion + "）");
                    Platform.runLater(onSuccess);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
                    LOGGER.warn("Fabric 实例安装失败: {} / {}", instanceId, loaderVersion, cause);
                    context.sendLog("[下载] Fabric 实例 " + instanceId + " 安装失败: " + message);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void fetchForgeVersions(String gameVersion,
                                   Consumer<List<ForgeVersion>> onLoaded,
                                   Consumer<String> onError) {
        fetchLoaderVersions("Forge",
                () -> context.backend().fetchForgeVersions(gameVersion), onLoaded, onError);
    }

    public void fetchNeoForgeVersions(String gameVersion,
                                      Consumer<List<NeoForgeVersion>> onLoaded,
                                      Consumer<String> onError) {
        fetchLoaderVersions("NeoForge",
                () -> context.backend().fetchNeoForgeVersions(gameVersion), onLoaded, onError);
    }

    public void fetchOptiFineVersions(String gameVersion,
                                      Consumer<List<OptiFineVersion>> onLoaded,
                                      Consumer<String> onError) {
        fetchLoaderVersions("OptiFine",
                () -> context.backend().fetchOptiFineVersions(gameVersion), onLoaded, onError);
    }

    private <T> void fetchLoaderVersions(String name, LoaderSupplier<T> supplier,
                                         Consumer<List<T>> onLoaded, Consumer<String> onError) {
        context.supplyAsync(supplier::get)
                .thenAccept(versions -> Platform.runLater(() -> onLoaded.accept(versions)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
                    LOGGER.warn("获取 {} 版本失败", name, cause);
                    Platform.runLater(() -> onError.accept(message));
                    return null;
                });
    }

    public void downloadForgeVersion(String gameVersion, String instanceId, ForgeVersion version,
                                     Path gameDirectory, Runnable onSuccess,
                                     Consumer<String> onError) {
        downloadExternalLoaderVersion("Forge", gameVersion, instanceId, gameDirectory,
                () -> context.backend().installForgeVersion(gameVersion, instanceId, version,
                        gameDirectory, DownloadListener.NONE),
                onSuccess, onError);
    }

    public void downloadNeoForgeVersion(String gameVersion, String instanceId, NeoForgeVersion version,
                                        Path gameDirectory, Runnable onSuccess,
                                        Consumer<String> onError) {
        downloadExternalLoaderVersion("NeoForge", gameVersion, instanceId, gameDirectory,
                () -> context.backend().installNeoForgeVersion(gameVersion, instanceId, version,
                        gameDirectory, DownloadListener.NONE),
                onSuccess, onError);
    }

    public void downloadForgeOptiFineVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            Runnable onSuccess,
            Consumer<String> onError
    ) {
        downloadExternalLoaderVersion("Forge + OptiFine", gameVersion, instanceId, gameDirectory,
                () -> context.backend().installForgeOptiFineVersion(
                        gameVersion, instanceId, forgeVersion, optiFineVersion,
                        gameDirectory, DownloadListener.NONE),
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
        context.supplyAsync(supplier::install)
                .thenAccept(installed -> {
                    LOGGER.info("{} 实例安装完成: {} -> {}", loaderName, instanceId, gameVersion);
                    context.sendLog("[下载] 已安装 " + loaderName + " 实例 " + instanceId);
                    Platform.runLater(onSuccess);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = ControllerContext.messageOf(cause);
                    LOGGER.warn("{} 实例安装失败: {}", loaderName, instanceId, cause);
                    context.sendLog("[下载] " + loaderName + " 实例 " + instanceId
                            + " 安装失败: " + message);
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
}
