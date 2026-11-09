package org.chs.camlauncher.backend.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;

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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class VersionManifestService {
    public static final URI DEFAULT_MANIFEST_URI = URI.create(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    );

    private static final Logger LOGGER = LogManager.getLogger(VersionManifestService.class);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_SOURCE_ATTEMPTS = 1;
    private static final int MAX_REDIRECTS = 10;

    private final HttpClient httpClient;
    private final MetadataService metadata;
    private final Duration requestTimeout;
    private final int sourceAttempts;
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

    public VersionManifestService() {
        this(defaultClient(), new MetadataService(), DEFAULT_REQUEST_TIMEOUT, DEFAULT_SOURCE_ATTEMPTS);
    }

    public VersionManifestService(HttpClient httpClient, MetadataService metadata) {
        this(httpClient, metadata, DEFAULT_REQUEST_TIMEOUT, DEFAULT_SOURCE_ATTEMPTS);
    }

    VersionManifestService(HttpClient httpClient, MetadataService metadata,
                           Duration requestTimeout, int sourceAttempts) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        if (sourceAttempts < 1) {
            throw new IllegalArgumentException("sourceAttempts must be positive");
        }
        this.sourceAttempts = sourceAttempts;
    }

    public VersionManifest fetchManifest() throws IOException, InterruptedException {
        return fetchManifest(DEFAULT_MANIFEST_URI);
    }

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    }

    public VersionManifest fetchManifest(URI uri) throws IOException, InterruptedException {
        return fetchParsedFromSources(uri, this::parseManifest);
    }

    public VersionInfo fetchVersionInfo(VersionManifest.Version version) throws IOException, InterruptedException {
        return metadata.parseVersionInfo(fetchVersionJson(version));
    }

    public VersionInfo fetchVersionInfo(URI uri) throws IOException, InterruptedException {
        return fetchParsedFromSources(uri, this::parseVersionInfo);
    }

    public String fetchVersionJson(VersionManifest.Version version) throws IOException, InterruptedException {
        if (version.url() == null || version.url().isBlank()) {
            throw new IOException("Version has no metadata URL: " + version.id());
        }
        return fetchParsedFromSources(URI.create(version.url()), text -> {
            parseVersionInfo(text);
            return text;
        });
    }

    public VersionManifest fetchManifestOrCache(URI uri, Path cacheFile)
            throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (URI candidate : DownloadSources.candidates(sources, uri.toString())) {
            try {
                String text = fetchText(candidate);
                VersionManifest manifest = parseManifest(text);
                writeCache(cacheFile, text);
                return manifest;
            } catch (IOException e) {
                lastFailure = e;
                LOGGER.warn("Failed to fetch version manifest from {}, trying next source", candidate, e);
            } catch (RuntimeException e) {
                lastFailure = new IOException("Invalid version manifest from " + candidate, e);
                LOGGER.warn("Invalid version manifest from {}, trying next source", candidate, e);
            }
        }
        if (Files.isRegularFile(cacheFile)) {
            LOGGER.warn("Failed to fetch version manifest, using cache: {}", cacheFile, lastFailure);
            return metadata.readVersionManifest(cacheFile);
        }
        throw lastFailure == null
                ? new IOException("No download source available for version manifest")
                : lastFailure;
    }

    public VersionManifest loadCachedManifest(Path cacheFile) throws IOException {
        return metadata.readVersionManifest(cacheFile);
    }

    private String fetchText(URI uri) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= sourceAttempts; attempt++) {
            try {
                long deadline = System.nanoTime() + requestTimeout.toNanos();
                return fetchFollowingRedirects(uri, deadline);
            } catch (IOException e) {
                lastFailure = e;
            }

            if (attempt < sourceAttempts) {
                LOGGER.warn("请求 {} 失败，重试 {}/{}: {}",
                        uri, attempt + 1, sourceAttempts, lastFailure.getMessage());
            }
        }
        throw lastFailure == null ? new IOException("请求失败：" + uri) : lastFailure;
    }

    private String fetchFollowingRedirects(URI uri, long deadline)
            throws IOException, InterruptedException {
        URI current = uri;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new IOException("请求超时（" + requestTimeout.toMillis() + "ms）：" + uri);
            }

            HttpRequest request = HttpRequest.newBuilder(current)
                    .header("User-Agent", "CAML")
                    .GET()
                    .build();
            CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            HttpResponse<String> response;
            try {
                response = future.get(remainingNanos, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new IOException(
                        "请求超时（" + requestTimeout.toMillis() + "ms）：" + uri, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw cause instanceof IOException ioException
                        ? ioException
                        : new IOException("请求失败：" + current, cause);
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw e;
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            }
            if (isRedirect(status)) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location == null || location.isBlank()) {
                    throw new IOException("重定向缺少 Location：" + current);
                }
                current = current.resolve(location);
                continue;
            }
            throw new IOException("HTTP " + status + " for " + current);
        }
        throw new IOException("重定向次数过多：" + uri);
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303
                || status == 307 || status == 308;
    }

    private <T> T fetchParsedFromSources(URI uri, TextParser<T> parser)
            throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (URI candidate : DownloadSources.candidates(sources, uri.toString())) {
            try {
                return parser.parse(fetchText(candidate));
            } catch (IOException e) {
                lastFailure = e;
                LOGGER.warn("Failed to fetch metadata from {}, trying next source", candidate, e);
            } catch (RuntimeException e) {
                lastFailure = new IOException("Invalid metadata from " + candidate, e);
                LOGGER.warn("Invalid metadata from {}, trying next source", candidate, e);
            }
        }
        throw lastFailure == null
                ? new IOException("No download source available for " + uri)
                : lastFailure;
    }

    private VersionManifest parseManifest(String text) throws IOException {
        VersionManifest manifest = metadata.parseVersionManifest(text);
        if (manifest == null || manifest.versions().isEmpty()) {
            throw new IOException("版本清单为空");
        }
        return manifest;
    }

    private VersionInfo parseVersionInfo(String text) throws IOException {
        VersionInfo info = metadata.parseVersionInfo(text);
        if (info == null) {
            throw new IOException("版本 JSON 为空");
        }
        return info;
    }

    @FunctionalInterface
    private interface TextParser<T> {
        T parse(String text) throws IOException;
    }

    private static void writeCache(Path cacheFile, String text) throws IOException {
        Path parent = cacheFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temporary = Files.createTempFile(parent, "caml-manifest-", ".part");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, cacheFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
}
