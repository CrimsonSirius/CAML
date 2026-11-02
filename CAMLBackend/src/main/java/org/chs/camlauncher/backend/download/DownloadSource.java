package org.chs.camlauncher.backend.download;

import java.net.URI;
import java.util.List;

public interface DownloadSource {
    String name();

    List<URI> candidates(URI original);
}
