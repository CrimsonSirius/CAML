package org.chs.camlauncher.backend.mod.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Mod 图标下载与磁盘缓存；图标只作为二进制数据返回给前端。 */
public final class ModIconService {
    private static final Logger LOGGER = LogManager.getLogger(ModIconService.class);
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MILLIS = 5_000;
    private final Path cacheDirectory;
    private final FileDownloader fileDownloader;

    public ModIconService(Path dataDirectory) {
        this.cacheDirectory = dataDirectory.resolve("cache").resolve("mod-icons");
        this.fileDownloader = new FileDownloader(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(15))
                        .build(),
                1,
                Duration.ofSeconds(30)
        );
    }

    public byte[] load(String iconUrl) throws IOException, InterruptedException {
        if (iconUrl == null || iconUrl.isBlank()) {
            return new byte[0];
        }
        URI uri;
        try {
            uri = URI.create(iconUrl);
        } catch (IllegalArgumentException e) {
            throw new BackendException(BackendError.of("mod.error.iconUrlInvalid", iconUrl), e);
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))) {
            throw new BackendException(BackendError.of("mod.error.iconUrlInvalid", iconUrl));
        }

        Files.createDirectories(cacheDirectory);
        String key = HashVerifier.sha1(iconUrl.getBytes(StandardCharsets.UTF_8));
        Path cached = cacheDirectory.resolve(key + ".img");
        IOException lastFailure = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (!Files.isRegularFile(cached) || Files.size(cached) == 0) {
                    fileDownloader.download(new DownloadItem(
                            "mod icon",
                            List.of(uri),
                            cached,
                            null,
                            -1
                    ), DownloadListener.NONE, null, new DownloadCancellationToken());
                }
                if (Files.size(cached) > 0) {
                    return Files.readAllBytes(cached);
                }
                throw new BackendException(BackendError.of("mod.error.iconEmpty", iconUrl));
            } catch (IOException e) {
                lastFailure = e;
                Files.deleteIfExists(cached);
                if (attempt < MAX_RETRIES) {
                    LOGGER.warn("Mod 图标下载失败，{}ms 后重试（{}/{}） url={}",
                            RETRY_DELAY_MILLIS, attempt + 1, MAX_RETRIES, iconUrl, e);
                    Thread.sleep(RETRY_DELAY_MILLIS);
                }
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new BackendException(BackendError.of("mod.error.iconEmpty", iconUrl));
    }
}
