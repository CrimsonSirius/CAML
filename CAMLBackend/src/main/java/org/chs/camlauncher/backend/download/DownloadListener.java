package org.chs.camlauncher.backend.download;

@FunctionalInterface
public interface DownloadListener {
    DownloadListener NONE = progress -> {
    };

    void onProgress(DownloadProgress progress);

    record DownloadProgress(String label, long downloaded, long total) {
    }
}
