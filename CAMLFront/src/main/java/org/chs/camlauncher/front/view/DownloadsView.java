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

/** 下载页：左侧“添加游戏实例”导航和右侧版本列表/下载设置页。 */
final class DownloadsView extends HBox implements SidebarPage {
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final CamlSideNav sidebar;
    private final CamlPageHost contentHost;
    private final DownloadVersionsView versionsView;

    DownloadsView(DownloadsController controller, LaunchSettings launchSettings) {
        this.controller = controller;
        this.launchSettings = launchSettings;
        sidebar = new CamlSideNav();
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setMaxHeight(Double.MAX_VALUE);
        CamlSideNavItem addInstance = new CamlSideNavItem("添加游戏实例");
        sidebar.addItem(addInstance);
        sidebar.select(addInstance);

        versionsView = new DownloadVersionsView(controller, this::showVersionSetupPage);
        contentHost = new CamlPageHost(versionsView);
        contentHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(contentHost, Priority.ALWAYS);

        addInstance.addEventHandler(ActionEvent.ACTION, event -> contentHost.setPage(versionsView));

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
                () -> contentHost.setPage(versionsView)
        ));
    }
}
