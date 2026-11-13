package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.mod.model.ModDependencyType;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Mod 页面的展示辅助。 */
final class ModViewSupport {
    private ModViewSupport() {
    }

    static String sourceLabel(ModSource source) {
        return UiText.tr(source == ModSource.MODRINTH
                ? "mods.source.modrinth"
                : "mods.source.curseforge");
    }

    static String loaderLabel(ModLoader loader) {
        return UiText.tr("mods.loader." + loader.apiName());
    }

    static String loaderLabel(String loader) {
        ModLoader value = ModLoader.fromApiName(loader);
        return value == null ? loader : loaderLabel(value);
    }

    static String joinLoaders(List<String> loaders) {
        return loaders.isEmpty()
                ? UiText.tr("mods.value.unknown")
                : loaders.stream().map(ModViewSupport::loaderLabel).collect(Collectors.joining(" / "));
    }

    static String joinVersions(List<String> versions) {
        return versions.isEmpty()
                ? UiText.tr("mods.value.unknown")
                : String.join(" / ", versions);
    }

    static String downloads(long count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    static String dependencyType(ModDependencyType type) {
        return UiText.tr("mods.dependency." + type.name().toLowerCase(Locale.ROOT));
    }

    /**
     * 宽屏下的“标签在左、控件在右”字段。
     *
     * <p>标签列固定宽度，两行表单因此保持左边缘对齐，控件列占用剩余空间。</p>
     */
    static HBox inlineField(ModDownloadLayout.Plan plan, String labelKey, Region control) {
        Label label = ViewSupport.fieldLabel(UiText.tr(labelKey), control);
        label.getStyleClass().add("app-mod-field-label");
        label.setMinWidth(plan.fieldLabelWidth());
        label.setPrefWidth(plan.fieldLabelWidth());
        label.setMaxWidth(plan.fieldLabelWidth());
        label.setAlignment(Pos.CENTER_LEFT);
        control.setMaxWidth(Double.MAX_VALUE);

        HBox field = new HBox(label, control);
        field.getStyleClass().add("app-mod-field");
        field.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(control, Priority.ALWAYS);
        return field;
    }

    /** 窄屏下的“标签在上、控件在下”字段。 */
    static VBox stackedField(String labelKey, Region control) {
        VBox field = ViewSupport.buildField(UiText.tr(labelKey), control, null);
        field.getStyleClass().add("app-mod-field-stacked");
        control.setMinWidth(0);
        control.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    /**
     * 生成等宽列网格。
     *
     * <p>用于“标签在上”的表单方向：百分比列宽配合 {@code minWidth=0} 的控件，
     * 让两列在窄窗口下等分可用宽度而不溢出。</p>
     */
    static GridPane evenGrid(ModDownloadLayout.Plan plan, int columns) {
        GridPane grid = new GridPane();
        grid.setHgap(plan.formGap());
        grid.setVgap(plan.formGap());
        for (int index = 0; index < columns; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / columns);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    /**
     * 结果行外壳：下层透明按钮承担点击、悬停和焦点，上层只读正文负责显示。
     *
     * <p>{@code CamlButton} 的样式带有 {@code -fx-min-height: 44}，CSS 会覆盖代码设置的
     * 最小高度；若把正文放进按钮的 graphic 并同时使用 {@code Region.USE_PREF_SIZE}，
     * 皮肤算出的首选高度会塌陷到 0，正文就溢出到相邻行。这里把尺寸交给普通容器，
     * 行高由图标与内边距自然决定，按钮只负责交互。</p>
     */
    static StackPane resultRow(ModDownloadLayout.Plan plan, Region icon, Node text,
                               String accessibleText, Runnable onActivate) {
        HBox content = new HBox(icon, text);
        content.getStyleClass().add("app-mod-row-content");
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMinWidth(0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.setPadding(new Insets(plan.rowPaddingVertical(), plan.rowPaddingRight(),
                plan.rowPaddingVertical(), plan.rowPaddingLeft()));
        content.setMouseTransparent(true);

        Region accent = new Region();
        accent.getStyleClass().add("app-mod-accent");
        accent.setMinWidth(plan.accentWidth());
        accent.setPrefWidth(plan.accentWidth());
        accent.setMaxWidth(plan.accentWidth());
        accent.setMaxHeight(Double.MAX_VALUE);
        accent.setMouseTransparent(true);

        CamlButton hit = CamlButton.outlined("");
        hit.getStyleClass().addAll("app-instance-item", "app-mod-item");
        hit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        hit.setAccessibleText(accessibleText);
        hit.setOnAction(event -> onActivate.run());

        StackPane row = new StackPane(hit, accent, content);
        row.getStyleClass().add("app-mod-row");
        row.setMinHeight(plan.rowMinHeight());
        row.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(accent, Pos.CENTER_LEFT);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
        StackPane.setMargin(accent, new Insets(1, 0, 1, 1));
        return row;
    }

    /**
     * 按 {@code primary:secondary} 权重排列一组字段。
     *
     * <p>图示中名称/游戏版本输入框明显宽于来源/加载器下拉框，比例为 {@code 2:1}；
     * 两行复用同一组列约束，保证上下字段对齐。</p>
     */
    static GridPane weightedRow(ModDownloadLayout.Plan plan, Region primary, Region secondary) {
        GridPane row = new GridPane();
        row.setHgap(plan.formGap());
        double total = plan.primaryFieldWeight() + plan.secondaryFieldWeight();

        ColumnConstraints primaryColumn = new ColumnConstraints();
        primaryColumn.setPercentWidth(100 * plan.primaryFieldWeight() / total);
        ColumnConstraints secondaryColumn = new ColumnConstraints();
        secondaryColumn.setPercentWidth(100 * plan.secondaryFieldWeight() / total);
        row.getColumnConstraints().addAll(primaryColumn, secondaryColumn);

        row.add(primary, 0, 0);
        row.add(secondary, 1, 0);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }
}