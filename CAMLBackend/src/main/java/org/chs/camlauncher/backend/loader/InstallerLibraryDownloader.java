package org.chs.camlauncher.backend.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.io.DownloadExecutor;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.metadata.model.Library;
import org.chs.camlauncher.backend.metadata.model.LibraryArtifact;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** 预下载 installer 依赖库；优先使用 installer 内嵌文件，失败后长期重试远程源。 */
final class InstallerLibraryDownloader {
    private static final Logger LOGGER = LogManager.getLogger(InstallerLibraryDownloader.class);

    private final FileDownloader fileDownloader;
    private final Duration retryDelay;
    private final int concurrency;
    private final List<DownloadSource> sources;
    private final InstallerArchive archive;

    InstallerLibraryDownloader(
            FileDownloader fileDownloader,
            Duration retryDelay,
            int concurrency,
            List<DownloadSource> sources,
            InstallerArchive archive
    ) {
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay");
        this.concurrency = concurrency;
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        this.archive = Objects.requireNonNull(archive, "archive");
    }

    void download(
            String loaderName,
            Path staging,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        List<DownloadItem> downloads = collect(staging);
        if (downloads.isEmpty()) {
            return;
        }

        DownloadListener.notifyStage(listener,
                DownloadStage.of("loader.libraries.download", loaderName));
        try {
            DownloadExecutor.download(
                    downloads,
                    concurrency,
                    listener,
                    (item, itemListener) -> fileDownloader.download(
                            item, itemListener, null, token),
                    token
            );
            return;
        } catch (IOException e) {
            LOGGER.warn("{} 安装器库首轮下载失败，将每隔 {} 秒继续尝试: {}",
                    loaderName, retryDelay.toSeconds(), rootCauseMessage(e));
        }

        int retryRound = 0;
        while (true) {
            awaitRetry(token);
            retryRound++;
            try {
                DownloadListener.notifyPlanRetry(listener);
                DownloadExecutor.download(
                        downloads,
                        concurrency,
                        listener,
                        (item, itemListener) -> fileDownloader.downloadOneRound(
                                item, itemListener, null, token),
                        token
                );
                return;
            } catch (IOException e) {
                LOGGER.warn("{} 安装器库第 {} 轮重试失败，将继续等待 {} 秒: {}",
                        loaderName, retryRound, retryDelay.toSeconds(),
                        rootCauseMessage(e));
            }
        }
    }

    private List<DownloadItem> collect(Path staging) throws IOException {
        Map<Path, DownloadItem> items = new LinkedHashMap<>();
        for (Library library : archive.libraries()) {
            String relativePath = LibraryArtifact.relativePath(library);
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }

            Path destination = SafePaths.resolveUnder(
                    staging.resolve("libraries"), relativePath, "installer library");
            String sha1 = LibraryArtifact.sha1(library);
            Long size = LibraryArtifact.size(library);
            if (archive.extractBundledLibrary(relativePath, destination, sha1, size)) {
                continue;
            }

            String url = LibraryArtifact.downloadUrl(library);
            if (url == null || url.isBlank()) {
                continue;
            }
            items.putIfAbsent(destination.toAbsolutePath().normalize(), new DownloadItem(
                    library.name(),
                    DownloadSources.candidates(sources, url),
                    destination,
                    sha1,
                    size == null ? -1 : size
            ));
        }
        return List.copyOf(items.values());
    }

    private void awaitRetry(DownloadCancellationToken token) throws InterruptedException {
        long remaining = retryDelay.toNanos();
        while (remaining > 0) {
            token.throwIfCancelled();
            long slice = Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(200));
            TimeUnit.NANOSECONDS.sleep(slice);
            remaining -= slice;
        }
        token.throwIfCancelled();
    }

    private static String rootCauseMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
