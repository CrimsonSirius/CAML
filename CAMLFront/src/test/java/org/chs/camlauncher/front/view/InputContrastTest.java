package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 输入控件边界与相邻背景的对比度回归测试。 */
class InputContrastTest {
    private static final double MIN_NON_TEXT_CONTRAST = 3.0;

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void textFieldBorderMeetsNonTextContrast() throws Exception {
        Ratios ratios = onFxThread(() -> {
            CamlTextField field = new CamlTextField();
            VBox root = new VBox(field);
            applyStyles(root);
            return ratios(root, field, field.getBorder().getStrokes().getFirst().getBottomStroke());
        });

        assertRatios(ratios, "文本框");
    }

    @Test
    void comboBoxBorderMeetsNonTextContrast() throws Exception {
        Ratios ratios = onFxThread(() -> {
            CamlComboBox<String> combo = new CamlComboBox<>();
            VBox root = new VBox(combo);
            applyStyles(root);
            return ratios(root, combo, combo.getBorder().getStrokes().getFirst().getTopStroke());
        });

        assertRatios(ratios, "下拉框");
    }

    private static Ratios ratios(Region root, Region control, Paint borderPaint) {
        assertNotNull(borderPaint, "输入控件必须定义可见边框");
        Color border = (Color) borderPaint;
        Color page = (Color) root.getBackground().getFills().getFirst().getFill();
        Color fill = (Color) control.getBackground().getFills().getFirst().getFill();
        return new Ratios(contrast(border, page), contrast(border, fill));
    }

    private static void assertRatios(Ratios ratios, String name) {
        assertTrue(ratios.againstPage() >= MIN_NON_TEXT_CONTRAST,
                name + "边框对页面的对比度不足: " + ratios.againstPage());
        assertTrue(ratios.againstFill() >= MIN_NON_TEXT_CONTRAST,
                name + "边框对填充背景的对比度不足: " + ratios.againstFill());
    }

    private static void applyStyles(VBox root) {
        Scene scene = new Scene(root, 400, 300);
        CamlTheme.install(scene);
        addStylesheet(scene, "/themes/mango.css");
        addStylesheet(scene, "/styles/app.css");
        root.applyCss();
        root.resize(400, 300);
        root.applyCss();
        root.layout();
    }

    private static double contrast(Color first, Color second) {
        double firstLuminance = luminance(first);
        double secondLuminance = luminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed())
                + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
    }

    private static double linear(double channel) {
        return channel <= 0.04045
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private record Ratios(double againstPage, double againstFill) {
    }
}
