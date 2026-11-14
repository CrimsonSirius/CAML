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
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.mod.source.CurseForgeClient;
import org.chs.camlauncher.backend.mod.source.ModrinthClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 聚合 Modrinth 与 CurseForge 的搜索、详情和文件下载。 */
public final class ModService {
    private static final Logger LOGGER = LogManager.getLogger(ModService.class);
    private final ModrinthClient modrinth;
    private final CurseForgeClient curseForge;
    private final FileDownloader fileDownloader;

    public ModService(ModrinthClient modrinth, CurseForgeClient curseForge,
                      FileDownloader fileDownloader) {
        this.modrinth = Objects.requireNonNull(modrinth, "modrinth");
        this.curseForge = Objects.requireNonNull(curseForge, "curseForge");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
    }

    public boolean curseForgeConfigured() {
        return curseForge.configured();
    }

    public ModSearchPage search(ModSearchQuery query) throws IOException, InterruptedException {
        ModSearchQuery normalized = normalize(query);
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

    public ModDownloadResult download(
            ModFile file,
            Path gameDirectory,
            String instanceId,
            VersionIsolationMode isolationMode,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(isolationMode, "isolationMode");
        if (!file.hasDownloadUri()) {
            throw new BackendException(BackendError.of("mod.error.downloadUrlMissing", file.name()));
        }
        String instance = InstanceNameRules.validate(instanceId);
        Path mods = modsDirectory(gameDirectory, instance, isolationMode);
        Path destination = SafePaths.resolveChild(mods, file.fileName(), "mod file");
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
        fileDownloader.download(item, listener, validator, new DownloadCancellationToken());
        LOGGER.info("Mod 下载完成: {} -> {}", file.name(), destination);
        return new ModDownloadResult(destination, file);
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
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(isolationMode, "isolationMode");
        String instance = InstanceNameRules.validate(instanceId);
        Path versionDirectory = SafePaths.resolveChild(
                gameDirectory.resolve("versions"), instance, "instance id");
        Path dataDirectory = GameDataLayout.resolve(gameDirectory, versionDirectory, isolationMode);
        return dataDirectory.resolve("mods");
    }

    private ModSearchPage searchAll(ModSearchQuery query) throws IOException, InterruptedException {
        boolean curseConfigured = curseForgeConfigured();
        int perSource = curseConfigured ? Math.max(1, query.pageSize() / 2) : query.pageSize();
        ModSearchQuery sourceQuery = new ModSearchQuery(
                query.text(), null, query.gameVersion(), query.loader(),
                query.pageIndex(), perSource);
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
                Math.max(1, Math.min(50, query.pageSize()))
        );
    }
}
