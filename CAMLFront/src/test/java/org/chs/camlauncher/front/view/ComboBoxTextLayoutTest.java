package org.chs.camlauncher.front.view;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.chs.camlauncher.front.UiScale;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 下拉框按钮单元格必须给 CJK 字形底部留出空间，不能把文字下半部分裁掉。 */
class ComboBoxTextLayoutTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void textKeepsRoomBelowAtScaledSizes() throws Exception {
        for (double factor : new double[]{
                UiScale.DEFAULT_FACTOR,
                UiScale.factor(1568, 1000),
                UiScale.MAX_FACTOR
        }) {
            Metrics metrics = onFxThread(() -> measure(factor));
            assertNotNull(metrics, "下拉框应渲染出可见文本");
            assertTrue(metrics.gapBelowText() >= 2.0 * factor,
                    "字体因子 " + factor + " 下文字底部余量不足: " + metrics);
        }
    }

    private static Metrics measure(double factor) {
        CamlComboBox<String> combo = new CamlComboBox<>();
        combo.getItems().add("全部版本");
        combo.setValue("全部版本");
        combo.setMinWidth(400);

        VBox root = new VBox(combo);
        root.setStyle("-fx-font-size: " + (int) UiScale.baseFontPx(factor) + "px;");
        Scene scene = new Scene(root, 800, 300);
        CamlTheme.install(scene);
        addStylesheet(scene, "/themes/mango.css");
        addStylesheet(scene, "/styles/app.css");
        root.applyCss();
        root.resize(800, 300);
        root.applyCss();
        root.layout();

        ListCell<?> cell = combo.lookupAll(".list-cell").stream()
                .filter(ListCell.class::isInstance)
                .map(ListCell.class::cast)
                .filter(candidate -> candidate.getItem() != null)
                .max(Comparator.comparingDouble(ListCell::getHeight))
                .orElse(null);
        if (cell == null) {
            return null;
        }
        Text text = cell.lookupAll(".text").stream()
                .filter(Text.class::isInstance)
                .map(Text.class::cast)
                .filter(candidate -> !candidate.getText().isEmpty())
                .findFirst()
                .orElse(null);
        if (text == null) {
            return null;
        }
        Bounds boundsInCell = cell.sceneToLocal(text.localToScene(text.getBoundsInLocal()));
        double contentBottom = cell.getHeight() - cell.getPadding().getBottom();
        return new Metrics(cell.getHeight(), boundsInCell.getMaxY(),
                contentBottom - boundsInCell.getMaxY());
    }

    private record Metrics(double cellHeight, double textBottom, double gapBelowText) {
    }
}
