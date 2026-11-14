package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 下载页侧栏在页面卸载后重新挂载时不能被组件库的固定宽度压回去。 */
class SidebarWidthRegressionTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void sidebarKeepsAssignedWidthAfterReattach() throws Exception {
        double[] widths = onFxThread(() -> {
            CamlSideNav sidebar = new CamlSideNav();
            sidebar.getStyleClass().add("app-sidebar");
            sidebar.addItem(new CamlSideNavItem("正式版"));
            sidebar.setMinWidth(260);
            sidebar.setPrefWidth(260);
            sidebar.setMaxWidth(260);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox root = new HBox(sidebar, spacer);
            root.setPrefSize(1568, 900);
            Scene scene = new Scene(root, 1568, 900);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
            layout(root, scene);
            double first = sidebar.getWidth();

            scene.setRoot(new Pane());
            scene.setRoot(root);
            layout(root, scene);
            return new double[]{first, sidebar.getWidth()};
        });

        assertEquals(260, widths[0], 1.0, "首次挂载时侧栏宽度应使用代码设置值");
        assertEquals(260, widths[1], 1.0, "重新挂载后侧栏宽度不能被 CSS 固定值覆盖");
    }

    @Test
    void settingsSidebarKeepsItsItemsWideEnoughForLabels() throws Exception {
        double[] metrics = onFxThread(() -> {
            CamlSideNav sidebar = new CamlSideNav();
            sidebar.getStyleClass().add("app-settings-sidebar");
            CamlSideNavItem game = new CamlSideNavItem("游戏");
            sidebar.addItem(game);
            sidebar.addItem(new CamlSideNavItem("下载"));
            sidebar.addItem(new CamlSideNavItem("外观"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox root = new HBox(sidebar, spacer);
            root.setPrefSize(1568, 900);
            Scene scene = new Scene(root, 1568, 900);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
            layout(root, scene);

            double itemWidth = game.getWidth();
            double itemPreferredWidth = game.prefWidth(-1);
            scene.setRoot(new Pane());
            scene.setRoot(root);
            layout(root, scene);
            return new double[]{itemWidth, itemPreferredWidth, game.getWidth()};
        });

        assertTrue(metrics[0] + 0.5 >= metrics[1], "首次挂载时导航项宽度必须容纳完整标签");
        assertTrue(metrics[2] + 0.5 >= metrics[1], "重新挂载后导航项宽度仍必须容纳完整标签");
    }

    private static void layout(HBox root, Scene scene) {
        root.applyCss();
        root.resize(scene.getWidth(), scene.getHeight());
        root.applyCss();
        root.layout();
    }
}
