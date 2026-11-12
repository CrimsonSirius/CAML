package org.chs.camlauncher.front.view;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.text.UiText;
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
    private final CamlPageHost contentHost;
    private final Map<VersionCategory, DownloadVersionsView> versionsViews =
            new EnumMap<>(VersionCategory.class);
    private VersionCategory currentCategory = VersionCategory.RELEASE;
    private boolean showingVersionList = true;
    private PageLifecycle activeContentPage;
    private ModSearchView modsView;
    private boolean disposed;

    DownloadsView(DownloadsController controller, LaunchSettings launchSettings) {
        this.controller = controller;
        this.launchSettings = launchSettings;
        sidebar = new CamlSideNav();
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setMaxHeight(Double.MAX_VALUE);
        CamlSideNavItem releaseItem = new CamlSideNavItem(VersionCategory.RELEASE.label());
        CamlSideNavItem snapshotItem = new CamlSideNavItem(VersionCategory.SNAPSHOT.label());
        CamlSideNavItem oldItem = new CamlSideNavItem(VersionCategory.OLD.label());
        CamlSideNavItem modItem = new CamlSideNavItem(UiText.tr("mods.nav"));
        sidebar.addItem(releaseItem);
        sidebar.addItem(snapshotItem);
        sidebar.addItem(oldItem);
        sidebar.addItem(modItem);
        sidebar.select(releaseItem);

        DownloadVersionsView initialView = versionsView(VersionCategory.RELEASE);
        contentHost = new CamlPageHost(initialView);
        activeContentPage = initialView;
        initialView.onShow();
        contentHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(contentHost, Priority.ALWAYS);

        releaseItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.RELEASE));
        snapshotItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.SNAPSHOT));
        oldItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.OLD));
        modItem.addEventHandler(ActionEvent.ACTION, event -> showMods());

        getChildren().addAll(sidebar, contentHost);
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

    private void showMods() {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        if (modsView == null) {
            modsView = new ModSearchView(controller, this::showModDetail);
        }
        setContentPage(modsView);
    }

    private void showModDetail(ModProjectSummary project) {
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        setContentPage(new ModDetailView(
                controller,
                launchSettings,
                project,
                this::showMods
        ));
    }

    private void startDownload(DownloadRequest request) {
        setDownloadNavigationVisible(false);
        DownloadTelemetry telemetry = new DownloadTelemetry();
        DownloadProgressView page = new DownloadProgressView(
                request,
                telemetry,
                () -> showVersions(currentCategory)
        );
        showingVersionList = false;
        setContentPage(page);

        Runnable onSuccess = () -> {
            telemetry.markCompleted();
            launchSettings.setSelectedVersion(request.instanceId());
            page.refreshNow();
        };
        Consumer<String> onError = message -> {
            telemetry.markFailed(message);
            page.refreshNow();
        };

        if (request.optiFine() != null) {
            controller.downloadForgeOptiFineVersion(request.version().id(), request.instanceId(),
                    request.forge(), request.optiFine(), request.gameDirectory(),
                    telemetry, onSuccess, onError);
        } else if (request.forge() != null) {
            controller.downloadForgeVersion(request.version().id(), request.instanceId(),
                    request.forge(), request.gameDirectory(), telemetry, onSuccess, onError);
        } else if (request.fabricLoader() != null) {
            controller.downloadFabricVersion(request.version().id(), request.instanceId(),
                    request.fabricLoader().version(), request.fabricApi(), request.gameDirectory(),
                    telemetry, onSuccess, onError);
        } else if (request.neoForge() != null) {
            controller.downloadNeoForgeVersion(request.version().id(), request.instanceId(),
                    request.neoForge(), request.gameDirectory(), telemetry, onSuccess, onError);
        } else {
            controller.downloadVersion(request.version().id(), request.instanceId(),
                    request.gameDirectory(), telemetry, onSuccess, onError);
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
                && !(previous instanceof ModSearchView)) {
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
    }

    private void setDownloadNavigationVisible(boolean visible) {
        sidebar.setVisible(visible);
        sidebar.setManaged(visible);
    }

    private DownloadVersionsView versionsView(VersionCategory category) {
        return versionsViews.computeIfAbsent(category,
                key -> new DownloadVersionsView(controller, key, this::showVersionSetupPage));
    }
}
