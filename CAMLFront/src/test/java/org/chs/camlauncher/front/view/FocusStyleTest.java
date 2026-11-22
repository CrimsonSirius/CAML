package org.chs.camlauncher.front.view;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlCheckBox;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlRadioButton;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 键盘焦点可见性回归测试。 */
class FocusStyleTest {
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void pageHostDoesNotJoinFocusTraversal() throws Exception {
        boolean traversable = onFxThread(() -> {
            CamlPageHost host = new CamlPageHost(new StackPane());
            return host.isFocusTraversable();
        });

        assertFalse(traversable, "页面容器不能成为不可见的焦点落点");
    }

    @Test
    void selectedSideNavigationStillShowsFocusRing() throws Exception {
        boolean visible = onFxThread(() -> {
            CamlSideNav nav = new CamlSideNav();
            nav.getStyleClass().add("app-global-nav");
            CamlSideNavItem item = new CamlSideNavItem("设置");
            nav.addItem(item);

            VBox root = new VBox(nav);
            applyStyles(root);
            assertNull(item.getEffect(), "未聚焦时不应显示焦点环");

            item.pseudoClassStateChanged(FOCUSED, true);
            applyStyles(root);
            return item.getEffect() != null;
        });

        assertTrue(visible, "已选中的侧栏项获得焦点时仍必须显示焦点环");
    }

    @Test
    void selectedRadioButtonShowsFocusRing() throws Exception {
        boolean visible = onFxThread(() -> {
            CamlRadioButton radio = new CamlRadioButton("默认");
            radio.setSelected(true);

            VBox root = new VBox(radio);
            applyStyles(root);
            Region circle = region(root, ".radio");
            assertNull(circle.getEffect(), "未聚焦时不应显示焦点环");

            radio.pseudoClassStateChanged(FOCUSED, true);
            applyStyles(root);
            return circle.getEffect() != null;
        });

        assertTrue(visible, "已选中的单选按钮获得焦点时仍必须显示焦点环");
    }

    @Test
    void checkBoxShowsFocusRing() throws Exception {
        boolean visible = onFxThread(() -> {
            CamlCheckBox checkBox = new CamlCheckBox("记住");
            checkBox.setSelected(true);

            VBox root = new VBox(checkBox);
            applyStyles(root);
            Region box = region(root, ".box");
            assertNull(box.getEffect(), "未聚焦时不应显示焦点环");

            checkBox.pseudoClassStateChanged(FOCUSED, true);
            applyStyles(root);
            return box.getEffect() != null;
        });

        assertTrue(visible, "复选框获得焦点时必须显示焦点环");
    }

    @Test
    void instanceModToggleShowsFocusRing() throws Exception {
        boolean visible = onFxThread(() -> {
            ToggleButton toggle = new ToggleButton("停用");
            toggle.getStyleClass().add("app-instance-mod-toggle");

            VBox root = new VBox(toggle);
            applyStyles(root);
            assertNull(toggle.getEffect(), "未聚焦时不应显示焦点环");

            toggle.pseudoClassStateChanged(FOCUSED, true);
            applyStyles(root);
            return toggle.getEffect() != null;
        });

        assertTrue(visible, "模组启停按钮获得焦点时必须显示焦点环");
    }

    private static void applyStyles(Parent root) {
        if (root.getScene() == null) {
            Scene scene = new Scene(root, 400, 300);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
        }
        root.applyCss();
        root.resize(400, 300);
        root.applyCss();
        root.layout();
    }

    private static Region region(Parent root, String selector) {
        Node node = root.lookup(selector);
        assertNotNull(node, "找不到控件内部节点 " + selector);
        return (Region) node;
    }
}
