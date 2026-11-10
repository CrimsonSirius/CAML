package org.chs.camlauncher.backend.download.source;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 按下载源候选顺序执行 HTTP 请求。
 *
 * <p>{@link #getText} / {@link #getBytes} 只在传输或 HTTP 状态失败时切换候选源；
 * 需要把无效 JSON/XML 或空结果也视为失败时，调用方必须使用
 * {@link #getParsedText} / {@link #getParsedBytes}。</p>
 */
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

    /** 解析文本响应；解析结果为空时继续尝试下一个候选源。 */
    public <T> Optional<T> getParsedText(URI uri, Duration timeout, String accept,
                                         ResponseParser<String, T> parser)
            throws IOException, InterruptedException {
        return getParsed(uri, timeout, accept, HttpResponse.BodyHandlers.ofString(), parser);
    }

    /** 解析二进制响应；解析结果为空时继续尝试下一个候选源。 */
    public <T> Optional<T> getParsedBytes(URI uri, Duration timeout, String accept,
                                          ResponseParser<byte[], T> parser)
            throws IOException, InterruptedException {
        return getParsed(uri, timeout, accept, HttpResponse.BodyHandlers.ofByteArray(), parser);
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
        throw new BackendException(BackendError.of(
                "network.requestFailed",
                original,
                lastUri == null ? "" : lastUri
        ), lastFailure);
    }

    private <T, R> Optional<R> getParsed(URI original, Duration timeout, String accept,
                                         HttpResponse.BodyHandler<T> bodyHandler,
                                         ResponseParser<T, R> parser)
            throws IOException, InterruptedException {
        Objects.requireNonNull(parser, "parser");
        IOException lastFailure = null;
        URI lastUri = null;
        boolean lastWasEmpty = false;
        for (URI candidate : DownloadSources.candidates(sources, original.toString())) {
            lastUri = candidate;
            try {
                Optional<R> parsed = parser.parse(send(candidate, timeout, accept, bodyHandler));
                if (parsed.isPresent()) {
                    return parsed;
                }
                lastFailure = null;
                lastWasEmpty = true;
            } catch (IOException e) {
                lastFailure = e;
                lastWasEmpty = false;
            } catch (RuntimeException e) {
                lastFailure = new BackendException(
                        BackendError.of("network.responseInvalid", candidate), e);
                lastWasEmpty = false;
            }
        }
        if (lastWasEmpty) {
            return Optional.empty();
        }
        throw new BackendException(BackendError.of(
                "network.requestFailed",
                original,
                lastUri == null ? "" : lastUri
        ), lastFailure);
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
        CompletableFuture<HttpResponse<T>> future = httpClient.sendAsync(
                builder.build(), bodyHandler);
        HttpResponse<T> response;
        try {
            response = future.get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BackendException(
                    BackendError.of("network.timeout", timeout.toMillis(), uri), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BackendException(BackendError.of("network.requestFailed", uri, uri), cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BackendException(
                    BackendError.of("network.httpError", response.statusCode(), uri));
        }
        return response.body();
    }

    @FunctionalInterface
    public interface ResponseParser<T, R> {
        Optional<R> parse(T body) throws IOException;
    }
}
