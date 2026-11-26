package org.chs.camlauncher.front.controller;

import org.chs.camlauncher.front.task.AsyncTaskHandle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceNameCheck;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.loader.quilt.QuiltApiVersion;
import org.chs.camlauncher.backend.loader.quilt.QuiltLoaderVersion;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModContentType;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.front.task.DownloadTask;

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
    private final ObservableList<DownloadTask> downloadTasks = FXCollections.observableArrayList();

    DownloadsController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public ObservableList<DownloadTask> downloadTasks() {
        return downloadTasks;
    }

    public void registerDownloadTask(DownloadTask task) {
        Objects.requireNonNull(task, "task");
        if (!downloadTasks.contains(task)) {
            downloadTasks.add(task);
        }
    }

    public void removeDownloadTask(DownloadTask task) {
        downloadTasks.remove(task);
    }

    public String downloadSourceLabel() {
        DownloadSourceMode mode = context.backend().downloadSourceMode();
        return switch (mode) {
            case MIRROR -> UiText.tr("download.source.mirror");
            case OFFICIAL -> UiText.tr("download.source.official");
            case CUSTOM -> DownloadSourceFactory.isValidCustomApi(
                    context.backend().customDownloadApi())
                    ? UiText.tr("download.source.custom")
                    : UiText.tr("download.source.official");
        };
    }

    public boolean curseForgeConfigured() {
        return context.backend().curseForgeConfigured();
    }

    public AsyncTaskHandle<ModSearchPage> searchMods(
            ModSearchQuery query,
            Consumer<ModSearchPage> onLoaded,
            Consumer<String> onError) {
        String operation = UiText.tr(contentOperation(query.type(),
                "operation.searchMods", "operation.searchShaders",
                "operation.searchResourcePacks", "operation.searchMaps",
                "operation.searchModpacks"), query.text());
        return context.runMetadata(operation,
                () -> context.backend().searchMods(query), onLoaded, onError);
    }

    public AsyncTaskHandle<ModProjectDetail> fetchModProject(
            ModSource source,
            String projectId,
            Consumer<ModProjectDetail> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchModProject", projectId),
                () -> context.backend().fetchModProject(source, projectId), onLoaded, onError);
    }

    public AsyncTaskHandle<ModProjectDetail> fetchContentProject(
            ModContentType type,
            ModSource source,
            String projectId,
            Consumer<ModProjectDetail> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr(contentOperation(type,
                        "operation.fetchModProject", "operation.fetchShaderProject",
                        "operation.fetchResourcePackProject", "operation.fetchMapProject",
                        "operation.fetchModpackProject"), projectId),
                () -> context.backend().fetchModProject(source, projectId), onLoaded, onError);
    }

    public AsyncTaskHandle<byte[]> loadModIcon(
            String iconUrl,
            Consumer<byte[]> onLoaded,
            Consumer<String> onError) {
        return context.runIcon(UiText.tr("operation.loadModIcon"),
                () -> context.backend().loadModIcon(iconUrl), onLoaded, onError);
    }

    public AsyncTaskHandle<byte[]> retryModIcon(
            String iconUrl,
            long delayMillis,
            Consumer<byte[]> onLoaded,
            Consumer<String> onError) {
        return context.runIconRetry(UiText.tr("operation.loadModIcon"), delayMillis,
                () -> context.backend().loadModIcon(iconUrl), onLoaded, onError);
    }

    public AsyncTaskHandle<ModDownloadResult> downloadMod(
            ModFile file,
            Path gameDirectory,
            String instanceId,
            boolean fullVersionIsolation,
            DownloadListener listener,
            DownloadCancellationToken token,
            Consumer<ModDownloadResult> onSuccess,
            Consumer<String> onError) {
        return context.runDownload(UiText.tr("operation.downloadMod", file.name()),
                () -> context.backend().downloadMod(
                        file,
                        gameDirectory,
                        instanceId,
                        fullVersionIsolation
                                ? VersionIsolationMode.FULL
                                : VersionIsolationMode.NONE,
                        listener,
                        token
                ),
                onSuccess,
                onError);
    }

    public AsyncTaskHandle<ModDownloadResult> downloadContent(
            ModContentType type,
            ModFile file,
            Path gameDirectory,
            String instanceId,
            boolean fullVersionIsolation,
            DownloadListener listener,
            DownloadCancellationToken token,
            Consumer<ModDownloadResult> onSuccess,
            Consumer<String> onError) {
        return context.runDownload(UiText.tr(contentOperation(type,
                        "operation.downloadMod", "operation.downloadShader",
                        "operation.downloadResourcePack", "operation.downloadMap",
                        "operation.installModpack"), file.name()),
                () -> context.backend().downloadContent(
                        type,
                        file,
                        gameDirectory,
                        instanceId,
                        fullVersionIsolation
                                ? VersionIsolationMode.FULL
                                : VersionIsolationMode.NONE,
                        listener,
                        token
                ),
                onSuccess,
                onError);
    }

    public AsyncTaskHandle<ModDownloadResult> installModpack(
            ModFile file,
            Path gameDirectory,
            String instanceName,
            DownloadListener listener,
            DownloadCancellationToken token,
            Consumer<ModDownloadResult> onSuccess,
            Consumer<String> onError) {
        return context.runDownload(
                UiText.tr("operation.installModpack", file.name()),
                () -> context.backend().installModpack(
                        file, gameDirectory, instanceName, listener, token),
                onSuccess,
                onError);
    }

    private static String contentOperation(ModContentType type,
                                           String modKey, String shaderKey,
                                           String resourcePackKey, String mapKey,
                                           String modpackKey) {
        return switch (type) {
            case MOD -> modKey;
            case MODPACK -> modpackKey;
            case SHADER -> shaderKey;
            case RESOURCE_PACK -> resourcePackKey;
            case WORLD -> mapKey;
        };
    }

    public AsyncTaskHandle<InstanceNameCheck> checkInstanceName(Path gameDirectory, String instanceId,
                                                          Consumer<InstanceNameCheck> onChecked,
                                                          Consumer<String> onError) {
        return context.runCheck(UiText.tr("operation.checkInstanceName"),
                () -> context.backend().checkInstanceName(gameDirectory, instanceId),
                onChecked, onError);
    }

    /** 异步获取官方版本清单中的正式版。 */
    public AsyncTaskHandle<List<VersionManifest.Version>> fetchReleaseVersions(
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return fetchVersions(UiText.tr("download.category.release"),
                VersionManifest::releasesNewestFirst, onLoaded, onError);
    }

    /** 异步获取官方版本清单中的快照版。 */
    public AsyncTaskHandle<List<VersionManifest.Version>> fetchSnapshotVersions(
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return fetchVersions(UiText.tr("download.category.snapshot"),
                VersionManifest::snapshotsNewestFirst, onLoaded, onError);
    }

    /** 异步获取官方版本清单中的远古版（Alpha/Beta）。 */
    public AsyncTaskHandle<List<VersionManifest.Version>> fetchOldVersions(
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return fetchVersions(UiText.tr("download.category.old"),
                VersionManifest::oldVersionsNewestFirst, onLoaded, onError);
    }

    private AsyncTaskHandle<List<VersionManifest.Version>> fetchVersions(
            String category,
            Function<VersionManifest, List<VersionManifest.Version>> selector,
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchVersionList", category),
                () -> selector.apply(context.backend().fetchVersionManifest()),
                onLoaded,
                onError);
    }

    /** 异步下载官方原版实例。 */
    public AsyncTaskHandle<InstalledVersion> downloadVersion(
            String versionId,
            String instanceId,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            Runnable onSuccess,
            Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        return context.runDownload(UiText.tr("operation.installVanilla", instanceId),
                () -> context.backend().installVersion(
                        versionId, instanceId, gameDirectory, listener, token),
                installed -> {
                    LOGGER.info("官方原版实例安装完成: {} / {} -> {}",
                            instanceId, versionId, installed.gameDirectory());
                    context.sendLog(UiText.tr("log.download.installed.vanilla", instanceId, versionId));
                    onSuccess.run();
                },
                onError);
    }

    public AsyncTaskHandle<List<FabricLoaderVersion>> fetchFabricLoaderVersions(
            String gameVersion,
            Consumer<List<FabricLoaderVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchFabricLoader"),
                () -> context.backend().fetchFabricLoaderVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<List<FabricApiVersion>> fetchFabricApiVersions(
            String gameVersion,
            Consumer<List<FabricApiVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchFabricApi"),
                () -> context.backend().fetchFabricApiVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<InstalledVersion> downloadFabricVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            Runnable onSuccess,
            Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        return context.runDownload(UiText.tr("operation.installFabric", instanceId),
                () -> context.backend().installFabricVersion(
                        gameVersion,
                        instanceId,
                        loaderVersion,
                        apiVersion,
                        gameDirectory,
                        listener,
                        token
                ),
                installed -> {
                    LOGGER.info("Fabric 实例安装完成: {} / {} -> {}",
                            instanceId, loaderVersion, installed.gameDirectory());
                    context.sendLog(UiText.tr("log.download.installed.fabric",
                            instanceId, loaderVersion));
                    onSuccess.run();
                },
                onError);
    }

    public AsyncTaskHandle<List<QuiltLoaderVersion>> fetchQuiltLoaderVersions(
            String gameVersion,
            Consumer<List<QuiltLoaderVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchQuiltLoader"),
                () -> context.backend().fetchQuiltLoaderVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<List<QuiltApiVersion>> fetchQuiltApiVersions(
            String gameVersion,
            Consumer<List<QuiltApiVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchQuiltApi"),
                () -> context.backend().fetchQuiltApiVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<InstalledVersion> downloadQuiltVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            QuiltApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            Runnable onSuccess,
            Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        return context.runDownload(UiText.tr("operation.installQuilt", instanceId),
                () -> context.backend().installQuiltVersion(
                        gameVersion,
                        instanceId,
                        loaderVersion,
                        apiVersion,
                        gameDirectory,
                        listener,
                        token
                ),
                installed -> {
                    LOGGER.info("Quilt 实例安装完成: {} / {} -> {}",
                            instanceId, loaderVersion, installed.gameDirectory());
                    context.sendLog(UiText.tr("log.download.installed.quilt",
                            instanceId, loaderVersion));
                    onSuccess.run();
                },
                onError);
    }

    public AsyncTaskHandle<List<ForgeVersion>> fetchForgeVersions(
            String gameVersion,
            Consumer<List<ForgeVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchForge"),
                () -> context.backend().fetchForgeVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<List<NeoForgeVersion>> fetchNeoForgeVersions(
            String gameVersion,
            Consumer<List<NeoForgeVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchNeoForge"),
                () -> context.backend().fetchNeoForgeVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<List<OptiFineVersion>> fetchOptiFineVersions(
            String gameVersion,
            Consumer<List<OptiFineVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchOptiFine"),
                () -> context.backend().fetchOptiFineVersions(gameVersion),
                onLoaded, onError);
    }

    public AsyncTaskHandle<InstalledVersion> downloadForgeVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion version,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            Runnable onSuccess,
            Consumer<String> onError) {
        return downloadExternalLoaderVersion("Forge", instanceId, gameDirectory,
                () -> context.backend().installForgeVersion(gameVersion, instanceId, version,
                        gameDirectory, listener, token),
                onSuccess, onError);
    }

    public AsyncTaskHandle<InstalledVersion> downloadNeoForgeVersion(
            String gameVersion,
            String instanceId,
            NeoForgeVersion version,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            Runnable onSuccess,
            Consumer<String> onError) {
        return downloadExternalLoaderVersion("NeoForge", instanceId, gameDirectory,
                () -> context.backend().installNeoForgeVersion(gameVersion, instanceId, version,
                        gameDirectory, listener, token),
                onSuccess, onError);
    }

    public AsyncTaskHandle<InstalledVersion> downloadForgeOptiFineVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            Runnable onSuccess,
            Consumer<String> onError) {
        return downloadExternalLoaderVersion("Forge + OptiFine", instanceId, gameDirectory,
                () -> context.backend().installForgeOptiFineVersion(
                        gameVersion, instanceId, forgeVersion, optiFineVersion,
                        gameDirectory, listener, token),
                onSuccess, onError);
    }

    private AsyncTaskHandle<InstalledVersion> downloadExternalLoaderVersion(
            String loaderName,
            String instanceId,
            Path gameDirectory,
            LoaderInstallSupplier supplier,
            Runnable onSuccess,
            Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        return context.runDownload(UiText.tr("operation.installLoader", loaderName, instanceId),
                supplier::install,
                installed -> {
                    LOGGER.info("{} 实例安装完成: {}", loaderName, instanceId);
                    context.sendLog(UiText.tr("log.download.installed.loader", loaderName, instanceId));
                    onSuccess.run();
                },
                onError);
    }

    @FunctionalInterface
    private interface LoaderInstallSupplier {
        InstalledVersion install() throws IOException, InterruptedException;
    }
}
