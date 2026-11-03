package org.chs.camlauncher.backend.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import java.util.Objects;

public final class VersionManifestService {
    public static final URI DEFAULT_MANIFEST_URI = URI.create(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    );

    private static final Logger LOGGER = LogManager.getLogger(VersionManifestService.class);

    private final HttpClient httpClient;
    private final MetadataService metadata;

    public VersionManifestService() {
        this(defaultClient(), new MetadataService());
    }

    public VersionManifestService(HttpClient httpClient, MetadataService metadata) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public VersionManifest fetchManifest() throws IOException, InterruptedException {
        return fetchManifest(DEFAULT_MANIFEST_URI);
    }

    public VersionManifest fetchManifest(URI uri) throws IOException, InterruptedException {
        return metadata.parseVersionManifest(fetchText(uri));
    }

    public VersionInfo fetchVersionInfo(VersionManifest.Version version) throws IOException, InterruptedException {
        return metadata.parseVersionInfo(fetchVersionJson(version));
    }

    public VersionInfo fetchVersionInfo(URI uri) throws IOException, InterruptedException {
        return metadata.parseVersionInfo(fetchText(uri));
    }

    public String fetchVersionJson(VersionManifest.Version version) throws IOException, InterruptedException {
        if (version.url() == null || version.url().isBlank()) {
            throw new IOException("Version has no metadata URL: " + version.id());
        }
        return fetchText(URI.create(version.url()));
    }

    public VersionManifest fetchManifestOrCache(URI uri, Path cacheFile)
            throws IOException, InterruptedException {
        try {
            String text = fetchText(uri);
            writeCache(cacheFile, text);
            return metadata.parseVersionManifest(text);
        } catch (IOException e) {
            if (Files.isRegularFile(cacheFile)) {
                LOGGER.warn("Failed to fetch version manifest, using cache: {}", cacheFile, e);
                return metadata.readVersionManifest(cacheFile);
            }
            throw e;
        }
    }

    public VersionManifest loadCachedManifest(Path cacheFile) throws IOException {
        return metadata.readVersionManifest(cacheFile);
    }

    private String fetchText(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "CAML")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + uri);
        }
        return response.body();
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
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
}
