package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** 按候选地址执行下载重试，并把单次下载委托给 {@link DownloadAttempt}。 */
public final class FileDownloader {
    private static final int DEFAULT_MAX_ATTEMPTS = 6;
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private final int maxAttempts;
    private final DownloadAttempt attempt;

    public FileDownloader() {
        this(DownloadIo.defaultClient(), DEFAULT_MAX_ATTEMPTS, DEFAULT_READ_TIMEOUT);
    }

    public FileDownloader(HttpClient httpClient, int maxAttempts) {
        this(httpClient, maxAttempts, DEFAULT_READ_TIMEOUT);
    }

    public FileDownloader(HttpClient httpClient, int maxAttempts, Duration readTimeout) {
        Objects.requireNonNull(httpClient, "httpClient");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
        Objects.requireNonNull(readTimeout, "readTimeout");
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("readTimeout must be positive");
        }
        this.attempt = new DownloadAttempt(httpClient, readTimeout);
    }

    public void download(DownloadItem item, DownloadListener listener)
            throws IOException, InterruptedException {
        download(item, listener, null);
    }

    public void download(DownloadItem item, DownloadListener listener,
                         DownloadValidator validator)
            throws IOException, InterruptedException {
        download(item, listener, validator, new DownloadCancellationToken());
    }

    public void download(DownloadItem item, DownloadListener listener,
                         DownloadValidator validator, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        download(item, listener, validator, token, maxAttempts);
    }

    /** 按当前候选顺序让每个地址各尝试一次；供调用方实现自己的长期重试策略。 */
    public void downloadOneRound(DownloadItem item, DownloadListener listener,
                                 DownloadValidator validator, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        download(item, listener, validator, token, item.candidates().size());
    }

    private void download(DownloadItem item, DownloadListener listener,
                          DownloadValidator validator, DownloadCancellationToken token,
                          int attemptLimit)
            throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        if (attemptLimit < 1) {
            throw new IllegalArgumentException("attemptLimit must be positive");
        }
        token.throwIfCancelled();
        DownloadListener progress = listener == null ? DownloadListener.NONE : listener;
        if (item.existsAndValid()
                && DownloadIo.validatesExistingFile(item.destination(), validator)) {
            progress.onProgress(new DownloadListener.DownloadProgress(
                    item.label(), item.size(), item.size()));
            return;
        }

        Path parent = item.destination().toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        IOException lastFailure = null;
        URI lastUri = null;
        int attempted = 0;
        int round = 0;
        while (attempted < attemptLimit) {
            token.throwIfCancelled();
            for (URI uri : item.candidates()) {
                if (attempted >= attemptLimit) {
                    break;
                }
                token.throwIfCancelled();
                attempted++;
                lastUri = uri;
                try {
                    attempt.download(uri, item, progress, validator, token);
                    return;
                } catch (IOException e) {
                    lastFailure = e;
                }
            }
            round++;
            if (attempted < attemptLimit) {
                Thread.sleep(200L * round);
                token.throwIfCancelled();
            }
        }

        throw new BackendException(BackendError.of(
                "download.error.failed",
                item.label(),
                attempted,
                lastUri == null ? "" : lastUri
        ), lastFailure);
    }
}
