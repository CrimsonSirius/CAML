package org.chs.camlauncher.front.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlProgressBar;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** 下载页：左侧版本类别导航和右侧版本列表/下载设置页。 */
final class DownloadsView extends HBox implements SidebarPage, PageLifecycle {
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final CamlSideNav sidebar;
    private final CamlPageHost contentHost;
    private final HBox taskBar = new HBox();
    private final Label taskTitle = new Label();
    private final Label taskDetail = new Label();
    private final CamlProgressBar taskProgress = new CamlProgressBar();
    private final CamlButton taskViewButton = CamlButton.text(UiText.tr("download.progress.progress"));
    private final CamlButton taskCancelButton = CamlButton.outlined(UiText.tr("common.cancel"));
    private final Timeline taskRefreshTimeline = new Timeline(
            new KeyFrame(Duration.millis(250), event -> refreshTaskBar()));
    private final Map<VersionCategory, DownloadVersionsView> versionsViews =
            new EnumMap<>(VersionCategory.class);
    private VersionCategory currentCategory = VersionCategory.RELEASE;
    private boolean showingVersionList = true;
    private PageLifecycle activeContentPage;
    private ModSearchView modsView;
    private ModSearchView shadersView;
    private ModSearchView resourcePacksView;
    private DownloadRequest activeRequest;
    private DownloadTelemetry activeTelemetry;
    private DownloadCancellationToken activeToken;
    private FrontTask<?> activeTask;
    private DownloadProgressView activeProgressPage;
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

        CamlSideNavItem releaseItem = new CamlSideNavItem(VersionCategory.RELEASE.label());
        CamlSideNavItem snapshotItem = new CamlSideNavItem(VersionCategory.SNAPSHOT.label());
        CamlSideNavItem oldItem = new CamlSideNavItem(VersionCategory.OLD.label());
        CamlSideNavItem modItem = new CamlSideNavItem(UiText.tr("mods.nav"));
        CamlSideNavItem shaderItem = new CamlSideNavItem(UiText.tr("shaders.nav"));
        CamlSideNavItem resourcePackItem = new CamlSideNavItem(UiText.tr("resourcepacks.nav"));
        sidebar.addItem(releaseItem);
        sidebar.addItem(snapshotItem);
        sidebar.addItem(oldItem);
        sidebar.addItem(modItem);
        sidebar.addItem(shaderItem);
        sidebar.addItem(resourcePackItem);
        sidebar.select(releaseItem);

        DownloadVersionsView initialView = versionsView(VersionCategory.RELEASE);
        contentHost = new CamlPageHost(initialView);
        activeContentPage = initialView;
        initialView.onShow();
        contentHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        buildTaskBar();
        taskRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        VBox contentColumn = new VBox(taskBar, contentHost);
        contentColumn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(contentColumn, Priority.ALWAYS);

        releaseItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.RELEASE));
        snapshotItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.SNAPSHOT));
        oldItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.OLD));
        modItem.addEventHandler(ActionEvent.ACTION, event -> showMods());
        shaderItem.addEventHandler(ActionEvent.ACTION, event -> showShaders());
        resourcePackItem.addEventHandler(ActionEvent.ACTION, event -> showResourcePacks());

        getChildren().add(contentColumn);
        getStyleClass().add("app-download-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private void buildTaskBar() {
        taskTitle.getStyleClass().add("app-download-task-title");
        taskTitle.setMinWidth(0);
        taskTitle.setMaxWidth(Double.MAX_VALUE);
        taskDetail.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        taskDetail.setWrapText(false);
        taskDetail.setMinWidth(0);
        taskDetail.setMaxWidth(Double.MAX_VALUE);
        VBox text = new VBox(taskTitle, taskDetail);
        text.getStyleClass().add("app-download-task-text");
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        taskProgress.getStyleClass().add("app-download-task-progress");

        taskViewButton.setOnAction(event -> showActiveProgress());
        taskCancelButton.setOnAction(event -> cancelActiveDownload());

        taskBar.getChildren().setAll(taskProgress, text, taskViewButton, taskCancelButton);
        taskBar.getStyleClass().add("app-download-task-bar");
        taskBar.setAlignment(Pos.CENTER_LEFT);
        taskBar.setVisible(false);
        taskBar.setManaged(false);
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

    private void startDownload(DownloadRequest request) {
        if (isTaskRunning()) {
            showActiveProgress();
            return;
        }

        DownloadProgressView previousProgress = activeProgressPage;
        clearTaskState();
        DownloadTelemetry telemetry = new DownloadTelemetry();
        DownloadCancellationToken token = new DownloadCancellationToken();
        DownloadProgressView page = new DownloadProgressView(
                request,
                telemetry,
                this::backFromProgress
        );
        activeRequest = request;
        activeTelemetry = telemetry;
        activeToken = token;
        activeProgressPage = page;
        setDownloadNavigationVisible(false);
        showingVersionList = false;
        setContentPage(page);
        if (previousProgress != null && previousProgress != page) {
            previousProgress.dispose();
        }

        Runnable onSuccess = () -> {
            telemetry.markCompleted();
            launchSettings.setSelectedVersion(request.instanceId());
            page.refreshNow();
            refreshTaskBar();
        };
        Consumer<String> onError = message -> {
            telemetry.markFailed(message);
            page.refreshNow();
            refreshTaskBar();
        };

        FrontTask<?> task;
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
        activeTask = task;
        refreshTaskBar();
        taskRefreshTimeline.playFromStart();
    }

    private void showActiveProgress() {
        if (activeProgressPage == null) {
            return;
        }
        setDownloadNavigationVisible(false);
        showingVersionList = false;
        setContentPage(activeProgressPage);
    }

    private void backFromProgress() {
        showVersions(currentCategory);
        if (!isTaskRunning()) {
            DownloadProgressView finishedPage = activeProgressPage;
            clearTaskState();
            if (finishedPage != null && finishedPage != activeContentPage) {
                finishedPage.dispose();
            }
        }
    }

    private void cancelActiveDownload() {
        FrontTask<?> task = activeTask;
        if (task == null) {
            return;
        }
        boolean cancelled = task.cancel();
        if (!cancelled) {
            refreshTaskBar();
            return;
        }
        if (activeToken != null) {
            activeToken.cancel();
        }
        if (activeTelemetry != null) {
            activeTelemetry.markCancelled();
        }
        if (activeProgressPage != null) {
            activeProgressPage.refreshNow();
        }
        refreshTaskBar();
    }

    private boolean isTaskRunning() {
        if (activeTelemetry == null) {
            return false;
        }
        DownloadTelemetry.Snapshot snapshot = activeTelemetry.snapshot();
        return !snapshot.finished() && !snapshot.failed() && !snapshot.cancelled();
    }

    private void refreshTaskBar() {
        if (activeRequest == null || activeTelemetry == null) {
            taskBar.setVisible(false);
            taskBar.setManaged(false);
            return;
        }

        DownloadTelemetry.Snapshot snapshot = activeTelemetry.snapshot();
        boolean running = !snapshot.finished() && !snapshot.failed() && !snapshot.cancelled();
        taskBar.setVisible(true);
        taskBar.setManaged(true);
        taskTitle.setText(taskTitle(snapshot));
        taskDetail.setText(taskDetail(snapshot));
        taskProgress.setProgress(taskProgress(snapshot, running));
        taskCancelButton.setVisible(running);
        taskCancelButton.setManaged(running);
        taskViewButton.setDisable(activeProgressPage == null);
        if (!running) {
            taskRefreshTimeline.stop();
        }
    }

    private String taskTitle(DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return UiText.tr("download.progress.completed");
        }
        if (snapshot.failed()) {
            return UiText.tr("download.progress.failed", snapshot.failureMessage());
        }
        if (snapshot.cancelled()) {
            return UiText.tr("download.progress.failedHint");
        }
        return UiText.tr("download.progress.title", activeRequest.instanceId());
    }

    private String taskDetail(DownloadTelemetry.Snapshot snapshot) {
        if (snapshot.finished()) {
            return UiText.tr("download.progress.finishedHint");
        }
        if (snapshot.failed() || snapshot.cancelled()) {
            return activeRequest.instanceId();
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

    private void clearTaskState() {
        taskRefreshTimeline.stop();
        activeRequest = null;
        activeTelemetry = null;
        activeToken = null;
        activeTask = null;
        activeProgressPage = null;
        taskBar.setVisible(false);
        taskBar.setManaged(false);
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
        if (activeContentPage != null) {
            activeContentPage.onShow();
        }
        refreshTaskBar();
        if (isTaskRunning()) {
            taskRefreshTimeline.playFromStart();
        }
    }

    @Override
    public void onHide() {
        taskRefreshTimeline.stop();
        if (activeContentPage != null) {
            activeContentPage.onHide();
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        taskRefreshTimeline.stop();
        if (activeProgressPage != null && activeProgressPage != activeContentPage) {
            activeProgressPage.dispose();
        }
        activeProgressPage = null;
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
