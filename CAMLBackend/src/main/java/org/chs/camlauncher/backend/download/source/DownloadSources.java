package org.chs.camlauncher.backend.download.source;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DownloadSources {
    private DownloadSources() {
    }

    public static List<URI> candidates(List<DownloadSource> sources, String url) {
        URI original = URI.create(url);
        Set<URI> candidates = new LinkedHashSet<>();
        for (DownloadSource source : sources) {
            candidates.addAll(source.candidates(original));
        }
        candidates.add(original);
        return List.copyOf(candidates);
    }
}
