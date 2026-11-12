package org.chs.camlauncher.backend.mod.service;

import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Mod 图标下载与磁盘缓存；图标只作为二进制数据返回给前端。
 *
 * <p>这里只执行一次网络尝试。重试和并发调度由前端任务层负责，避免失败的图标
 * 长时间占住下载线程并阻塞后续图标显示。</p>
 */
public final class ModIconService {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final Path cacheDirectory;
    private final HttpClient httpClient;

    public ModIconService(Path dataDirectory) {
        this.cacheDirectory = dataDirectory.resolve("cache").resolve("mod-icons");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
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
        if (Files.isRegularFile(cached)) {
            if (Files.size(cached) > 0) {
                return Files.readAllBytes(cached);
            }
            Files.deleteIfExists(cached);
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " for " + uri);
        }
        byte[] bytes = response.body();
        if (bytes == null || bytes.length == 0) {
            throw new BackendException(BackendError.of("mod.error.iconEmpty", iconUrl));
        }
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (contentLength >= 0 && bytes.length != contentLength) {
            throw new IOException("Incomplete icon response for " + iconUrl
                    + ": expected " + contentLength + " bytes, got " + bytes.length);
        }
        writeCache(cached, bytes);
        return bytes;
    }

    private void writeCache(Path cached, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(
                cacheDirectory,
                cached.getFileName().toString() + "-",
                ".tmp"
        );
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, cached,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
