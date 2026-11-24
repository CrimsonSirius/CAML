package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlTextArea;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 日志内容区在深色主题下不能留下圆角缺口。 */
class LogPaneStyleTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void logPreviewAndTextAreaUseSquareCorners() throws Exception {
        boolean square = onFxThread(() -> {
            javafx.scene.layout.VBox pane = new javafx.scene.layout.VBox();
            pane.getStyleClass().add("app-log-pane");
            Label preview = new Label("暂无活动记录");
            preview.getStyleClass().add("app-log-preview");
            CamlTextArea area = new CamlTextArea();
            area.getStyleClass().add("app-log-area");

            VBox root = new VBox(pane, preview, area);
            applyStyles(root);

            return isSquare(pane) && isSquare(preview) && isSquare(area);
        });

        assertTrue(square, "日志面板和内容区都不能保留圆形角");
    }

    private static boolean isSquare(Region control) {
        var fills = control.getBackground().getFills();
        if (fills.isEmpty()) {
            return false;
        }
        var radii = fills.getFirst().getRadii();
        return radii.getTopLeftHorizontalRadius() == 0
                && radii.getTopRightHorizontalRadius() == 0
                && radii.getBottomLeftHorizontalRadius() == 0
                && radii.getBottomRightHorizontalRadius() == 0;
    }

    private static void applyStyles(VBox root) {
        Scene scene = new Scene(root, 400, 300);
        CamlTheme.install(scene);
        addStylesheet(scene, "/themes/night.css");
        addStylesheet(scene, "/styles/app.css");
        root.applyCss();
        root.resize(400, 300);
        root.applyCss();
        root.layout();
    }
}
