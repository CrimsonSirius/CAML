package org.chs.camlauncher.front.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.front.text.UiText;

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

    public FrontTask<ModSearchPage> searchMods(
            ModSearchQuery query,
            Consumer<ModSearchPage> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.searchMods", query.text()),
                () -> context.backend().searchMods(query), onLoaded, onError);
    }

    public FrontTask<ModProjectDetail> fetchModProject(
            ModSource source,
            String projectId,
            Consumer<ModProjectDetail> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchModProject", projectId),
                () -> context.backend().fetchModProject(source, projectId), onLoaded, onError);
    }

    public FrontTask<byte[]> loadModIcon(
            String iconUrl,
            Consumer<byte[]> onLoaded,
            Consumer<String> onError) {
        return context.runIcon(UiText.tr("operation.loadModIcon"),
                () -> context.backend().loadModIcon(iconUrl), onLoaded, onError);
    }

    public FrontTask<ModDownloadResult> downloadMod(
            ModFile file,
            Path gameDirectory,
            String instanceId,
            boolean fullVersionIsolation,
            DownloadListener listener,
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
                        listener
                ),
                onSuccess,
                onError);
    }

    public FrontTask<InstanceNameCheck> checkInstanceName(Path gameDirectory, String instanceId,
                                                          Consumer<InstanceNameCheck> onChecked,
                                                          Consumer<String> onError) {
        return context.runCheck(UiText.tr("operation.checkInstanceName"),
                () -> context.backend().checkInstanceName(gameDirectory, instanceId),
                onChecked, onError);
    }

    /** 异步获取官方版本清单中的正式版。 */
    public FrontTask<List<VersionManifest.Version>> fetchReleaseVersions(
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return fetchVersions(UiText.tr("download.category.release"),
                VersionManifest::releasesNewestFirst, onLoaded, onError);
    }

    /** 异步获取官方版本清单中的快照版。 */
    public FrontTask<List<VersionManifest.Version>> fetchSnapshotVersions(
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return fetchVersions(UiText.tr("download.category.snapshot"),
                VersionManifest::snapshotsNewestFirst, onLoaded, onError);
    }

    /** 异步获取官方版本清单中的远古版（Alpha/Beta）。 */
    public FrontTask<List<VersionManifest.Version>> fetchOldVersions(
            Consumer<List<VersionManifest.Version>> onLoaded,
            Consumer<String> onError) {
        return fetchVersions(UiText.tr("download.category.old"),
                VersionManifest::oldVersionsNewestFirst, onLoaded, onError);
    }

    private FrontTask<List<VersionManifest.Version>> fetchVersions(
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
    public FrontTask<InstalledVersion> downloadVersion(
            String versionId,
            String instanceId,
            Path gameDirectory,
            DownloadListener listener,
            Runnable onSuccess,
            Consumer<String> onError) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        return context.runDownload(UiText.tr("operation.installVanilla", instanceId),
                () -> context.backend().installVersion(versionId, instanceId, gameDirectory, listener),
                installed -> {
                    LOGGER.info("官方原版实例安装完成: {} / {} -> {}",
                            instanceId, versionId, installed.gameDirectory());
                    context.sendLog(UiText.tr("log.download.installed.vanilla", instanceId, versionId));
                    onSuccess.run();
                },
                onError);
    }

    public FrontTask<List<FabricLoaderVersion>> fetchFabricLoaderVersions(
            String gameVersion,
            Consumer<List<FabricLoaderVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchFabricLoader"),
                () -> context.backend().fetchFabricLoaderVersions(gameVersion),
                onLoaded, onError);
    }

    public FrontTask<List<FabricApiVersion>> fetchFabricApiVersions(
            String gameVersion,
            Consumer<List<FabricApiVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchFabricApi"),
                () -> context.backend().fetchFabricApiVersions(gameVersion),
                onLoaded, onError);
    }

    public FrontTask<InstalledVersion> downloadFabricVersion(
            String gameVersion,
            String instanceId,
            String loaderVersion,
            FabricApiVersion apiVersion,
            Path gameDirectory,
            DownloadListener listener,
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
                        listener
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

    public FrontTask<List<ForgeVersion>> fetchForgeVersions(
            String gameVersion,
            Consumer<List<ForgeVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchForge"),
                () -> context.backend().fetchForgeVersions(gameVersion),
                onLoaded, onError);
    }

    public FrontTask<List<NeoForgeVersion>> fetchNeoForgeVersions(
            String gameVersion,
            Consumer<List<NeoForgeVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchNeoForge"),
                () -> context.backend().fetchNeoForgeVersions(gameVersion),
                onLoaded, onError);
    }

    public FrontTask<List<OptiFineVersion>> fetchOptiFineVersions(
            String gameVersion,
            Consumer<List<OptiFineVersion>> onLoaded,
            Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchOptiFine"),
                () -> context.backend().fetchOptiFineVersions(gameVersion),
                onLoaded, onError);
    }

    public FrontTask<InstalledVersion> downloadForgeVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion version,
            Path gameDirectory,
            DownloadListener listener,
            Runnable onSuccess,
            Consumer<String> onError) {
        return downloadExternalLoaderVersion("Forge", instanceId, gameDirectory,
                () -> context.backend().installForgeVersion(gameVersion, instanceId, version,
                        gameDirectory, listener),
                onSuccess, onError);
    }

    public FrontTask<InstalledVersion> downloadNeoForgeVersion(
            String gameVersion,
            String instanceId,
            NeoForgeVersion version,
            Path gameDirectory,
            DownloadListener listener,
            Runnable onSuccess,
            Consumer<String> onError) {
        return downloadExternalLoaderVersion("NeoForge", instanceId, gameDirectory,
                () -> context.backend().installNeoForgeVersion(gameVersion, instanceId, version,
                        gameDirectory, listener),
                onSuccess, onError);
    }

    public FrontTask<InstalledVersion> downloadForgeOptiFineVersion(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener,
            Runnable onSuccess,
            Consumer<String> onError) {
        return downloadExternalLoaderVersion("Forge + OptiFine", instanceId, gameDirectory,
                () -> context.backend().installForgeOptiFineVersion(
                        gameVersion, instanceId, forgeVersion, optiFineVersion,
                        gameDirectory, listener),
                onSuccess, onError);
    }

    private FrontTask<InstalledVersion> downloadExternalLoaderVersion(
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
