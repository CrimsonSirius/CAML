package org.chs.camlauncher.backend.mod.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadValidator;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.GameDataLayout;
import org.chs.camlauncher.backend.instance.InstanceInstallLock;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.io.ArchiveExtractionGuard;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModContentType;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.mod.source.CurseForgeClient;
import org.chs.camlauncher.backend.mod.source.ModrinthClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 聚合 Modrinth 与 CurseForge 的搜索、详情和文件下载。 */
public final class ModService {
    private static final Logger LOGGER = LogManager.getLogger(ModService.class);
    private final ModrinthClient modrinth;
    private final CurseForgeClient curseForge;
    private final FileDownloader fileDownloader;
    private volatile int archiveMaxEntries = ArchiveExtractionGuard.DEFAULT_MAX_ENTRIES;
    private volatile long archiveMaxEntryBytes = ArchiveExtractionGuard.DEFAULT_MAX_ENTRY_BYTES;
    private volatile long archiveMaxTotalBytes = ArchiveExtractionGuard.DEFAULT_MAX_TOTAL_BYTES;

    public ModService(ModrinthClient modrinth, CurseForgeClient curseForge,
                      FileDownloader fileDownloader) {
        this.modrinth = Objects.requireNonNull(modrinth, "modrinth");
        this.curseForge = Objects.requireNonNull(curseForge, "curseForge");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
    }

    public boolean curseForgeConfigured() {
        return curseForge.configured();
    }

    public void setArchiveExtractionLimits(int maxEntries, long maxEntryBytes, long maxTotalBytes) {
        new ArchiveExtractionGuard(maxEntries, maxEntryBytes, maxTotalBytes);
        this.archiveMaxEntries = maxEntries;
        this.archiveMaxEntryBytes = maxEntryBytes;
        this.archiveMaxTotalBytes = maxTotalBytes;
    }

    public ModSearchPage search(ModSearchQuery query) throws IOException, InterruptedException {
        ModSearchQuery normalized = normalize(query);
        if (!normalized.type().supportsModrinth()) {
            if (normalized.source() == ModSource.MODRINTH) {
                return new ModSearchPage(List.of(), normalized.pageIndex(),
                        normalized.pageSize(), 0, false);
            }
            return curseForge.search(normalized);
        }
        if (!normalized.type().supportsCurseForge()) {
            if (normalized.source() == ModSource.CURSEFORGE) {
                return new ModSearchPage(List.of(), normalized.pageIndex(),
                        normalized.pageSize(), 0, false);
            }
            return modrinth.search(normalized);
        }
        if (normalized.source() == ModSource.MODRINTH) {
            return modrinth.search(normalized);
        }
        if (normalized.source() == ModSource.CURSEFORGE) {
            return curseForge.search(normalized);
        }
        return searchAll(normalized);
    }

    public ModProjectDetail project(ModSource source, String projectId)
            throws IOException, InterruptedException {
        Objects.requireNonNull(source, "source");
        return switch (source) {
            case MODRINTH -> modrinth.project(projectId);
            case CURSEFORGE -> curseForge.project(projectId);
        };
    }

    public Map<String, ModFile> latestVersions(
            List<String> hashes, String gameVersion, org.chs.camlauncher.backend.mod.model.ModLoader loader)
            throws IOException, InterruptedException {
        return modrinth.latestVersions(hashes, gameVersion, loader);
    }

    public Map<Long, String> curseForgeFingerprintModIds(List<Long> fingerprints)
            throws IOException, InterruptedException {
        return curseForge.fingerprintModIds(fingerprints);
    }

    public Map<Long, ModFile> latestCurseForgeVersions(
            Map<Long, String> fingerprintToModId, String gameVersion,
            org.chs.camlauncher.backend.mod.model.ModLoader loader)
            throws IOException, InterruptedException {
        return curseForge.latestVersions(fingerprintToModId, gameVersion, loader);
    }

    public ModDownloadResult download(
            ModFile file,
            Path gameDirectory,
            String instanceId,
            VersionIsolationMode isolationMode,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return download(file, ModContentType.MOD, gameDirectory, instanceId,
                isolationMode, listener, new DownloadCancellationToken());
    }

    public ModDownloadResult download(
            ModFile file,
            ModContentType type,
            Path gameDirectory,
            String instanceId,
            VersionIsolationMode isolationMode,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return download(file, type, gameDirectory, instanceId, isolationMode, listener,
                new DownloadCancellationToken());
    }

    public ModDownloadResult download(
            ModFile file,
            ModContentType type,
            Path gameDirectory,
            String instanceId,
            VersionIsolationMode isolationMode,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(isolationMode, "isolationMode");
        Objects.requireNonNull(token, "token");
        String instance = InstanceNameRules.validate(instanceId);
        try (InstanceInstallLock ignored = InstanceInstallLock.acquire(gameDirectory, instance)) {
            if (type == ModContentType.WORLD) {
                return downloadWorld(file, gameDirectory, instance, isolationMode, listener, token);
            }
            if (!file.hasDownloadUri()) {
                throw new BackendException(
                        BackendError.of("mod.error.downloadUrlMissing", file.name()));
            }
            Path contentDirectory = contentDirectory(
                    gameDirectory, instance, isolationMode, type);
            Path destination = SafePaths.resolveChild(
                    contentDirectory, file.fileName(), type.directoryName() + " file");
            downloadTo(file, destination, listener, token);
            String contentType = switch (type) {
                case MOD -> "Mod";
                case MODPACK -> "整合包";
                case SHADER -> "光影包";
                case RESOURCE_PACK -> "资源包";
                case WORLD -> "地图";
            };
            LOGGER.info("{} 下载完成: {} -> {}", contentType, file.name(), destination);
            return new ModDownloadResult(destination, file);
        }
    }

    /** 把 Mod 文件下载到指定目标路径；调用方必须持有对应实例的写锁。 */
    public Path downloadTo(
            ModFile file,
            Path destination,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(token, "token");
        if (!file.hasDownloadUri()) {
            throw new BackendException(BackendError.of("mod.error.downloadUrlMissing", file.name()));
        }
        Map<String, String> headers = file.source() == ModSource.CURSEFORGE
                ? curseForge.downloadHeaders()
                : Map.of();
        DownloadItem item = new DownloadItem(
                file.name(),
                file.downloadUris(),
                destination,
                file.sha1(),
                file.size(),
                headers
        );
        DownloadValidator validator = md5Validator(file);
        fileDownloader.download(item, listener, validator, token);
        return destination;
    }

    private ModDownloadResult downloadWorld(
            ModFile file,
            Path gameDirectory,
            String instanceId,
            VersionIsolationMode isolationMode,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        if (!file.hasDownloadUri()) {
            throw new BackendException(BackendError.of("mod.error.downloadUrlMissing", file.name()));
        }
        String instance = InstanceNameRules.validate(instanceId);
        Path savesDirectory = contentDirectory(
                gameDirectory, instance, isolationMode, ModContentType.WORLD);
        Files.createDirectories(savesDirectory);
        Path archive = savesDirectory.resolve(".caml-world-" + UUID.randomUUID() + ".zip");
        try {
            Map<String, String> headers = file.source() == ModSource.CURSEFORGE
                    ? curseForge.downloadHeaders()
                    : Map.of();
            DownloadItem item = new DownloadItem(
                    file.name(),
                    file.downloadUris(),
                    archive,
                    file.sha1(),
                    file.size(),
                    headers
            );
            fileDownloader.download(item, listener, md5Validator(file), token);
            Path installed = new WorldInstaller(new ArchiveExtractionGuard(
                    archiveMaxEntries, archiveMaxEntryBytes, archiveMaxTotalBytes))
                    .install(archive, savesDirectory, file.fileName(), token);
            LOGGER.info("地图下载完成: {} -> {}", file.name(), installed);
            return new ModDownloadResult(installed, file);
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    private static DownloadValidator md5Validator(ModFile file) {
        if (hasText(file.sha1()) || !hasText(file.md5())) {
            return null;
        }
        return path -> {
            if (!HashVerifier.matchesMd5(path, file.md5())) {
                throw new IOException("MD5 mismatch for " + file.name());
            }
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public Path modsDirectory(Path gameDirectory, String instanceId,
                              VersionIsolationMode isolationMode) {
        return contentDirectory(gameDirectory, instanceId, isolationMode, ModContentType.MOD);
    }

    public Path contentDirectory(Path gameDirectory, String instanceId,
                                 VersionIsolationMode isolationMode, ModContentType type) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(isolationMode, "isolationMode");
        Objects.requireNonNull(type, "type");
        String instance = InstanceNameRules.validate(instanceId);
        Path versionDirectory = SafePaths.resolveChild(
                gameDirectory.resolve("versions"), instance, "instance id");
        Path dataDirectory = GameDataLayout.resolve(gameDirectory, versionDirectory, isolationMode);
        return dataDirectory.resolve(type.directoryName());
    }

    private ModSearchPage searchAll(ModSearchQuery query) throws IOException, InterruptedException {
        boolean curseConfigured = curseForgeConfigured();
        int perSource = curseConfigured ? Math.max(1, query.pageSize() / 2) : query.pageSize();
        ModSearchQuery sourceQuery = new ModSearchQuery(
                query.text(), null, query.gameVersion(), query.loader(),
                query.pageIndex(), perSource, query.type());
        ModSearchPage modrinthPage = modrinth.search(sourceQuery);
        ModSearchPage cursePage = null;
        if (curseConfigured) {
            try {
                cursePage = curseForge.search(sourceQuery);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("CurseForge 搜索失败，仅显示 Modrinth 结果", e);
            }
        }

        List<ModProjectSummary> items = new ArrayList<>(modrinthPage.items());
        if (cursePage != null) {
            items.addAll(cursePage.items());
        }
        items.sort(Comparator.comparingLong(ModProjectSummary::downloads).reversed());
        int total = modrinthPage.totalCount() + (cursePage == null ? 0 : cursePage.totalCount());
        boolean hasNext = modrinthPage.hasNext() || (cursePage != null && cursePage.hasNext());
        return new ModSearchPage(List.copyOf(items), query.pageIndex(), perSource, total, hasNext);
    }

    private static ModSearchQuery normalize(ModSearchQuery query) {
        Objects.requireNonNull(query, "query");
        return new ModSearchQuery(
                query.text() == null ? "" : query.text().trim(),
                query.source(),
                query.gameVersion() == null ? "" : query.gameVersion().trim(),
                query.loader(),
                Math.max(0, query.pageIndex()),
                Math.max(1, Math.min(50, query.pageSize())),
                query.type()
        );
    }
}
