package org.chs.camlauncher.backend.download.model;

import org.chs.camlauncher.backend.download.hash.HashVerifier;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DownloadItem(
        String label,
        List<URI> candidates,
        Path destination,
        String sha1,
        long size,
        Map<String, String> headers
) {
    public DownloadItem(String label, List<URI> candidates, Path destination, String sha1, long size) {
        this(label, candidates, destination, sha1, size, Map.of());
    }

    public DownloadItem {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(destination, "destination");
        candidates = List.copyOf(candidates);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
    }

    public boolean existsAndValid() throws IOException {
        if (!Files.isRegularFile(destination) || Files.size(destination) <= 0) {
            return false;
        }
        if (hasSha1()) {
            return HashVerifier.matches(destination, sha1);
        }
        return size <= 0 || Files.size(destination) == size;
    }

    /** 是否提供了非空 SHA-1。 */
    public boolean hasSha1() {
        return sha1 != null && !sha1.isBlank();
    }
}
