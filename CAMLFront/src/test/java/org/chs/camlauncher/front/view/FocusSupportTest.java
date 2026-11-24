package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTopNavItem;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 焦点交接回归测试：移除焦点节点后不能让导航胶囊接管焦点。 */
class FocusSupportTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void restoreFocusSkipsNavigationItems() throws Exception {
        Node target = onFxThread(() -> {
            CamlTopNavItem navigationItem = new CamlTopNavItem("设置");
            StackPane fallback = new StackPane();
            VBox root = new VBox(navigationItem, fallback);
            show(root);

            return FocusSupport.resolveRestoreTarget(navigationItem, fallback);
        });

        assertFalse(target instanceof CamlTopNavItem, "程序化恢复焦点不应落到导航胶囊上");
        assertTrue(target instanceof StackPane, "导航项不可用时焦点应交给页面容器");
    }

    @Test
    void restoreFocusKeepsOrdinaryPageControl() throws Exception {
        boolean same = onFxThread(() -> {
            CamlButton preferred = CamlButton.filled("下载 Java");
            StackPane fallback = new StackPane();
            VBox root = new VBox(preferred, fallback);
            show(root);

            return FocusSupport.resolveRestoreTarget(preferred, fallback) == preferred;
        });

        assertTrue(same, "普通页面控件应正常成为焦点目标");
    }

    @Test
    void restoreFocusIgnoresDetachedNode() throws Exception {
        boolean fallbackSelected = onFxThread(() -> {
            Button detached = new Button("旧页面");
            StackPane fallback = new StackPane();
            VBox root = new VBox(fallback);
            show(root);

            return FocusSupport.resolveRestoreTarget(detached, fallback) == fallback;
        });

        assertTrue(fallbackSelected, "已脱离场景的旧控件不能接收焦点");
    }

    @Test
    void focusFirstSkipsDisabledControls() throws Exception {
        boolean enabledSelected = onFxThread(() -> {
            CamlButton disabled = CamlButton.filled("不可用");
            disabled.setDisable(true);
            CamlButton enabled = CamlButton.filled("可用");
            StackPane fallback = new StackPane();
            VBox root = new VBox(disabled, enabled, fallback);
            show(root);

            return FocusSupport.resolveFirstTarget(root, fallback) == enabled;
        });

        assertTrue(enabledSelected, "应选择第一个可用控件，跳过禁用项");
    }

    private static void show(VBox root) {
        Scene scene = new Scene(root, 400, 300);
        CamlTheme.install(scene);
        FxTestSupport.addStylesheet(scene, "/themes/mango.css");
        FxTestSupport.addStylesheet(scene, "/styles/app.css");
        root.applyCss();
        root.resize(400, 300);
        root.layout();
    }
}
