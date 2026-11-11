package org.chs.camlauncher.backend.download;

@FunctionalInterface
public interface DownloadListener {
    DownloadListener NONE = progress -> { };

    void onProgress(DownloadProgress progress);

    default void onStage(String stage) {
    }

    default void onStage(DownloadStage stage) {
        onStage(stage.key());
    }

    default void onSummary(DownloadSummary summary) {
    }

    static void notifyStage(DownloadListener listener, String stage) {
        if (listener != null) {
            listener.onStage(stage);
        }
    }

    static void notifyStage(DownloadListener listener, DownloadStage stage) {
        if (listener != null) {
            listener.onStage(stage);
        }
    }

    record DownloadProgress(String label, long downloaded, long total) {
    }

    record DownloadSummary(long downloaded, long total, int completedItems, int totalItems) {
    }
}
