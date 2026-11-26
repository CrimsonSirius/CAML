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

    /** 同一批下载即将重试；进度聚合器不应把上一轮失败批次累计为已完成计划。 */
    default void onPlanRetry() {
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

    static void notifyPlanRetry(DownloadListener listener) {
        if (listener != null) {
            listener.onPlanRetry();
        }
    }

    record DownloadProgress(String label, long downloaded, long total) {
    }

    record DownloadSummary(long downloaded, long total, int completedItems, int totalItems) {
    }
}
