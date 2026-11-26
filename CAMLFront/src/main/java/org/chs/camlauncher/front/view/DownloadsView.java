package org.chs.camlauncher.front.view;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
import org.chs.camlauncher.ui.control.CamlPageHost;
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
    private final CamlSideNavItem releaseItem;
    private final CamlSideNavItem snapshotItem;
    private final CamlSideNavItem oldItem;
    private final CamlPageHost contentHost;
    private final Map<VersionCategory, DownloadVersionsView> versionsViews =
            new EnumMap<>(VersionCategory.class);
    private VersionCategory currentCategory = VersionCategory.RELEASE;
    private boolean showingVersionList = true;
    private PageLifecycle activeContentPage;
    private ModSearchView modpacksView;
    private ModSearchView modsView;
    private ModSearchView shadersView;
    private ModSearchView resourcePacksView;
    private ModSearchView mapsView;
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
        CamlSideNavItem modpackItem = new CamlSideNavItem(UiText.tr("modpacks.nav"));
        CamlSideNavItem modItem = new CamlSideNavItem(UiText.tr("mods.nav"));
        CamlSideNavItem shaderItem = new CamlSideNavItem(UiText.tr("shaders.nav"));
        CamlSideNavItem resourcePackItem = new CamlSideNavItem(UiText.tr("resourcepacks.nav"));
        CamlSideNavItem mapItem = new CamlSideNavItem(UiText.tr("maps.nav"));
        sidebar.addSection(UiText.tr("downloads.section.instances"));
        sidebar.addItem(releaseItem);
        sidebar.addItem(snapshotItem);
        sidebar.addItem(oldItem);
        sidebar.addItem(modpackItem);
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
        HBox.setHgrow(contentHost, Priority.ALWAYS);

        releaseItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.RELEASE));
        snapshotItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.SNAPSHOT));
        oldItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.OLD));
        modpackItem.addEventHandler(ActionEvent.ACTION, event -> showModpacks());
        modItem.addEventHandler(ActionEvent.ACTION, event -> showMods());
        shaderItem.addEventHandler(ActionEvent.ACTION, event -> showShaders());
        resourcePackItem.addEventHandler(ActionEvent.ACTION, event -> showResourcePacks());
        mapItem.addEventHandler(ActionEvent.ACTION, event -> showMaps());

        getChildren().add(contentHost);
        getStyleClass().add("app-download-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
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

    private void showModpacks() {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        if (modpacksView == null) {
            modpacksView = new ModSearchView(controller, ModContentType.MODPACK,
                    this::showModpackDetail);
        }
        setContentPage(modpacksView);
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

    private void showModpackDetail(ModProjectSummary project) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new ModDetailView(
                controller,
                launchSettings,
                ModContentType.MODPACK,
                project,
                this::showModpacks
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
                downloadTask,
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
        };
        Consumer<String> onError = message -> {
            downloadTask.fail(message);
            page.refreshNow();
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
    }

    @Override
    public void onHide() {
        if (activeContentPage != null) {
            activeContentPage.onHide();
        }
    }

    @Override
    public void dispose() {
        disposed = true;
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
        if (modpacksView != null) {
            modpacksView.dispose();
            modpacksView = null;
        }
        if (shadersView != null) {
            shadersView.dispose();
            shadersView = null;
        }
        if (resourcePacksView != null) {
            resourcePacksView.dispose();
            resourcePacksView = null;
        }
        if (mapsView != null) {
            mapsView.dispose();
            mapsView = null;
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
