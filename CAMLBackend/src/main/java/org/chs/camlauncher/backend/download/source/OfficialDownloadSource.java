package org.chs.camlauncher.backend.download.source;

import java.net.URI;
import java.util.List;

public final class OfficialDownloadSource implements DownloadSource {
    public static final OfficialDownloadSource INSTANCE = new OfficialDownloadSource();

    private OfficialDownloadSource() {
    }

    @Override
    public String name() {
        return "official";
    }

    @Override
    public List<URI> candidates(URI original) {
        return List.of(original);
    }
}
