package org.chs.camlauncher.front.view;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** 下载页：当前只保留左侧空白侧边栏。 */
final class DownloadsView extends HBox implements SidebarPage {
    private final VBox sidebar;

    DownloadsView() {
        Region content = new Region();
        HBox.setHgrow(content, Priority.ALWAYS);

        sidebar = ViewSupport.emptySidebar();
        getChildren().addAll(sidebar, content);
        getStyleClass().add("app-download-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    @Override
    public VBox sidebar() {
        return sidebar;
    }
}
