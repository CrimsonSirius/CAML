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
        assertTrue(metrics.minHeight() <= 728, "最小高度必须放得进屏幕");
        assertTrue(metrics.minHeight() < WindowSizePlanner.DESIGN_MIN_HEIGHT, "小屏上最小高度应随屏幕收缩");
    }

    @Test
    void keepsComfortableSizeOnFullHdScreen() {
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(0, 0, 1920, 1040);
        WindowSizePlanner.WindowGeometry window = metrics.window();

        assertTrue(window.width() >= 1100 && window.width() <= 1440, "初始宽度应在舒适区间内");
        assertTrue(window.height() >= 720 && window.height() <= 960, "初始高度应在舒适区间内");
        assertEquals(720, metrics.minWidth());
        assertEquals(700, metrics.minHeight());
    }

    @Test
    void capsWindowOnVeryLargeScreen() {
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(0, 0, 3840, 2120);
        WindowSizePlanner.WindowGeometry window = metrics.window();

        assertEquals(WindowSizePlanner.MAX_WIDTH, window.width());
        assertEquals(WindowSizePlanner.MAX_HEIGHT, window.height());
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
