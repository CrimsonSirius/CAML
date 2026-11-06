package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlTopBar;
import org.chs.camlauncher.ui.control.CamlTopNav;
import org.chs.camlauncher.ui.control.CamlTopNavItem;

import java.util.List;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 应用壳层：顶部导航、页面宿主和底栏。
 *
 * <p>具体页面拆分到 {@link HomeView}、{@link DownloadsView} 和
 * {@link SettingsView}，本类不持有启动表单、日志或设置控件。</p>
 */
public final class MainView extends BorderPane {
    private final MainController controller;
    private final HomeView homeView;
    private final DownloadsView downloadsView;
    private final SettingsView settingsView;
    private final CamlPageHost pageHost;

    public MainView(MainController controller, int initialMaxMemoryMb,
                    SystemMemoryInfo systemMemory, Path initialGameDirectory,
                    boolean fullVersionIsolation) {
        this.controller = controller;
        getStyleClass().add("app-root");

        LaunchSettings launchSettings = new LaunchSettings(
                initialMaxMemoryMb, initialGameDirectory, fullVersionIsolation);
        settingsView = new SettingsView(
                launchSettings,
                controller::saveMaxMemoryMbAsync,
                controller::saveVersionIsolationAsync,
                systemMemory,
                controller
        );
        homeView = new HomeView(controller, launchSettings);
        downloadsView = new DownloadsView(controller, launchSettings);
        pageHost = new CamlPageHost(homeView);

        setTop(buildTopBar());
        setCenter(pageHost);
        setBottom(buildFooter());
    }

    private CamlTopBar buildTopBar() {
        CamlTopBar bar = new CamlTopBar("CAML", "Minecraft Launcher");

        CamlTopNav navigation = new CamlTopNav();
        CamlTopNavItem home = navigation.addItem("主页");
        CamlTopNavItem downloads = navigation.addItem("下载");
        CamlTopNavItem settings = navigation.addItem("设置");
        navigation.select(home);
        navigation.selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected == home) {
                showPage(homeView);
            } else if (selected == downloads) {
                showPage(downloadsView);
            } else if (selected == settings) {
                showPage(settingsView);
            }
        });
        bar.addNavigation(navigation);
        return bar;
    }

    private void showPage(Node page) {
        Node sidebar = page instanceof SidebarPage sidebarPage ? sidebarPage.sidebar() : null;
        pageHost.setPage(page, sidebar);
    }

    private Label buildFooter() {
        BackendStatus backendStatus = controller.status();
        Label footer = new Label(String.format(
                "CAML %s · 后端%s · 数据目录: %s",
                backendStatus.version(),
                backendStatus.ready() ? "已就绪" : "未就绪",
                backendStatus.dataDirectory()));
        footer.getStyleClass().addAll("caml-body-small", "app-footer-bar");
        footer.setWrapText(true);
        footer.setMaxWidth(Double.MAX_VALUE);
        return footer;
    }

    /** 填充设置页的主题选择器；应用入口负责实际加载 CSS 并持久化。 */
    public void configureThemes(List<ThemeInfo> themes, ThemeInfo active, Consumer<ThemeInfo> onSelected) {
        settingsView.configureThemes(themes, active, onSelected);
    }
}
