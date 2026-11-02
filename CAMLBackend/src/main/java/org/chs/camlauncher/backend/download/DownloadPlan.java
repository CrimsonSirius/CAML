package org.chs.camlauncher.backend.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record DownloadPlan(List<DownloadItem> items) {
    public DownloadPlan {
        items = List.copyOf(items);
    }

    public List<DownloadItem> missing() throws IOException {
        List<DownloadItem> missing = new ArrayList<>();
        for (DownloadItem item : items) {
            if (!item.existsAndValid()) {
                missing.add(item);
            }
        }
        return List.copyOf(missing);
    }
}
