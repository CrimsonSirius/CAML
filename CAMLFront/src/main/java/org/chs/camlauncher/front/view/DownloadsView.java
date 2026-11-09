package org.chs.camlauncher.front.view;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** 下载页：左侧版本类别导航和右侧版本列表/下载设置页。 */
final class DownloadsView extends HBox implements SidebarPage {
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final CamlSideNav sidebar;
    private final CamlPageHost contentHost;
    private final Map<VersionCategory, DownloadVersionsView> versionsViews =
            new EnumMap<>(VersionCategory.class);
    private VersionCategory currentCategory = VersionCategory.RELEASE;
    private boolean showingVersionList = true;
    private DownloadProgressView progressView;

    DownloadsView(DownloadsController controller, LaunchSettings launchSettings) {
        this.controller = controller;
        this.launchSettings = launchSettings;
        sidebar = new CamlSideNav();
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setMaxHeight(Double.MAX_VALUE);
        CamlSideNavItem releaseItem = new CamlSideNavItem(VersionCategory.RELEASE.label());
        CamlSideNavItem snapshotItem = new CamlSideNavItem(VersionCategory.SNAPSHOT.label());
        CamlSideNavItem oldItem = new CamlSideNavItem(VersionCategory.OLD.label());
        sidebar.addItem(releaseItem);
        sidebar.addItem(snapshotItem);
        sidebar.addItem(oldItem);
        sidebar.select(releaseItem);

        contentHost = new CamlPageHost(versionsView(VersionCategory.RELEASE));
        contentHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(contentHost, Priority.ALWAYS);

        releaseItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.RELEASE));
        snapshotItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.SNAPSHOT));
        oldItem.addEventHandler(ActionEvent.ACTION,
                event -> showVersions(VersionCategory.OLD));

        getChildren().addAll(sidebar, contentHost);
        getStyleClass().add("app-download-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    @Override
    public Node sidebar() {
        return sidebar;
    }

    void refreshVersions() {
        versionsViews.clear();
        if (showingVersionList) {
            contentHost.setPage(versionsView(currentCategory));
        }
    }

    private void showVersionSetupPage(VersionManifest.Version version) {
        disposeProgressView();
        setDownloadNavigationVisible(true);
        showingVersionList = false;
        contentHost.setPage(new DownloadVersionSetupView(
                version,
                controller,
                launchSettings,
                () -> showVersions(currentCategory),
                this::startDownload
        ));
    }

    private void showVersions(VersionCategory category) {
        disposeProgressView();
        setDownloadNavigationVisible(true);
        showingVersionList = true;
        currentCategory = category;
        contentHost.setPage(versionsView(category));
    }

    private void startDownload(DownloadRequest request) {
        disposeProgressView();
        setDownloadNavigationVisible(false);
        DownloadTelemetry telemetry = new DownloadTelemetry();
        DownloadProgressView page = new DownloadProgressView(
                request,
                telemetry,
                () -> showVersions(currentCategory)
        );
        progressView = page;
        showingVersionList = false;
        contentHost.setPage(page);

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

    private void disposeProgressView() {
        if (progressView != null) {
            progressView.dispose();
            progressView = null;
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
