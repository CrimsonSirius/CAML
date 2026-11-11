package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.front.text.UiText;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 把后端下载事件聚合为进度页需要的阶段、总进度和网速。 */
final class DownloadTelemetry implements DownloadListener {
    private static final long SPEED_WINDOW_NANOS = 1_000_000_000L;
    private static final double MIN_SPEED_WINDOW_SECONDS = 0.25;

    private final Object lock = new Object();
    private final Map<String, Long> itemBytes = new HashMap<>();
    private final Deque<SpeedSample> speedSamples = new ArrayDeque<>();
    private final LinkedHashSet<String> stages = new LinkedHashSet<>();

    private String currentStage = UiText.tr("download.stage.preparing");
    private String currentFile = "";
    private long currentFileDownloaded;
    private long currentFileTotal;
    private long downloadedBytes;
    private long totalBytes;
    private int completedItems;
    private int totalItems;
    private long completedPlanBytes;
    private int completedPlanItems;
    private long lastPlanDownloaded;
    private long lastPlanTotalBytes;
    private int lastPlanCompletedItems;
    private int lastPlanTotalItems;
    private boolean hasPlanSummary;
    private boolean finished;
    private boolean failed;
    private String failureMessage = "";

    DownloadTelemetry() {
        stages.add(currentStage);
    }

    @Override
    public void onProgress(DownloadProgress progress) {
        long now = System.nanoTime();
        synchronized (lock) {
            if (finished || failed) {
                return;
            }
            currentFile = progress.label();
            currentFileDownloaded = progress.downloaded();
            currentFileTotal = progress.total();
            long previous = itemBytes.getOrDefault(progress.label(), 0L);
            itemBytes.put(progress.label(), progress.downloaded());

            boolean alreadyComplete = progress.total() > 0
                    && progress.downloaded() >= progress.total();
            if (alreadyComplete) {
                return;
            }

            long delta = progress.downloaded() >= previous
                    ? progress.downloaded() - previous
                    : progress.downloaded();
            if (delta > 0) {
                speedSamples.addLast(new SpeedSample(now, delta));
            }
        }
    }

    @Override
    public void onSummary(DownloadSummary summary) {
        synchronized (lock) {
            if (finished || failed) {
                return;
            }
            if (hasPlanSummary && startsNewPlan(summary)) {
                completedPlanBytes += Math.max(lastPlanTotalBytes, lastPlanDownloaded);
                completedPlanItems += lastPlanTotalItems;
            }

            lastPlanDownloaded = summary.downloaded();
            lastPlanTotalBytes = summary.total();
            lastPlanCompletedItems = summary.completedItems();
            lastPlanTotalItems = summary.totalItems();
            hasPlanSummary = true;

            downloadedBytes = completedPlanBytes + summary.downloaded();
            totalBytes = completedPlanBytes + summary.total();
            completedItems = completedPlanItems + summary.completedItems();
            totalItems = completedPlanItems + summary.totalItems();
        }
    }

    @Override
    public void onStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return;
        }
        synchronized (lock) {
            if (finished || failed) {
                return;
            }
            if (stages.add(stage)) {
                currentStage = stage;
            }
        }
    }

    @Override
    public void onStage(DownloadStage stage) {
        if (stage == null) {
            return;
        }
        String label = stage.detail() == null
                ? UiText.tr("download.stage." + stage.key())
                : UiText.tr("download.stage." + stage.key(), stage.detail());
        onStage(label);
    }

    void markCompleted() {
        synchronized (lock) {
            if (finished) {
                return;
            }
            finished = true;
            failed = false;
            currentFile = "";
            currentFileDownloaded = 0;
            currentFileTotal = 0;
            currentStage = UiText.tr("download.stage.completed");
            stages.add(currentStage);
            if (totalBytes > 0) {
                downloadedBytes = totalBytes;
            }
            completedItems = totalItems;
            speedSamples.clear();
        }
    }

    void markFailed(String message) {
        synchronized (lock) {
            if (finished || failed) {
                return;
            }
            failed = true;
            failureMessage = message == null ? UiText.tr("download.stage.unknownError") : message;
            currentFile = "";
            currentFileDownloaded = 0;
            currentFileTotal = 0;
            currentStage = UiText.tr("download.stage.failed");
            stages.add(currentStage);
            speedSamples.clear();
        }
    }

    Snapshot snapshot() {
        synchronized (lock) {
            long now = System.nanoTime();
            long cutoff = now - SPEED_WINDOW_NANOS;
            while (!speedSamples.isEmpty() && speedSamples.peekFirst().timestamp() < cutoff) {
                speedSamples.removeFirst();
            }

            long bytesPerSecond = 0;
            if (!finished && !failed && !speedSamples.isEmpty()) {
                long bytes = speedSamples.stream().mapToLong(SpeedSample::bytes).sum();
                double seconds = Math.max(
                        MIN_SPEED_WINDOW_SECONDS,
                        (now - speedSamples.peekFirst().timestamp()) / 1_000_000_000.0
                );
                bytesPerSecond = Math.round(bytes / seconds);
            }

            return new Snapshot(
                    List.copyOf(stages),
                    currentStage,
                    currentFile,
                    currentFileDownloaded,
                    currentFileTotal,
                    downloadedBytes,
                    totalBytes,
                    completedItems,
                    totalItems,
                    bytesPerSecond,
                    finished,
                    failed,
                    failureMessage
            );
        }
    }

    private boolean startsNewPlan(DownloadSummary summary) {
        return summary.downloaded() == 0
                && summary.completedItems() == 0
                && (lastPlanDownloaded > 0
                || lastPlanCompletedItems > 0
                || summary.totalItems() != lastPlanTotalItems);
    }

    record Snapshot(
            List<String> stages,
            String currentStage,
            String currentFile,
            long currentFileDownloaded,
            long currentFileTotal,
            long downloadedBytes,
            long totalBytes,
            int completedItems,
            int totalItems,
            long bytesPerSecond,
            boolean finished,
            boolean failed,
            String failureMessage
    ) {
    }

    private record SpeedSample(long timestamp, long bytes) {
    }
}
