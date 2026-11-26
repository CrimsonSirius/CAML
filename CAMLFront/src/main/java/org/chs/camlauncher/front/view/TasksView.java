package org.chs.camlauncher.front.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.task.DownloadTask;
import org.chs.camlauncher.front.task.DownloadTelemetry;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlProgressBar;

import java.util.LinkedHashMap;
import java.util.Map;

/** 任务中心：列出下载与安装任务，并复用下载进度页展示单个任务。 */
final class TasksView extends VBox implements PageLifecycle {
    private static final Duration REFRESH_INTERVAL = Duration.millis(250);
    private static final String[] STATUS_CLASSES = {
            "app-task-status-running",
            "app-task-status-completed",
            "app-task-status-failed",
            "app-task-status-cancelled"
    };

    private final ObservableList<DownloadTask> tasks;
    private final VBox listPage = new VBox();
    private final VBox taskList = new VBox();
    private final Label emptyLabel = new Label(UiText.tr("tasks.empty"));
    private final ScrollPane taskScroll = new ScrollPane(listPage);
    private final CamlPageHost contentHost;
    private final Map<DownloadTask, TaskRow> taskRows = new LinkedHashMap<>();
    private final ListChangeListener<DownloadTask> taskListListener = change -> rebuildRows();
    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(REFRESH_INTERVAL, event -> refreshRows()));
    private ModDownloadLayout.Plan appliedPlan;
    private DownloadTask currentTask;
    private DownloadProgressView detailView;
    private boolean shown;
    private boolean disposed;

    TasksView(DownloadsController controller) {
        this(controller.downloadTasks());
    }

    TasksView(ObservableList<DownloadTask> tasks) {
        this.tasks = tasks;
        getStyleClass().add("app-task-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label title = new Label(UiText.tr("tasks.title"));
        title.getStyleClass().add("app-task-title");
        Label subtitle = new Label(UiText.tr("tasks.subtitle"));
        subtitle.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        subtitle.setWrapText(true);

        emptyLabel.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        taskList.getStyleClass().add("app-task-list");
        taskList.setFillWidth(true);
        listPage.getChildren().addAll(title, subtitle, emptyLabel, taskList);
        listPage.getStyleClass().add("app-task-list-page");
        listPage.setFillWidth(true);
        listPage.setMaxWidth(Double.MAX_VALUE);

        taskScroll.setFitToWidth(true);
        taskScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        taskScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        taskScroll.getStyleClass().add("app-instance-scroll");
        taskScroll.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(taskScroll, Priority.ALWAYS);

        contentHost = new CamlPageHost(taskScroll);
        contentHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        getChildren().setAll(contentHost);

        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        tasks.addListener(taskListListener);
        applyPlan(ModDownloadLayout.WIDE);
        ViewSupport.onLayoutMetricsChanged(this, this::updateLayout);
        rebuildRows();
    }

    DownloadTask currentTask() {
        return currentTask;
    }

    void showTask(DownloadTask task) {
        if (disposed || task == null) {
            return;
        }
        if (detailView != null) {
            detailView.dispose();
        }
        currentTask = task;
        detailView = new DownloadProgressView(task, this::showList);
        contentHost.setPage(detailView);
    }

    void showList() {
        if (disposed) {
            return;
        }
        currentTask = null;
        if (detailView != null) {
            detailView.dispose();
            detailView = null;
        }
        contentHost.setPage(taskScroll);
        refreshRows();
    }

    private void updateLayout() {
        double width = getWidth();
        if (width <= 0) {
            return;
        }
        applyPlan(ModDownloadLayout.plan(width, ViewSupport.uiFactor(this)));
    }

    private void applyPlan(ModDownloadLayout.Plan plan) {
        if (plan.equals(appliedPlan)) {
            return;
        }
        boolean scaleChanged = appliedPlan != null && appliedPlan.factor() != plan.factor();
        appliedPlan = plan;
        listPage.setPadding(new Insets(plan.contentPaddingVertical(),
                plan.contentPaddingHorizontal(), plan.contentPaddingVertical(),
                plan.contentPaddingHorizontal()));
        listPage.setSpacing(plan.sectionSpacing());
        taskList.setSpacing(plan.listSpacing());
        if (scaleChanged) {
            rebuildRows();
        }
    }

    private void rebuildRows() {
        if (disposed) {
            return;
        }
        taskList.getChildren().clear();
        taskRows.clear();
        for (DownloadTask task : tasks) {
            TaskRow row = createTaskRow(task);
            taskRows.put(task, row);
            taskList.getChildren().add(row.container());
        }
        boolean empty = taskRows.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
        refreshRows();
    }

    private TaskRow createTaskRow(DownloadTask task) {
        ModDownloadLayout.Plan plan = plan();
        int iconSize = (int) Math.round(plan.iconSize());

        Label icon = new Label();
        icon.getStyleClass().addAll("app-mod-icon-text", "app-task-icon");
        StackPane placeholder = new StackPane(icon);
        placeholder.getStyleClass().add("app-mod-icon-placeholder");
        placeholder.setMinSize(iconSize, iconSize);
        placeholder.setPrefSize(iconSize, iconSize);
        placeholder.setMaxSize(iconSize, iconSize);

        Label title = new Label(task.title());
        title.getStyleClass().add("app-mod-title");
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label();
        status.getStyleClass().add("app-mod-source");
        status.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        status.setAlignment(Pos.CENTER_RIGHT);

        HBox heading = new HBox(title, status);
        heading.getStyleClass().add("app-mod-title-row");
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label subject = new Label(task.subject());
        subject.getStyleClass().add("app-mod-summary");
        subject.setMinWidth(0);
        subject.setMaxWidth(Double.MAX_VALUE);

        CamlProgressBar progress = new CamlProgressBar();
        progress.getStyleClass().add("app-task-progress");
        progress.setMaxWidth(Double.MAX_VALUE);

        Label detail = new Label();
        detail.getStyleClass().add("app-mod-meta");
        detail.setMinWidth(0);
        detail.setMaxWidth(Double.MAX_VALUE);

        VBox text = new VBox(heading, subject, progress, detail);
        text.getStyleClass().add("app-mod-text");
        text.setFillWidth(true);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        StackPane row = ModViewSupport.resultRow(plan, placeholder, text,
                UiText.tr("tasks.open", task.title(), task.subject()),
                () -> showTask(task));
        return new TaskRow(row, icon, title, status, subject, detail, progress);
    }

    private void refreshRows() {
        if (disposed) {
            return;
        }
        boolean anyRunning = false;
        for (Map.Entry<DownloadTask, TaskRow> entry : taskRows.entrySet()) {
            DownloadTask task = entry.getKey();
            TaskRow row = entry.getValue();
            DownloadTelemetry.Snapshot snapshot = task.snapshot();
            boolean running = task.isRunning();
            anyRunning |= running;
            row.icon().setText(iconText(snapshot));
            row.title().setText(task.title());
            row.status().setText(statusText(snapshot));
            applyStatusClass(row.status(), snapshot);
            row.subject().setText(task.subject());
            row.detail().setText(detailText(snapshot));
            row.progress().setProgress(progress(snapshot, running));
        }
        if (shown && anyRunning) {
            refreshTimeline.playFromStart();
        } else {
            refreshTimeline.stop();
        }
    }

    private static String statusText(DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return UiText.tr("tasks.status.completed");
        }
        if (snapshot.failed()) {
            return UiText.tr("tasks.status.failed");
        }
        if (snapshot.cancelled()) {
            return UiText.tr("tasks.status.cancelled");
        }
        return UiText.tr("tasks.status.running");
    }

    private static void applyStatusClass(Label status, DownloadTelemetry.Snapshot snapshot) {
        status.getStyleClass().removeAll(STATUS_CLASSES);
        if (snapshot.finished()) {
            status.getStyleClass().add("app-task-status-completed");
        } else if (snapshot.failed()) {
            status.getStyleClass().add("app-task-status-failed");
        } else if (snapshot.cancelled()) {
            status.getStyleClass().add("app-task-status-cancelled");
        } else {
            status.getStyleClass().add("app-task-status-running");
        }
    }

    private static String iconText(DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return "✓";
        }
        if (snapshot.failed()) {
            return "!";
        }
        if (snapshot.cancelled()) {
            return "×";
        }
        return "▶";
    }

    private static String detailText(DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return UiText.tr("download.progress.finishedHint");
        }
        if (snapshot.failed()) {
            return snapshot.failureMessage();
        }
        if (snapshot.cancelled()) {
            return UiText.tr("download.progress.failedHint");
        }
        return snapshot.currentFile().isBlank()
                ? snapshot.currentStage()
                : snapshot.currentFile();
    }

    private static double progress(DownloadTelemetry.Snapshot snapshot, boolean running) {
        if (snapshot.finished()) {
            return 1;
        }
        if (snapshot.totalBytes() > 0) {
            return Math.max(0, Math.min(1,
                    snapshot.downloadedBytes() / (double) snapshot.totalBytes()));
        }
        return running ? ProgressBar.INDETERMINATE_PROGRESS : 0;
    }

    private ModDownloadLayout.Plan plan() {
        return appliedPlan == null ? ModDownloadLayout.WIDE : appliedPlan;
    }

    void refreshNow() {
        refreshRows();
    }

    @Override
    public void onShow() {
        shown = true;
        if (detailView != null) {
            detailView.onShow();
        } else {
            refreshRows();
        }
    }

    @Override
    public void onHide() {
        shown = false;
        refreshTimeline.stop();
        if (detailView != null) {
            detailView.onHide();
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        refreshTimeline.stop();
        tasks.removeListener(taskListListener);
        if (detailView != null) {
            detailView.dispose();
            detailView = null;
        }
    }

    private record TaskRow(
            StackPane container,
            Label icon,
            Label title,
            Label status,
            Label subject,
            Label detail,
            CamlProgressBar progress
    ) {
    }
}
