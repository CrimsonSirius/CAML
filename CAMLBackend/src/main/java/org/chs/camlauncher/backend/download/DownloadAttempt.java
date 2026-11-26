package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;

/** 针对单个候选地址的一次下载尝试，决定使用分片还是单流。 */
final class DownloadAttempt {
    private final HttpClient httpClient;
    private final Duration readTimeout;

    DownloadAttempt(HttpClient httpClient, Duration readTimeout) {
        this.httpClient = httpClient;
        this.readTimeout = readTimeout;
    }

    void download(
            URI uri,
            DownloadItem item,
            DownloadListener listener,
            DownloadValidator validator,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        token.throwIfCancelled();
        long expectedSize = item.size();
        if (expectedSize >= SegmentResumeState.MIN_SEGMENT_SIZE) {
            RangeProbe probe = probeRange(uri, item, token);
            if (probe.ranged()) {
                if (probe.totalSize() <= 0
                        || (expectedSize > 0 && expectedSize != probe.totalSize())) {
                    throw new IOException("服务器返回的文件大小与元数据不一致: " + uri);
                }
                if (probe.totalSize() >= SegmentResumeState.MIN_SEGMENT_SIZE) {
                    try {
                        new SegmentedDownload(httpClient, readTimeout).download(
                                uri, item, probe, listener, validator, token);
                        return;
                    } catch (InterruptedException e) {
                        DownloadIo.clearPartialQuietly(item, e);
                        throw e;
                    } catch (RangeUnsupportedException e) {
                        DownloadIo.clearPartial(item);
                    }
                }
            }
        }
        new SingleStreamDownload(httpClient, readTimeout).download(
                uri, item, listener, validator, token);
    }

    private RangeProbe probeRange(
            URI uri,
            DownloadItem item,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        token.throwIfCancelled();
        HttpRequest request = DownloadIo.requestBuilder(uri, item)
                .timeout(Duration.ofMinutes(2))
                .header("Range", "bytes=0-0")
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );
        try (InputStream ignored = response.body()) {
            token.throwIfCancelled();
            int status = response.statusCode();
            if (status == 206) {
                String contentRange = response.headers().firstValue("Content-Range").orElse("");
                Matcher matcher = DownloadIo.CONTENT_RANGE.matcher(contentRange);
                if (matcher.matches()) {
                    return new RangeProbe(
                            true,
                            Long.parseLong(matcher.group(3)),
                            DownloadIo.strongEtag(response),
                            DownloadIo.lastModified(response)
                    );
                }
                return new RangeProbe(false, -1, null, null);
            }
            if (status == 200 || status == 416) {
                return new RangeProbe(false, response.headers()
                        .firstValueAsLong("Content-Length").orElse(-1), null, null);
            }
            throw new IOException("HTTP " + status + " for " + uri);
        }
    }

    record RangeProbe(boolean ranged, long totalSize, String etag, String lastModified) {
    }

    static final class RangeUnsupportedException extends IOException {
        RangeUnsupportedException(String message) {
            super(message);
        }
    }
}
