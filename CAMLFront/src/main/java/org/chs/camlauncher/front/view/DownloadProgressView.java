package org.chs.camlauncher.front.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.language.Language;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 独立下载进度页：左侧网速，右侧阶段、总进度和当前文件。 */
final class DownloadProgressView extends VBox implements PageLifecycle {
    private static final Duration REFRESH_INTERVAL = Duration.millis(200);
    private static final String[] STAGE_STATE_CLASSES = {
            "app-download-stage-done",
            "app-download-stage-active",
            "app-download-stage-failed"
    };

    private final DownloadTelemetry telemetry;
    private final Label speedLabel = new Label(Language.tr("download.progress.speedValue"));
    private final Label totalsLabel = new Label();
    private final Label stageLabel = new Label(Language.tr("download.progress.preparing"));
    private final Label currentFileLabel = new Label(Language.tr("download.progress.waiting"));
    private final Label progressDetail = new Label();
    private final CamlProgressBar progressBar = new CamlProgressBar();
    private final VBox stageList = new VBox(8);
    private final List<HBox> stageRows = new ArrayList<>();
    private final List<Label> stageMarkers = new ArrayList<>();
    private final List<Label> stageTitles = new ArrayList<>();
    private final Timeline refreshTimeline;
    private boolean disposed;

    DownloadProgressView(DownloadRequest request, DownloadTelemetry telemetry, Runnable onBack) {
        this.telemetry = telemetry;
        getStyleClass().add("app-download-progress-page");
        setPadding(new Insets(32, 40, 32, 40));
        setSpacing(20);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label title = new Label(Language.tr("download.progress.title", request.instanceId()));
        title.getStyleClass().add("app-download-progress-heading");
        title.setWrapText(true);
        Label subtitle = new Label(Language.tr("download.progress.subtitle",
                request.description(), request.version().id()));
        subtitle.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        subtitle.setWrapText(true);
        VBox titleBlock = new VBox(4, title, subtitle);

        CamlButton back = CamlButton.outlined(Language.tr("download.progress.back"));
        back.getStyleClass().add("app-pill-button");
        back.setOnAction(event -> onBack.run());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(16, titleBlock, headerSpacer, back);
        header.setAlignment(Pos.CENTER_LEFT);

        Label speedTitle = new Label(Language.tr("download.progress.speed"));
        speedTitle.getStyleClass().add("app-download-card-title");
        speedLabel.getStyleClass().add("app-download-speed-value");
        totalsLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        totalsLabel.setWrapText(true);
        VBox speedCard = new VBox(12, speedTitle, speedLabel, totalsLabel);
        speedCard.getStyleClass().addAll("app-download-card", "app-download-speed-card");
        speedCard.setMinWidth(240);
        speedCard.setPrefWidth(240);
        speedCard.setMaxWidth(260);
        speedCard.setAlignment(Pos.TOP_LEFT);

        Label progressTitle = new Label(Language.tr("download.progress.progress"));
        progressTitle.getStyleClass().add("app-download-card-title");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        stageLabel.getStyleClass().add("app-download-stage-current");
        stageLabel.setWrapText(true);
        currentFileLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        currentFileLabel.setWrapText(true);
        progressDetail.getStyleClass().add("app-download-progress-detail");
        progressDetail.setWrapText(true);
        stageList.getStyleClass().add("app-download-stage-list");

        VBox progressCard = new VBox(14,
                progressTitle, progressBar, progressDetail, stageLabel, currentFileLabel, stageList);
        progressCard.getStyleClass().add("app-download-card");
        progressCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressCard, Priority.ALWAYS);

        HBox body = new HBox(20, speedCard, progressCard);
        body.setAlignment(Pos.TOP_LEFT);
        body.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);

        refreshTimeline = new Timeline(new KeyFrame(REFRESH_INTERVAL, event -> refreshNow()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        refreshNow();
    }

    void refreshNow() {
        if (disposed) {
            return;
        }
        DownloadTelemetry.Snapshot snapshot = telemetry.snapshot();
        speedLabel.setText(formatSpeed(snapshot.bytesPerSecond()));
        totalsLabel.setText(snapshot.finished()
                ? Language.tr("download.progress.completedTotal")
                : snapshot.totalBytes() > 0
                ? Language.tr("download.progress.totalWithMax",
                        formatBytes(snapshot.downloadedBytes()),
                        formatBytes(snapshot.totalBytes()))
                : Language.tr("download.progress.total",
                        formatBytes(snapshot.downloadedBytes())));

        updateStatus(snapshot);
        updateProgress(snapshot);
        updateStages(snapshot);
    }

    @Override
    public void dispose() {
        disposed = true;
        refreshTimeline.stop();
    }

    @Override
    public void onShow() {
        if (!disposed && refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshNow();
            refreshTimeline.play();
        }
    }

    @Override
    public void onHide() {
        refreshTimeline.stop();
    }

    private void updateStatus(DownloadTelemetry.Snapshot snapshot) {
        stageLabel.getStyleClass().removeAll(
                "app-download-status-error", "app-download-status-success");
        if (snapshot.failed()) {
            stageLabel.setText(Language.tr("download.progress.failed", snapshot.failureMessage()));
            stageLabel.getStyleClass().add("app-download-status-error");
        } else if (snapshot.finished()) {
            stageLabel.setText(Language.tr("download.progress.completed"));
            stageLabel.getStyleClass().add("app-download-status-success");
        } else {
            stageLabel.setText(snapshot.currentStage());
        }
        if (snapshot.currentFile().isBlank()) {
            currentFileLabel.setText(Language.tr("download.progress.waiting"));
        } else if (snapshot.currentFileTotal() > 0) {
            currentFileLabel.setText(Language.tr("download.progress.currentFileWithBytes",
                    snapshot.currentFile(),
                    formatBytes(snapshot.currentFileDownloaded()),
                    formatBytes(snapshot.currentFileTotal())));
        } else {
            currentFileLabel.setText(Language.tr("download.progress.currentFile", snapshot.currentFile()));
        }
    }

    private void updateProgress(DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            progressBar.setProgress(1.0);
            progressDetail.setText(Language.tr("download.progress.finishedHint"));
            return;
        }
        if (snapshot.failed()) {
            progressDetail.setText(Language.tr("download.progress.failedHint"));
            return;
        }

        if (snapshot.totalBytes() > 0) {
            double progress = Math.max(0, Math.min(1,
                    snapshot.downloadedBytes() / (double) snapshot.totalBytes()));
            if (snapshot.completedItems() < snapshot.totalItems()) {
                progress = Math.min(progress, 0.99);
            }
            progressBar.setProgress(progress);
            progressDetail.setText(Language.tr("download.progress.bytesDetail",
                    progress * 100,
                    formatBytes(snapshot.downloadedBytes()),
                    formatBytes(snapshot.totalBytes()),
                    snapshot.completedItems(),
                    snapshot.totalItems()));
        } else if (snapshot.totalItems() > 0) {
            double progress = Math.max(0, Math.min(1,
                    snapshot.completedItems() / (double) snapshot.totalItems()));
            progressBar.setProgress(progress);
            progressDetail.setText(Language.tr("download.progress.filesDetail",
                    snapshot.completedItems(), snapshot.totalItems()));
        } else {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            progressDetail.setText(Language.tr("download.progress.planning"));
        }
    }

    private void updateStages(DownloadTelemetry.Snapshot snapshot) {
        List<String> stages = snapshot.stages();
        while (stageRows.size() < stages.size()) {
            Label marker = new Label("○");
            marker.getStyleClass().add("app-download-stage-marker");
            Label title = new Label();
            title.getStyleClass().add("app-download-stage-title");
            title.setWrapText(true);
            HBox row = new HBox(10, marker, title);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("app-download-stage-row");
            HBox.setHgrow(title, Priority.ALWAYS);
            stageRows.add(row);
            stageMarkers.add(marker);
            stageTitles.add(title);
            stageList.getChildren().add(row);
        }

        int currentIndex = stages.indexOf(snapshot.currentStage());
        for (int index = 0; index < stages.size(); index++) {
            HBox row = stageRows.get(index);
            Label marker = stageMarkers.get(index);
            Label title = stageTitles.get(index);
            title.setText(stages.get(index));
            row.getStyleClass().removeAll(STAGE_STATE_CLASSES);

            boolean done = snapshot.finished() || index < currentIndex;
            boolean active = !snapshot.finished() && !snapshot.failed() && index == currentIndex;
            boolean failed = snapshot.failed() && index == currentIndex;
            if (done) {
                marker.setText("✓");
                row.getStyleClass().add("app-download-stage-done");
            } else if (active) {
                marker.setText("●");
                row.getStyleClass().add("app-download-stage-active");
            } else if (failed) {
                marker.setText("!");
                row.getStyleClass().add("app-download-stage-failed");
            } else {
                marker.setText("○");
            }
        }
    }

    private static String formatSpeed(long bytesPerSecond) {
        return formatBytes(bytesPerSecond) + "/s";
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(Locale.ROOT, value >= 100 ? "%.0f %s" : "%.1f %s",
                value, units[unit]);
    }
}
