package org.chs.camlauncher.backend.download.plan;

import org.chs.camlauncher.backend.metadata.model.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.model.AssetObject;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.model.DownloadPlan;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.io.SafePaths;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AssetDownloadPlanner {
    private static final String DEFAULT_BASE_URL = "https://resources.download.minecraft.net/";

    private volatile List<DownloadSource> sources;
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

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    }

    public DownloadPlan plan(AssetIndexFile index, Path assetsDirectory) {
        Map<Path, DownloadItem> items = new LinkedHashMap<>();
        for (AssetObject object : index.objects().values()) {
            String hash = object.hash();
            if (hash == null || !hash.matches("[0-9a-fA-F]{40}")) {
                throw new IllegalArgumentException("非法 asset hash: " + hash);
            }

            Path destination = SafePaths.resolveUnder(
                    assetsDirectory.resolve("objects"),
                    hash.substring(0, 2) + "/" + hash,
                    "asset object");
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
