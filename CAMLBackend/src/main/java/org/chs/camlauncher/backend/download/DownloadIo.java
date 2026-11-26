package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

/** 下载器共享的文件、HTTP 和校验工具。 */
final class DownloadIo {
    static final Pattern CONTENT_RANGE = Pattern.compile("bytes\\s+(\\d+)-(\\d+)/(\\d+)");

    private DownloadIo() {
    }

    static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    static HttpRequest.Builder requestBuilder(URI uri, DownloadItem item) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        for (Map.Entry<String, String> header : item.headers().entrySet()) {
            if (header.getKey() != null && !header.getKey().isBlank()
                    && header.getValue() != null && !header.getValue().isBlank()) {
                builder.header(header.getKey(), header.getValue());
            }
        }
        return builder;
    }

    static String strongEtag(HttpResponse<?> response) {
        String etag = response.headers().firstValue("ETag").orElse(null);
        return etag == null || etag.isBlank() || etag.regionMatches(true, 0, "W/", 0, 2)
                ? null
                : etag;
    }

    static String lastModified(HttpResponse<?> response) {
        return response.headers().firstValue("Last-Modified")
                .filter(value -> !value.isBlank())
                .orElse(null);
    }

    static boolean validatesExistingFile(Path file, DownloadValidator validator) {
        if (validator == null) {
            return true;
        }
        try {
            validator.validate(file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static void verify(Path file, DownloadItem item, DownloadValidator validator)
            throws IOException {
        if (item.hasSha1() && !HashVerifier.matches(file, item.sha1())) {
            throw new IOException("SHA-1 mismatch for " + item.label());
        }
        if (validator != null) {
            validator.validate(file);
        }
    }

    static Path partialPath(Path destination) {
        return destination.resolveSibling(destination.getFileName() + ".part");
    }

    static Path metaPath(Path part) {
        return part.resolveSibling(part.getFileName() + ".json");
    }

    static void clearPartial(DownloadItem item) throws IOException {
        Path part = partialPath(item.destination());
        Files.deleteIfExists(part);
        Files.deleteIfExists(metaPath(part));
    }

    static void clearPartialQuietly(DownloadItem item, Throwable original) {
        try {
            clearPartial(item);
        } catch (IOException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
        }
    }

    static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
