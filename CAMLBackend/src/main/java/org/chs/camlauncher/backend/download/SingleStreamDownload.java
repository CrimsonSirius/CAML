package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/** 不支持分片时的普通单流下载。 */
final class SingleStreamDownload {
    private final HttpClient httpClient;
    private final Duration readTimeout;

    SingleStreamDownload(HttpClient httpClient, Duration readTimeout) {
        this.httpClient = httpClient;
        this.readTimeout = readTimeout;
    }

    void download(URI uri, DownloadItem item, DownloadListener listener,
                  DownloadValidator validator, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        token.throwIfCancelled();
        DownloadIo.clearPartial(item);
        Path destination = item.destination();
        Path part = DownloadIo.partialPath(destination);
        boolean moved = false;
        try {
            HttpRequest request = DownloadIo.requestBuilder(uri, item)
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            try (InputStream input = new ReadTimeoutInputStream(response.body(), readTimeout)) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP " + response.statusCode() + " for " + uri);
                }

                long expectedSize = item.size();
                long contentLength = response.headers()
                        .firstValueAsLong("Content-Length").orElse(-1L);
                if (expectedSize > 0 && contentLength >= 0 && expectedSize != contentLength) {
                    throw new IOException("Content-Length mismatch for " + item.label()
                            + ": expected " + expectedSize + ", got " + contentLength);
                }
                long total = expectedSize > 0 ? expectedSize : contentLength;
                long downloaded = 0;
                byte[] buffer = new byte[8192];
                int read;

                try (OutputStream output = Files.newOutputStream(
                        part,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                )) {
                    while ((read = input.read(buffer)) != -1) {
                        token.throwIfCancelled();
                        output.write(buffer, 0, read);
                        downloaded += read;
                        listener.onProgress(new DownloadListener.DownloadProgress(
                                item.label(), downloaded, total));
                    }
                }

                token.throwIfCancelled();
                if (contentLength >= 0 && downloaded != contentLength) {
                    throw new IOException("Incomplete response for " + item.label()
                            + ": expected " + contentLength + " bytes, got " + downloaded);
                }
                if (expectedSize > 0 && downloaded != expectedSize) {
                    throw new IOException("Size mismatch for " + item.label()
                            + ": expected " + expectedSize + " bytes, got " + downloaded);
                }
                DownloadIo.verify(part, item, validator);
                DownloadIo.move(part, destination);
                moved = true;
            }
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(part);
                } catch (IOException ignored) {
                    // 保留原始下载异常；取消时尤其不能因为清理失败而掩盖 InterruptedException。
                }
            }
        }
    }
}
