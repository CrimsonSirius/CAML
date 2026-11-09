package org.chs.camlauncher.backend.download.source;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** 按下载源候选顺序执行 HTTP 请求。 */
public final class SourceAwareHttpClient {
    private static final String USER_AGENT = "CAML/1.0";

    private final HttpClient httpClient;
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

    public SourceAwareHttpClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    }

    public String getText(URI uri, Duration timeout, String accept) throws IOException, InterruptedException {
        return get(uri, timeout, accept, HttpResponse.BodyHandlers.ofString());
    }

    public byte[] getBytes(URI uri, Duration timeout, String accept)
            throws IOException, InterruptedException {
        return get(uri, timeout, accept, HttpResponse.BodyHandlers.ofByteArray());
    }

    private <T> T get(URI original, Duration timeout, String accept,
                      HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        IOException lastFailure = null;
        URI lastUri = null;
        for (URI candidate : DownloadSources.candidates(sources, original.toString())) {
            lastUri = candidate;
            try {
                return send(candidate, timeout, accept, bodyHandler);
            } catch (IOException e) {
                lastFailure = e;
            }
        }
        throw new IOException("无法请求 " + original + "（最后地址: " + lastUri + "）", lastFailure);
    }

    private <T> T send(URI uri, Duration timeout, String accept,
                       HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET();
        if (accept != null && !accept.isBlank()) {
            builder.header("Accept", accept);
        }
        HttpResponse<T> response = httpClient.send(builder.build(), bodyHandler);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + uri);
        }
        return response.body();
    }
}
