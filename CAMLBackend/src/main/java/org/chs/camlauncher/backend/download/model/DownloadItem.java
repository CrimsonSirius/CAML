package org.chs.camlauncher.backend.download.model;

import org.chs.camlauncher.backend.download.hash.HashVerifier;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record DownloadItem(
        String label,
        List<URI> candidates,
        Path destination,
        String sha1,
        long size
) {
    public DownloadItem {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(destination, "destination");
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
    }

    public boolean existsAndValid() throws IOException {
        return Files.isRegularFile(destination)
                && Files.size(destination) > 0
                && HashVerifier.matches(destination, sha1);
    }
}
