package org.chs.camlauncher.front;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiScaleTest {

    @Test
    void designWindowUsesBaseFactor() {
        double factor = UiScale.factor(UiScale.DESIGN_WIDTH, UiScale.DESIGN_HEIGHT);

        assertEquals(1, factor);
        assertEquals(UiScale.BASE_FONT_PX, UiScale.baseFontPx(factor));
    }

    @Test
    void smallerWindowShrinksEverything() {
        // 150% 缩放的 1080p 屏幕只有约 1280x720 逻辑像素，界面需要整体缩小。
        double factor = UiScale.factor(1280, 720);

        assertTrue(factor < 1, "小于设计基准的窗口应缩小");
        assertEquals(0.8125, factor, "1280/1440 = 0.888…，720/900 = 0.8，取小值后量化到 1/16");
        assertEquals(13, UiScale.baseFontPx(factor), "根字号应为整数像素");
    }

    @Test
    void largerWindowEnlargesEverything() {
        double factor = UiScale.factor(2200, 1400);

        assertTrue(factor > 1, "大于设计基准的窗口应放大");
        assertEquals(1.375, factor, "2200/1440 = 1.527… 超过上限，应钳制到最大因子");
    }

    @Test
    void factorIsClampedAtBothEnds() {
        assertEquals(UiScale.MIN_FACTOR, UiScale.factor(320, 200));
        assertEquals(UiScale.MIN_FACTOR, UiScale.factor(4000, 300));
        assertEquals(UiScale.MAX_FACTOR, UiScale.factor(4000, 2600));
        assertEquals(UiScale.MAX_FACTOR, UiScale.factor(4000, 4000));
    }

    @Test
    void factorUsesSmallerDirectionToKeepProportions() {
        // 宽而矮的窗口按高度缩小，窄而高的窗口按宽度缩小，两者都不会被拉伸。
        assertEquals(UiScale.factor(1440, 900), UiScale.factor(2880, 900));
        assertEquals(UiScale.factor(1440, 900), UiScale.factor(1440, 1800));
    }

    @Test
    void factorQuantizesToIntegerFontPixels() {
        for (double width = 700; width <= 2600; width += 37) {
            double factor = UiScale.factor(width, width * UiScale.DESIGN_HEIGHT / UiScale.DESIGN_WIDTH);
            double fontPx = UiScale.baseFontPx(factor);

            assertEquals(fontPx, Math.rint(fontPx), 1e-9, "根字号必须是整数像素");
            assertTrue(fontPx >= 11 && fontPx <= 22, "根字号应落在 11–22px 之间");
        }
    }

    @Test
    void sizeScalesDesignPixelsWithSameFactor() {
        assertEquals(0, UiScale.size(0, 1.5), 1e-9);
        assertEquals(112, UiScale.size(112, 1), 1e-9);
        assertEquals(154, UiScale.size(112, UiScale.MAX_FACTOR), 1e-9);
        assertEquals(77, UiScale.size(112, UiScale.MIN_FACTOR), 1e-9);
    }

    @Test
    void maxWindowMatchesDesignWindowAtMaxFactor() {
        assertEquals(UiScale.DESIGN_WIDTH * UiScale.MAX_FACTOR, UiScale.maxWindowWidth(), 1e-9);
        assertEquals(UiScale.DESIGN_HEIGHT * UiScale.MAX_FACTOR, UiScale.maxWindowHeight(), 1e-9);
        assertEquals(UiScale.MAX_FACTOR,
                UiScale.factor(UiScale.maxWindowWidth(), UiScale.maxWindowHeight()));
    }

    @Test
    void minWindowMatchesDesignWindowAtMinFactor() {
        double width = UiScale.size(UiScale.DESIGN_WIDTH, UiScale.MIN_FACTOR);
        double height = UiScale.size(UiScale.DESIGN_HEIGHT, UiScale.MIN_FACTOR);

        assertEquals(UiScale.MIN_FACTOR, UiScale.factor(width, height));
    }

    @Test
    void rejectsNonPositiveWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> UiScale.factor(0, 900));
        assertThrows(IllegalArgumentException.class, () -> UiScale.factor(1440, 0));
        assertThrows(IllegalArgumentException.class, () -> UiScale.factor(-1, 900));
    }
}
