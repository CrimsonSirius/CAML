package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Objects;

public final class FileDownloader {
    private static final int DEFAULT_MAX_ATTEMPTS = 6;

    private final HttpClient httpClient;
    private final int maxAttempts;

    public FileDownloader() {
        this(defaultClient(), DEFAULT_MAX_ATTEMPTS);
    }

    public FileDownloader(HttpClient httpClient, int maxAttempts) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
    }

    public void download(DownloadItem item, DownloadListener listener)
            throws IOException, InterruptedException {
        download(item, listener, null);
    }

    public void download(DownloadItem item, DownloadListener listener,
                         DownloadValidator validator)
            throws IOException, InterruptedException {
        DownloadListener progress = listener == null ? DownloadListener.NONE : listener;
        if (item.existsAndValid()) {
            progress.onProgress(new DownloadListener.DownloadProgress(item.label(), item.size(), item.size()));
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
        while (attempted < maxAttempts) {
            for (URI uri : item.candidates()) {
                if (attempted >= maxAttempts) {
                    break;
                }
                attempted++;
                lastUri = uri;
                try {
                    downloadOnce(uri, item, progress, validator);
                    return;
                } catch (IOException e) {
                    lastFailure = e;
                }
            }
            round++;
            if (attempted < maxAttempts) {
                Thread.sleep(200L * round);
            }
        }

        String location = lastUri == null ? "" : "，最后地址: " + lastUri;
        throw new IOException(
                "无法下载 " + item.label() + "（已尝试 " + attempted + " 次" + location + "）",
                lastFailure
        );
    }

    private void downloadOnce(URI uri, DownloadItem item, DownloadListener listener,
                              DownloadValidator validator)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        try (InputStream input = response.body()) {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }

            Path destination = item.destination();
            Path parent = destination.toAbsolutePath().getParent();
            Path temporary = Files.createTempFile(parent, "caml-download-", ".part");
            try {
                long total = item.size() > 0
                        ? item.size()
                        : response.headers().firstValueAsLong("Content-Length").orElse(-1);
                long downloaded = 0;
                byte[] buffer = new byte[8192];
                int read;

                try (OutputStream output = Files.newOutputStream(
                        temporary,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                )) {
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        downloaded += read;
                        listener.onProgress(new DownloadListener.DownloadProgress(
                                item.label(), downloaded, total));
                    }
                }

                if (!HashVerifier.matches(temporary, item.sha1())) {
                    throw new IOException("SHA-1 mismatch for " + item.label());
                }
                if (validator != null) {
                    validator.validate(temporary);
                }
                move(temporary, destination);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
}
