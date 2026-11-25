package org.chs.camlauncher.front.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.mod.model.ModContentType;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.front.UiScale;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.task.AsyncTaskHandle;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.front.task.DownloadTelemetry;
import org.chs.camlauncher.front.task.DownloadTask;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlProgressBar;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;

import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** 下载页：左侧版本类别导航和右侧版本列表/下载设置页。 */
final class DownloadsView extends HBox implements SidebarPage, PageLifecycle {
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final CamlSideNav sidebar;
    private final CamlSideNavItem releaseItem;
    private final CamlSideNavItem snapshotItem;
    private final CamlSideNavItem oldItem;
    private final CamlPageHost contentHost;
    private final VBox taskRowsBox = new VBox();
    private final Map<DownloadTask, TaskRow> taskRows = new LinkedHashMap<>();
    private final ListChangeListener<DownloadTask> taskListListener = change -> rebuildTaskRows();
    private final Timeline taskRefreshTimeline = new Timeline(
            new KeyFrame(Duration.millis(250), event -> refreshTaskRows()));
    private final Map<VersionCategory, DownloadVersionsView> versionsViews =
            new EnumMap<>(VersionCategory.class);
    private VersionCategory currentCategory = VersionCategory.RELEASE;
    private boolean showingVersionList = true;
    private PageLifecycle activeContentPage;
    private ModSearchView modsView;
    private ModSearchView shadersView;
    private ModSearchView resourcePacksView;
    private ModSearchView mapsView;
    private boolean shown;
    private boolean disposed;

    DownloadsView(DownloadsController controller, LaunchSettings launchSettings) {
        this.controller = controller;
        this.launchSettings = launchSettings;
        sidebar = new CamlSideNav();
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setMaxHeight(Double.MAX_VALUE);
        // 图示中侧边栏约占窗口宽度的 1/6；宽度按 UI 缩放因子钳制，几何来自 ModDownloadLayout。
        // 内边距和子项间距由 app.css 的 .app-sidebar（em）负责，跟随根字号缩放。
        ViewSupport.onLayoutMetricsChanged(this, this::updateSidebarWidth);

        releaseItem = new CamlSideNavItem(VersionCategory.RELEASE.label());
        snapshotItem = new CamlSideNavItem(VersionCategory.SNAPSHOT.label());
        oldItem = new CamlSideNavItem(VersionCategory.OLD.label());
        CamlSideNavItem modItem = new CamlSideNavItem(UiText.tr("mods.nav"));
        CamlSideNavItem shaderItem = new CamlSideNavItem(UiText.tr("shaders.nav"));
        CamlSideNavItem resourcePackItem = new CamlSideNavItem(UiText.tr("resourcepacks.nav"));
        CamlSideNavItem mapItem = new CamlSideNavItem(UiText.tr("maps.nav"));
        sidebar.addSection(UiText.tr("downloads.section.instances"));
        sidebar.addItem(releaseItem);
        sidebar.addItem(snapshotItem);
        sidebar.addItem(oldItem);
        sidebar.addSection(UiText.tr("downloads.section.content"));
        sidebar.addItem(modItem);
        sidebar.addItem(shaderItem);
        sidebar.addItem(resourcePackItem);
        sidebar.addItem(mapItem);
        sidebar.select(releaseItem);

        DownloadVersionsView initialView = versionsView(VersionCategory.RELEASE);
        contentHost = new CamlPageHost(initialView);
        activeContentPage = initialView;
        initialView.onShow();
        contentHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        buildTaskRows();
        taskRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        VBox contentColumn = new VBox(taskRowsBox, contentHost);
        contentColumn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(contentColumn, Priority.ALWAYS);
        controller.downloadTasks().addListener(taskListListener);
        rebuildTaskRows();

        releaseItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.RELEASE));
        snapshotItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.SNAPSHOT));
        oldItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.OLD));
        modItem.addEventHandler(ActionEvent.ACTION, event -> showMods());
        shaderItem.addEventHandler(ActionEvent.ACTION, event -> showShaders());
        resourcePackItem.addEventHandler(ActionEvent.ACTION, event -> showResourcePacks());
        mapItem.addEventHandler(ActionEvent.ACTION, event -> showMaps());

        getChildren().add(contentColumn);
        getStyleClass().add("app-download-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private void buildTaskRows() {
        taskRowsBox.getStyleClass().add("app-download-task-bar");
        taskRowsBox.setFillWidth(true);
        taskRowsBox.setVisible(false);
        taskRowsBox.setManaged(false);
    }

    @Override
    public Node sidebar() {
        return sidebar;
    }

    void refreshVersions() {
        if (disposed) {
            return;
        }
        for (DownloadVersionsView view : versionsViews.values()) {
            view.onHide();
            view.dispose();
        }
        versionsViews.clear();
        if (showingVersionList) {
            setContentPage(versionsView(currentCategory));
        } else if (activeContentPage instanceof DownloadVersionSetupView setupPage) {
            setupPage.refreshLoaderVersions();
        }
    }

    private void showVersionSetupPage(VersionManifest.Version version) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new DownloadVersionSetupView(
                version,
                controller,
                launchSettings,
                () -> showVersions(currentCategory),
                this::startDownload
        ));
    }

    private void showVersions(VersionCategory category) {
        setDownloadNavigationVisible(true);
        showingVersionList = true;
        currentCategory = category;
        setContentPage(versionsView(category));
    }

    VersionCategory currentCategory() {
        return currentCategory;
    }

    void showCategory(VersionCategory category) {
        VersionCategory target = category == null ? VersionCategory.RELEASE : category;
        sidebar.select(switch (target) {
            case RELEASE -> releaseItem;
            case SNAPSHOT -> snapshotItem;
            case OLD -> oldItem;
        });
        showVersions(target);
    }

    private void showMods() {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        if (modsView == null) {
            modsView = new ModSearchView(controller, ModContentType.MOD, this::showModDetail);
        }
        setContentPage(modsView);
    }

    private void showShaders() {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        if (shadersView == null) {
            shadersView = new ModSearchView(controller, ModContentType.SHADER,
                    this::showShaderDetail);
        }
        setContentPage(shadersView);
    }

    private void showResourcePacks() {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        if (resourcePacksView == null) {
            resourcePacksView = new ModSearchView(controller, ModContentType.RESOURCE_PACK,
                    this::showResourcePackDetail);
        }
        setContentPage(resourcePacksView);
    }

    private void showMaps() {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        if (mapsView == null) {
            mapsView = new ModSearchView(controller, ModContentType.WORLD, this::showMapDetail);
        }
        setContentPage(mapsView);
    }

    private void showModDetail(ModProjectSummary project) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new ModDetailView(
                controller,
                launchSettings,
                ModContentType.MOD,
                project,
                this::showMods
        ));
    }

    private void showShaderDetail(ModProjectSummary project) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new ModDetailView(
                controller,
                launchSettings,
                ModContentType.SHADER,
                project,
                this::showShaders
        ));
    }

    private void showResourcePackDetail(ModProjectSummary project) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new ModDetailView(
                controller,
                launchSettings,
                ModContentType.RESOURCE_PACK,
                project,
                this::showResourcePacks
        ));
    }

    private void showMapDetail(ModProjectSummary project) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new ModDetailView(
                controller,
                launchSettings,
                ModContentType.WORLD,
                project,
                this::showMaps
        ));
    }

    private void startDownload(DownloadRequest request) {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        DownloadCancellationToken token = new DownloadCancellationToken();
        DownloadTask downloadTask = new DownloadTask(
                "download.progress.title",
                new Object[]{request.instanceId()},
                request.instanceId(),
                telemetry,
                token
        );
        controller.registerDownloadTask(downloadTask);
        DownloadProgressView previousProgress =
                activeContentPage instanceof DownloadProgressView page ? page : null;
        DownloadProgressView page = new DownloadProgressView(
                request,
                telemetry,
                this::backFromProgress
        );
        setDownloadNavigationVisible(false);
        showingVersionList = false;
        setContentPage(page);
        if (previousProgress != null && previousProgress != page) {
            previousProgress.dispose();
        }

        Runnable onSuccess = () -> {
            downloadTask.complete();
            launchSettings.setSelectedVersion(request.instanceId());
            page.refreshNow();
            refreshTaskRows();
        };
        Consumer<String> onError = message -> {
            downloadTask.fail(message);
            page.refreshNow();
            refreshTaskRows();
        };

        AsyncTaskHandle<?> task;
        if (request.optiFine() != null) {
            task = controller.downloadForgeOptiFineVersion(request.version().id(), request.instanceId(),
                    request.forge(), request.optiFine(), request.gameDirectory(),
                    telemetry, token, onSuccess, onError);
        } else if (request.forge() != null) {
            task = controller.downloadForgeVersion(request.version().id(), request.instanceId(),
                    request.forge(), request.gameDirectory(), telemetry, token, onSuccess, onError);
        } else if (request.fabricLoader() != null) {
            task = controller.downloadFabricVersion(request.version().id(), request.instanceId(),
                    request.fabricLoader().version(), request.fabricApi(), request.gameDirectory(),
                    telemetry, token, onSuccess, onError);
        } else if (request.quiltLoader() != null) {
            task = controller.downloadQuiltVersion(request.version().id(), request.instanceId(),
                    request.quiltLoader().version(), request.quiltApi(), request.gameDirectory(),
                    telemetry, token, onSuccess, onError);
        } else if (request.neoForge() != null) {
            task = controller.downloadNeoForgeVersion(request.version().id(), request.instanceId(),
                    request.neoForge(), request.gameDirectory(), telemetry, token, onSuccess, onError);
        } else {
            task = controller.downloadVersion(request.version().id(), request.instanceId(),
                    request.gameDirectory(), telemetry, token, onSuccess, onError);
        }
        downloadTask.attach(task::cancel);
        refreshTaskRows();
        taskRefreshTimeline.playFromStart();
    }

    private void backFromProgress() {
        DownloadProgressView page = activeContentPage instanceof DownloadProgressView current
                ? current
                : null;
        showVersions(currentCategory);
        if (page != null && page != activeContentPage) {
            page.dispose();
        }
    }

    private void rebuildTaskRows() {
        if (disposed) {
            return;
        }
        taskRowsBox.getChildren().clear();
        taskRows.clear();
        for (DownloadTask task : controller.downloadTasks()) {
            TaskRow row = createTaskRow(task);
            taskRows.put(task, row);
            taskRowsBox.getChildren().add(row.container());
        }
        boolean visible = !taskRows.isEmpty();
        taskRowsBox.setVisible(visible);
        taskRowsBox.setManaged(visible);
        refreshTaskRows();
        if (shown && hasRunningTasks()) {
            taskRefreshTimeline.playFromStart();
        } else {
            taskRefreshTimeline.stop();
        }
    }

    private TaskRow createTaskRow(DownloadTask task) {
        Label title = new Label();
        title.getStyleClass().add("app-download-task-title");
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);

        Label detail = new Label();
        detail.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        detail.setWrapText(false);
        detail.setMinWidth(0);
        detail.setMaxWidth(Double.MAX_VALUE);

        VBox text = new VBox(title, detail);
        text.getStyleClass().add("app-download-task-text");
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        CamlProgressBar progress = new CamlProgressBar();
        progress.getStyleClass().add("app-download-task-progress");

        CamlButton cancel = CamlButton.outlined(UiText.tr("common.cancel"));
        cancel.setOnAction(event -> {
            task.cancel();
            refreshTaskRows();
        });

        HBox row = new HBox(progress, text, cancel);
        row.getStyleClass().add("app-download-task-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return new TaskRow(row, title, detail, progress, cancel);
    }

    private void refreshTaskRows() {
        if (disposed) {
            return;
        }
        boolean anyRunning = false;
        for (Map.Entry<DownloadTask, TaskRow> entry : taskRows.entrySet()) {
            DownloadTask task = entry.getKey();
            TaskRow row = entry.getValue();
            DownloadTelemetry.Snapshot snapshot = task.snapshot();
            boolean running = !snapshot.finished() && !snapshot.failed() && !snapshot.cancelled();
            anyRunning |= running;
            row.title().setText(taskHeadline(task, snapshot));
            row.detail().setText(taskDetail(task, snapshot));
            row.progress().setProgress(taskProgress(snapshot, running));
            row.cancel().setVisible(running);
            row.cancel().setManaged(running);
        }
        if (!anyRunning) {
            taskRefreshTimeline.stop();
        }
    }

    private static String taskHeadline(DownloadTask task, DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return UiText.tr("download.progress.completed");
        }
        if (snapshot.failed()) {
            return UiText.tr("download.progress.failed", snapshot.failureMessage());
        }
        if (snapshot.cancelled()) {
            return UiText.tr("download.progress.failedHint");
        }
        return task.title();
    }

    private static String taskDetail(DownloadTask task, DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return task.subject() + " · " + UiText.tr("download.progress.finishedHint");
        }
        if (snapshot.failed()) {
            return task.subject();
        }
        if (snapshot.cancelled()) {
            return task.subject();
        }
        return snapshot.currentFile().isBlank()
                ? snapshot.currentStage()
                : snapshot.currentFile();
    }

    private static double taskProgress(DownloadTelemetry.Snapshot snapshot, boolean running) {
        if (snapshot.finished()) {
            return 1;
        }
        if (snapshot.totalBytes() > 0) {
            return Math.max(0, Math.min(1,
                    snapshot.downloadedBytes() / (double) snapshot.totalBytes()));
        }
        return running ? ProgressBar.INDETERMINATE_PROGRESS : 0;
    }

    private record TaskRow(
            HBox container,
            Label title,
            Label detail,
            CamlProgressBar progress,
            CamlButton cancel
    ) {
    }

    private void setContentPage(Node next) {
        if (disposed) {
            return;
        }
        PageLifecycle previous = activeContentPage;
        if (previous != null) {
            previous.onHide();
        }
        contentHost.setPage(next);
        if (previous != null
                && !(previous instanceof DownloadVersionsView)
                && !(previous instanceof ModSearchView)
                && !(previous instanceof DownloadProgressView)) {
            previous.dispose();
        }
        activeContentPage = next instanceof PageLifecycle lifecycle ? lifecycle : null;
        if (activeContentPage != null) {
            activeContentPage.onShow();
        }
    }

    @Override
    public void onShow() {
        shown = true;
        if (activeContentPage != null) {
            activeContentPage.onShow();
        }
        refreshTaskRows();
        if (hasRunningTasks()) {
            taskRefreshTimeline.playFromStart();
        }
    }

    private boolean hasRunningTasks() {
        for (DownloadTask task : controller.downloadTasks()) {
            if (task.isRunning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onHide() {
        shown = false;
        taskRefreshTimeline.stop();
        if (activeContentPage != null) {
            activeContentPage.onHide();
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        controller.downloadTasks().removeListener(taskListListener);
        taskRefreshTimeline.stop();
        taskRows.clear();
        if (activeContentPage != null) {
            activeContentPage.onHide();
            activeContentPage.dispose();
            activeContentPage = null;
        }
        for (DownloadVersionsView view : versionsViews.values()) {
            view.dispose();
        }
        versionsViews.clear();
        if (modsView != null) {
            modsView.dispose();
            modsView = null;
        }
        if (shadersView != null) {
            shadersView.dispose();
            shadersView = null;
        }
        if (resourcePacksView != null) {
            resourcePacksView.dispose();
            resourcePacksView = null;
        }
    }

    private void setDownloadNavigationVisible(boolean visible) {
        sidebar.setVisible(visible);
        sidebar.setManaged(visible);
    }

    private DownloadVersionsView versionsView(VersionCategory category) {
        return versionsViews.computeIfAbsent(category,
                key -> new DownloadVersionsView(controller, key, this::showVersionSetupPage));
    }

    private void updateSidebarWidth() {
        Scene scene = getScene();
        double width = scene == null ? getWidth() : scene.getWidth();
        double factor = ViewSupport.uiFactor(this);
        double sidebarWidth = width <= 0
                ? UiScale.size(ModDownloadLayout.SIDEBAR_MIN_WIDTH, factor)
                : ModDownloadLayout.sidebarWidth(width, factor);
        sidebar.setMinWidth(sidebarWidth);
        sidebar.setPrefWidth(sidebarWidth);
        sidebar.setMaxWidth(sidebarWidth);
    }
}
