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

/** 下载页：左侧版本类别导航和右侧版本列表/下载设置页。 */
final class DownloadsView extends HBox implements SidebarPage {
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final CamlSideNav sidebar;
    private final CamlPageHost contentHost;
    private final Map<VersionCategory, DownloadVersionsView> versionsViews =
            new EnumMap<>(VersionCategory.class);
    private VersionCategory currentCategory = VersionCategory.RELEASE;

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

    private void showVersionSetupPage(VersionManifest.Version version) {
        contentHost.setPage(new DownloadVersionSetupView(
                version,
                controller,
                launchSettings,
                () -> contentHost.setPage(versionsView(currentCategory))
        ));
    }

    private void showVersions(VersionCategory category) {
        currentCategory = category;
        contentHost.setPage(versionsView(category));
    }

    private DownloadVersionsView versionsView(VersionCategory category) {
        return versionsViews.computeIfAbsent(category,
                key -> new DownloadVersionsView(controller, key, this::showVersionSetupPage));
    }
}
