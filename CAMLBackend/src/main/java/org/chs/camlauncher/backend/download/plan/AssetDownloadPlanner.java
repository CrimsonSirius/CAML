package org.chs.camlauncher.backend.download.plan;

import org.chs.camlauncher.backend.metadata.model.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.model.AssetObject;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.model.DownloadPlan;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AssetDownloadPlanner {
    private static final String DEFAULT_BASE_URL = "https://resources.download.minecraft.net/";

    private final List<DownloadSource> sources;
    private final String baseUrl;

    public AssetDownloadPlanner() {
        this(List.of(OfficialDownloadSource.INSTANCE), DEFAULT_BASE_URL);
    }

    public AssetDownloadPlanner(List<DownloadSource> sources, String baseUrl) {
        this.sources = List.copyOf(sources);
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").endsWith("/")
                ? baseUrl
                : baseUrl + "/";
    }

    public DownloadPlan plan(AssetIndexFile index, Path assetsDirectory) {
        Map<Path, DownloadItem> items = new LinkedHashMap<>();
        for (AssetObject object : index.objects().values()) {
            String hash = object.hash();
            if (hash.length() < 2) {
                continue;
            }

            Path destination = assetsDirectory
                    .resolve("objects")
                    .resolve(hash.substring(0, 2))
                    .resolve(hash);
            String url = baseUrl + hash.substring(0, 2) + "/" + hash;
            items.putIfAbsent(destination.toAbsolutePath().normalize(), new DownloadItem(
                    "asset " + hash,
                    DownloadSources.candidates(sources, url),
                    destination,
                    hash,
                    object.size() == null ? -1 : object.size()
            ));
        }
        return new DownloadPlan(items.values().stream().toList());
    }
}
