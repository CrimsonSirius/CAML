package org.chs.camlauncher.front;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowSizePlannerTest {

    @Test
    void fillsMostOfSmallScreenWithoutOverflowing() {
        // 1366x768 笔记本：可用高度只剩约 728。
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(0, 0, 1366, 728);
        WindowSizePlanner.WindowGeometry window = metrics.window();

        assertTrue(window.width() <= 1366, "宽度不能超过屏幕");
        assertTrue(window.height() <= 728, "高度不能超过屏幕");
        assertTrue(window.width() >= metrics.minWidth(), "预设宽度不能小于最小宽度");
        assertTrue(window.height() >= metrics.minHeight(), "预设高度不能小于最小高度");
        assertTrue(metrics.minWidth() <= 1366, "最小宽度必须放得进屏幕");
        assertTrue(metrics.minHeight() <= 728, "最小高度必须放得进屏幕");
        assertTrue(metrics.minHeight() <= WindowSizePlanner.DESIGN_MIN_HEIGHT,
                "屏幕比设计基准更矮时，最小高度应随屏幕收缩");
        // 小屏窗口必须落在缩放因子覆盖的区间内，界面才能按比例完整显示。
        assertTrue(UiScale.factor(window.width(), window.height()) >= UiScale.MIN_FACTOR,
                "小屏窗口的缩放因子不应低于下限");
    }

    @Test
    void keepsComfortableSizeOnFullHdScreen() {
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(0, 0, 1920, 1040);
        WindowSizePlanner.WindowGeometry window = metrics.window();

        assertTrue(window.width() >= 1100 && window.width() <= 1440, "初始宽度应在舒适区间内");
        assertTrue(window.height() >= 720 && window.height() <= 960, "初始高度应在舒适区间内");
        // 全高清屏幕上窗口能容纳设计基准，因此最小尺寸就是设计基准缩小到因子下限的结果。
        assertEquals(Math.floor(UiScale.DESIGN_WIDTH * UiScale.MIN_FACTOR), metrics.minWidth());
        assertEquals(Math.floor(UiScale.DESIGN_HEIGHT * UiScale.MIN_FACTOR), metrics.minHeight());
        assertEquals(1, UiScale.factor(window.width(), window.height()), 0.2,
                "全高清屏幕上的初始窗口应接近设计基准的缩放因子 1");
    }

    @Test
    void capsWindowOnVeryLargeScreen() {
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(0, 0, 3840, 2120);
        WindowSizePlanner.WindowGeometry window = metrics.window();

        assertEquals(WindowSizePlanner.MAX_WIDTH, window.width());
        assertEquals(WindowSizePlanner.MAX_HEIGHT, window.height());
        assertEquals(UiScale.MAX_FACTOR, UiScale.factor(window.width(), window.height()),
                "上限窗口应对应最大缩放因子");
    }

    @Test
    void centersWindowInsideScreenArea() {
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(100, 50, 1600, 1000);
        WindowSizePlanner.WindowGeometry window = metrics.window();

        assertEquals(100 + Math.floor((1600 - window.width()) / 2), window.x());
        assertEquals(50 + Math.floor((1000 - window.height()) / 2), window.y());
    }

    @Test
    void rejectsNonPositiveScreenSize() {
        assertThrows(IllegalArgumentException.class, () -> WindowSizePlanner.compute(0, 0, 0, 800));
        assertThrows(IllegalArgumentException.class, () -> WindowSizePlanner.compute(0, 0, 800, -1));
    }
}