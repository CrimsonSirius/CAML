package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.backend.download.io.DownloadExecutor;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.model.DownloadPlan;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.metadata.model.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.resolve.ManifestResolver;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.io.SafePaths;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class VersionInstaller {
    private final MetadataService metadata;
    private final VersionManifestService manifestService;
    private final DownloadPlanner downloadPlanner;
    private final AssetDownloadPlanner assetDownloadPlanner;
    private final FileDownloader fileDownloader;
    private final ManifestResolver manifestResolver = new ManifestResolver();
    private volatile int downloadConcurrency = DownloadConcurrency.DEFAULT;

    public VersionInstaller() {
        this(
                new MetadataService(),
                new VersionManifestService(),
                new DownloadPlanner(),
                new AssetDownloadPlanner(),
                new FileDownloader()
        );
    }

    public VersionInstaller(
            MetadataService metadata,
            VersionManifestService manifestService,
            DownloadPlanner downloadPlanner,
            AssetDownloadPlanner assetDownloadPlanner,
            FileDownloader fileDownloader
    ) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.manifestService = Objects.requireNonNull(manifestService, "manifestService");
        this.downloadPlanner = Objects.requireNonNull(downloadPlanner, "downloadPlanner");
        this.assetDownloadPlanner = Objects.requireNonNull(assetDownloadPlanner, "assetDownloadPlanner");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
    }

    /**
     * 确保版本可用，并返回启动所需的 {@link InstalledVersion}。
     *
     * <p>解析顺序：</p>
     * <ol>
     *     <li>本地已存在 {@code versions/<id>/<id>.json} 时直接使用本地版本。
     *         此时版本 ID 只是 {@code versions/} 下的文件夹名，可以是第三方启动器
     *         或整合包自定义的名字（例如 {@code 1.21.4-DeepSeek}）,
     *         不需要存在于官方版本清单中。</li>
     *     <li>本地不存在时才回退到官方版本清单下载安装。</li>
     * </ol>
     *
     * <p>两条分支都会重新生成下载计划并补齐缺失文件,
     * 因此本地版本缺库或缺资源时也会被自动修复。</p>
     */
    public InstalledVersion install(
            String versionId,
            Path gameDirectory,
            URI manifestUri,
            Path manifestCacheFile,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return install(versionId, versionId, gameDirectory, manifestUri, manifestCacheFile, listener);
    }

    public InstalledVersion install(
            String versionId,
            Path gameDirectory,
            URI manifestUri,
            Path manifestCacheFile,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        return install(versionId, versionId, gameDirectory, manifestUri, manifestCacheFile,
                listener, token);
    }

    /**
     * 使用单独的原版版本 ID 和实例 ID 安装。
     *
     * <p>官方版本 ID 用于查询远程元数据，实例 ID 用作 {@code versions/<id>} 目录名。
     * 因此实例可以命名为“我的生存”，同时仍下载官方 {@code 1.21.4}。</p>
     */
    public InstalledVersion install(
            String versionId,
            String instanceId,
            Path gameDirectory,
            URI manifestUri,
            Path manifestCacheFile,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return install(versionId, instanceId, gameDirectory, manifestUri, manifestCacheFile,
                listener, new DownloadCancellationToken());
    }

    public InstalledVersion install(
            String versionId,
            String instanceId,
            Path gameDirectory,
            URI manifestUri,
            Path manifestCacheFile,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        Objects.requireNonNull(versionId, "versionId");
        instanceId = validateInstanceId(instanceId);
        DownloadListener.notifyStage(listener, DownloadStage.of("version.fetch"));
        Path localVersionFile = versionFile(instanceId, gameDirectory);

        // 1. 本地已安装：直接读取本地版本 JSON。
        if (Files.isRegularFile(localVersionFile)) {
            DownloadListener.notifyStage(listener, DownloadStage.of("version.checkLocal"));
            String localJson = Files.readString(localVersionFile, StandardCharsets.UTF_8);
            return complete(instanceId, gameDirectory, localJson, listener, token, false);
        }

        // 2. 本地没有该版本，才需要访问官方版本清单。
        VersionManifest manifest = manifestService.fetchManifestOrCache(manifestUri, manifestCacheFile);
        VersionManifest.Version remoteVersion = manifest.versions().stream()
                .filter(version -> versionId.equals(version.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown version: " + versionId
                                + " (本地未安装，官方版本清单中也不存在该版本 ID)"));

        String versionJson = manifestService.fetchVersionJson(remoteVersion);
        return completeNewInstance(instanceId, gameDirectory, versionJson, listener, token);
    }

    /**
     * 直接安装给定的版本 JSON，例如 Fabric Meta 返回的 loader profile。
     */
    public InstalledVersion installProfile(
            String instanceId,
            Path gameDirectory,
            String versionJson,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return installProfile(instanceId, gameDirectory, versionJson, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion installProfile(
            String instanceId,
            Path gameDirectory,
            String versionJson,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        instanceId = validateInstanceId(instanceId);
        InstanceNameRules.ensureNewInstance(gameDirectory, instanceId);
        Objects.requireNonNull(versionJson, "versionJson");
        return completeNewInstance(instanceId, gameDirectory, versionJson, listener, token);
    }

    private InstalledVersion completeNewInstance(
            String instanceId,
            Path gameDirectory,
            String versionJson,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Path versionDirectory = SafePaths.resolveChild(
                gameDirectory.resolve("versions"), instanceId, "version id");
        boolean existed = Files.exists(versionDirectory);
        try {
            return complete(instanceId, gameDirectory, versionJson, listener, token, true);
        } catch (IOException | RuntimeException | InterruptedException e) {
            if (!existed) {
                deleteRecursively(versionDirectory);
            }
            throw e;
        }
    }

    /**
     * 统一的收尾流程：解析合并、生成下载计划、补齐文件并返回安装结果。
     *
     * @param writeVersionJson 只有远程安装才需要写入版本 JSON，本地版本保持原文件不变
     */
    private InstalledVersion complete(
            String versionId,
            Path gameDirectory,
            String versionJson,
            DownloadListener listener,
            DownloadCancellationToken token,
            boolean writeVersionJson
    ) throws IOException, InterruptedException {
        token.throwIfCancelled();
        VersionInfo parsed = metadata.parseVersionInfo(versionJson);
        VersionInfo resolved = resolve(parsed, gameDirectory, versionId);

        Path versionsDirectory = gameDirectory.resolve("versions");
        Path versionDirectory = SafePaths.resolveChild(versionsDirectory, versionId, "version id");
        Path versionFile = SafePaths.resolveChild(
                versionDirectory, versionId + ".json", "version json");
        String clientJarId = resolveClientJarId(parsed, resolved, versionDirectory, versionId);
        Path clientJarDirectory = SafePaths.resolveChild(
                versionsDirectory, clientJarId, "client jar id");
        Path clientJar = SafePaths.resolveChild(
                clientJarDirectory, clientJarId + ".jar", "client jar");
        Path librariesDirectory = gameDirectory.resolve("libraries");
        Path assetsDirectory = gameDirectory.resolve("assets");

        token.throwIfCancelled();
        DownloadListener.notifyStage(listener, DownloadStage.of("version.downloadFiles"));
        downloadPlan(downloadPlanner.plan(resolved, gameDirectory, clientJarId), listener, token);

        Path assetIndexFile = null;
        if (resolved.assetIndex() != null) {
            token.throwIfCancelled();
            DownloadListener.notifyStage(listener, DownloadStage.of("version.downloadAssets"));
            assetIndexFile = SafePaths.resolveChild(
                    assetsDirectory.resolve("indexes"),
                    resolved.assetIndex().id() + ".json",
                    "asset index");
            AssetIndexFile index = metadata.readAssetIndex(assetIndexFile);
            downloadPlan(assetDownloadPlanner.plan(index, assetsDirectory), listener, token);
        }

        if (writeVersionJson) {
            token.throwIfCancelled();
            DownloadListener.notifyStage(listener, DownloadStage.of("version.writeJson"));
            writeAtomically(versionFile, versionJson);
        }

        return new InstalledVersion(
                versionId,
                resolved,
                gameDirectory,
                versionDirectory,
                versionFile,
                clientJar,
                librariesDirectory,
                assetsDirectory,
                assetIndexFile
        );
    }

    /** @return {@code versions/<id>/<id>.json} 的标准路径 */
    private static Path versionFile(String versionId, Path gameDirectory) {
        Path versionsDirectory = gameDirectory.resolve("versions");
        Path versionDirectory = SafePaths.resolveChild(versionsDirectory, versionId, "version id");
        return SafePaths.resolveChild(versionDirectory, versionId + ".json", "version json");
    }

    private void downloadPlan(DownloadPlan plan, DownloadListener listener,
                              DownloadCancellationToken token)
            throws IOException, InterruptedException {
        DownloadExecutor.download(
                plan.missing(),
                downloadConcurrency,
                listener,
                (item, itemListener) -> fileDownloader.download(item, itemListener, null, token),
                token
        );
    }

    public void setDownloadConcurrency(int downloadConcurrency) {
        this.downloadConcurrency = DownloadConcurrency.clamp(downloadConcurrency);
    }

    private static String resolveClientJarId(
            VersionInfo parsed,
            VersionInfo resolved,
            Path versionDirectory,
            String versionId
    ) {
        if (parsed.jar() != null && !parsed.jar().isBlank()) {
            return parsed.jar();
        }
        Path ownJar = SafePaths.resolveChild(versionDirectory, versionId + ".jar", "client jar");
        if (Files.isRegularFile(ownJar)) {
            return versionId;
        }
        return resolved.clientJarId();
    }

    private static String validateInstanceId(String instanceId) {
        return InstanceNameRules.validate(instanceId);
    }

    /**
     * 合并 {@code inheritsFrom} 父版本，并把 manifest 的 id 统一为文件夹名。
     *
     * <p>启动时使用的版本 ID 来自文件夹名，因此即使 JSON 内部保留的是原版 ID,
     * 也要改成文件夹名，保证 {@code versions/<id>/<id>.jar} 等路径一致。</p>
     */
    private VersionInfo resolve(VersionInfo version, Path gameDirectory, String versionId) throws IOException {
        VersionInfo resolved = version.inheritsFrom() == null
                ? version
                : resolveInheritance(version, gameDirectory);
        return resolved.withId(versionId);
    }

    /** 逐级读取本地父版本并合并。 */
    private VersionInfo resolveInheritance(VersionInfo version, Path gameDirectory) throws IOException {
        Map<String, VersionInfo> parents = new LinkedHashMap<>();
        VersionInfo current = version;
        while (current.inheritsFrom() != null) {
            String parentId = current.inheritsFrom();
            if (parents.containsKey(parentId)) {
                throw new IllegalArgumentException("Circular inheritance: " + parentId);
            }

            Path parentFile = versionFile(parentId, gameDirectory);
            if (!Files.isRegularFile(parentFile)) {
                throw new IOException("Parent version is not installed: " + parentId);
            }

            VersionInfo parent = metadata.readVersionInfo(parentFile);
            // 按文件夹名索引：inheritsFrom 指向文件夹名,
            // 部分第三方版本 JSON 内部的 id 与文件夹名并不一致。
            parents.put(parentId, parent);
            current = parent;
        }
        return manifestResolver.resolve(version, parents);
    }

    private static void writeAtomically(Path file, String text) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporary = Files.createTempFile(parent, "caml-version-", ".part");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 失败清理不能覆盖原始安装异常。
                }
            }
        } catch (IOException ignored) {
            // 失败清理不能覆盖原始安装异常。
        }
    }
}
