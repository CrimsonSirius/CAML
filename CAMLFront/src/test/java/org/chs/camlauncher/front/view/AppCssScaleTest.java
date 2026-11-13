package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.UiScale;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 界面整体缩放的回归测试。
 *
 * <p>{@code MainView} 只把缩放因子换算成根节点字号写进内联样式，
 * 其余尺寸全部依赖 {@code caml.css} 与 {@code app.css} 中的 {@code em}。
 * 这里在真实样式表下验证：根字号放大后，字号、内边距和最大宽度都按同一比例放大。
 * 若有人在样式表里写回固定 {@code px}，该测试就会失败。</p>
 */
class AppCssScaleTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void typographyAndGeometryScaleWithRootFont() throws Exception {
        Metrics base = onFxThread(() -> measure(UiScale.DEFAULT_FACTOR));

        // 设计基准：1em = 16px，标题 1.875em，分组内边距 1.5em，内容列上限 35em。
        assertEquals(30, base.headingFontPx(), 0.5, "标题字号应为 1.875em");
        assertEquals(24, base.groupPadding(), 0.5, "分组内边距应为 1.5em");
        assertEquals(560, base.contentMaxWidth(), 0.5, "内容列上限应为 35em");
    }

    @Test
    void scalingUpKeepsEveryProportion() throws Exception {
        Metrics base = onFxThread(() -> measure(UiScale.DEFAULT_FACTOR));
        Metrics scaled = onFxThread(() -> measure(UiScale.MAX_FACTOR));

        assertEquals(UiScale.MAX_FACTOR, scaled.headingFontPx() / base.headingFontPx(), 0.01,
                "字体应按因子等比放大");
        assertEquals(UiScale.MAX_FACTOR, scaled.groupPadding() / base.groupPadding(), 0.01,
                "内边距应按因子等比放大");
        assertEquals(UiScale.MAX_FACTOR, scaled.contentMaxWidth() / base.contentMaxWidth(), 0.01,
                "最大宽度应按因子等比放大");
        assertEquals(UiScale.MAX_FACTOR, scaled.rowMinHeight() / base.rowMinHeight(), 0.01,
                "控件最小高度应按因子等比放大");
    }

    @Test
    void scalingDownKeepsEveryProportion() throws Exception {
        Metrics base = onFxThread(() -> measure(UiScale.DEFAULT_FACTOR));
        Metrics scaled = onFxThread(() -> measure(UiScale.MIN_FACTOR));

        assertEquals(UiScale.MIN_FACTOR, scaled.headingFontPx() / base.headingFontPx(), 0.01);
        assertEquals(UiScale.MIN_FACTOR, scaled.groupPadding() / base.groupPadding(), 0.01);
        assertEquals(UiScale.MIN_FACTOR, scaled.contentMaxWidth() / base.contentMaxWidth(), 0.01);
        assertEquals(UiScale.MIN_FACTOR, scaled.rowMinHeight() / base.rowMinHeight(), 0.01);
    }

    private record Metrics(
            double headingFontPx,
            double groupPadding,
            double contentMaxWidth,
            double rowMinHeight) {
    }

    private static Metrics measure(double factor) {
        Label heading = new Label("CAML");
        heading.getStyleClass().add("app-download-progress-heading");

        VBox group = new VBox(heading);
        group.getStyleClass().addAll("app-settings-group", "app-settings-content");

        Label buttonLike = new Label("启动");
        buttonLike.getStyleClass().add("app-download-primary-button");

        VBox root = new VBox(group, buttonLike);
        // 与 MainView.updateUiScale 相同：由缩放因子推导根字号，其余全部交给 CSS 的 em。
        root.setStyle("-fx-font-size: " + (int) UiScale.baseFontPx(factor) + "px;");

        Scene scene = new Scene(root, 900, 600);
        CamlTheme.install(scene);
        addStylesheet(scene, "/themes/mango.css");
        addStylesheet(scene, "/styles/app.css");
        root.applyCss();
        root.resize(900, 600);
        root.applyCss();
        root.layout();

        return new Metrics(
                heading.getFont().getSize(),
                group.getPadding().getTop(),
                group.maxWidth(-1),
                buttonLike.minHeight(-1));
    }
}
