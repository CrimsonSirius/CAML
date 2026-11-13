package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.UiScale;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结果行的真实布局回归测试。
 *
 * <p>这里的行按 {@link ModViewSupport#resultRow} 组装，并套用真实的 {@code caml.css}、
 * 主题和 {@code app.css}。行高若被按钮样式的 {@code -fx-min-height: 44} 压住，
 * 图标与文字就会溢出到相邻行——正是该测试要守住的回归点。</p>
 */
class ModResultRowLayoutTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void rowHeightFollowsIconAndPadding() throws Exception {
        ModDownloadLayout.Plan plan = ModDownloadLayout.plan(1020, UiScale.DEFAULT_FACTOR);
        VBox rows = onFxThread(() -> layoutRows(plan, 610));
        Region first = (Region) rows.getChildren().get(0);
        Region second = (Region) rows.getChildren().get(1);

        assertEquals(plan.rowMinHeight(), first.getHeight(), 0.5,
                "行高必须容纳图标与上下内边距");
        assertEquals(plan.rowMinHeight(), second.getHeight(), 0.5);
        assertTrue(second.getLayoutY() - first.getLayoutY() >= plan.rowMinHeight(),
                "相邻行之间不能重叠");
        assertEquals(plan.listSpacing(),
                second.getLayoutY() - first.getLayoutY() - first.getHeight(), 0.5);
    }

    @Test
    void contentStaysInsideRow() throws Exception {
        ModDownloadLayout.Plan plan = ModDownloadLayout.plan(1020, UiScale.DEFAULT_FACTOR);
        VBox rows = onFxThread(() -> layoutRows(plan, 610));
        StackPane row = (StackPane) rows.getChildren().get(0);
        HBox content = (HBox) row.getChildren().get(2);

        assertEquals(plan.rowMinHeight(), content.getHeight(), 0.5);
        assertTrue(content.getHeight() <= row.getHeight() + 0.5, "正文不能高于行框");
        Region icon = (Region) content.getChildren().get(0);
        assertEquals(plan.iconSize(), icon.getHeight(), 0.5);
        assertEquals(plan.iconSize(), icon.getWidth(), 0.5);
        assertTrue(icon.getLayoutY() + icon.getHeight() <= row.getHeight() + 0.5,
                "图标不能溢出到行框之外");
    }

    @Test
    void hitButtonFillsWholeRow() throws Exception {
        ModDownloadLayout.Plan plan = ModDownloadLayout.plan(1020, UiScale.DEFAULT_FACTOR);
        VBox rows = onFxThread(() -> layoutRows(plan, 610));
        StackPane row = (StackPane) rows.getChildren().get(0);
        Region hit = (Region) row.getChildren().get(0);

        // 点击层铺满整行，正文层不拦截鼠标事件。
        assertEquals(row.getHeight(), hit.getHeight(), 0.5);
        assertEquals(row.getWidth(), hit.getWidth(), 0.5);
        assertTrue(row.getChildren().get(2).isMouseTransparent());
    }

    @Test
    void rowScalesWithUiFactor() throws Exception {
        // 放大后的行高与图标必须同比例变大；若代码几何与 CSS 字号脱钩，这里就会失配。
        ModDownloadLayout.Plan plan = ModDownloadLayout.plan(1020, UiScale.MAX_FACTOR);
        VBox rows = onFxThread(() -> layoutRows(plan, 760));
        StackPane row = (StackPane) rows.getChildren().get(0);
        HBox content = (HBox) row.getChildren().get(2);
        Region icon = (Region) content.getChildren().get(0);

        double expectedHeight = ModDownloadLayout.ROW_MIN_HEIGHT * UiScale.MAX_FACTOR;
        assertEquals(expectedHeight, row.getHeight(), 1.0,
                "行高应等于设计行高乘以缩放因子");
        assertEquals(ModDownloadLayout.ICON_SIZE * UiScale.MAX_FACTOR, icon.getHeight(), 1.0);
        assertTrue(icon.getLayoutY() + icon.getHeight() <= row.getHeight() + 0.5,
                "放大后图标仍不能溢出到行框之外");
    }

    private static VBox layoutRows(ModDownloadLayout.Plan plan, double width) {
        VBox list = new VBox(plan.listSpacing(), resultRow(plan), resultRow(plan));
        list.setPrefWidth(width);
        list.setMaxWidth(width);
        VBox root = new VBox(list);
        // 与 MainView.updateUiScale 相同：根字号由 UiScale 写入，CSS 的 em 随之等比缩放。
        root.setStyle("-fx-font-size: " + (int) UiScale.baseFontPx(plan.factor()) + "px;");
        Scene scene = new Scene(root, width, 400);
        CamlTheme.install(scene);
        addStylesheet(scene, "/themes/mango.css");
        addStylesheet(scene, "/styles/app.css");
        root.applyCss();
        root.resize(width, 400);
        root.applyCss();
        root.layout();
        return list;
    }

    private static StackPane resultRow(ModDownloadLayout.Plan plan) {
        int size = (int) Math.round(plan.iconSize());
        Label iconText = new Label("S");
        iconText.getStyleClass().add("app-mod-icon-text");
        StackPane placeholder = new StackPane(iconText);
        placeholder.getStyleClass().add("app-mod-icon-placeholder");
        placeholder.setMinSize(size, size);
        placeholder.setPrefSize(size, size);
        placeholder.setMaxSize(size, size);
        StackPane icon = new StackPane(placeholder);
        icon.getStyleClass().add("app-mod-icon-frame");
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);

        Label name = new Label("Fabric API");
        name.getStyleClass().add("app-mod-title");
        name.setWrapText(false);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        Label source = new Label("Modrinth");
        source.getStyleClass().add("app-mod-source");
        source.setMinWidth(Region.USE_PREF_SIZE);
        HBox heading = new HBox(name, source);
        heading.getStyleClass().add("app-mod-title-row");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label summary = new Label("Lightweight and modular API providing common hooks");
        summary.getStyleClass().add("app-mod-summary");
        summary.setWrapText(false);
        summary.setTextOverrun(OverrunStyle.ELLIPSIS);
        summary.setMinWidth(0);
        summary.setMaxWidth(Double.MAX_VALUE);
        Label meta = new Label("Fabric · 1.14 / 1.14-pre1 / 1.15");
        meta.getStyleClass().add("app-mod-meta");
        meta.setWrapText(false);
        meta.setTextOverrun(OverrunStyle.ELLIPSIS);
        meta.setMinWidth(0);
        meta.setMaxWidth(Double.MAX_VALUE);

        VBox text = new VBox(heading, summary, meta);
        text.getStyleClass().add("app-mod-text");
        text.setFillWidth(true);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        return ModViewSupport.resultRow(plan, icon, text, "Fabric API", () -> {
        });
    }
}