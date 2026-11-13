package org.chs.camlauncher.front.view;

import org.chs.camlauncher.front.UiScale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 design/ui.md 与 design/architecture.md 约定的 900px 断点行为，
 * 以及断点、最小高度随 {@link UiScale} 缩放因子等比移动的规则。
 */
class WorkspaceLayoutTest {
    private static final double BASE = UiScale.DEFAULT_FACTOR;

    @Test
    void stacksVerticallyBelowBreakpoint() {
        assertTrue(WorkspaceLayout.isCompact(899, BASE));
        assertTrue(WorkspaceLayout.plan(899, BASE).compact());
        assertTrue(WorkspaceLayout.isCompact(720, BASE));
    }

    @Test
    void splitsSideBySideFromBreakpointOnwards() {
        assertFalse(WorkspaceLayout.isCompact(900, BASE));
        assertFalse(WorkspaceLayout.plan(900, BASE).compact());
        assertFalse(WorkspaceLayout.isCompact(1600, BASE));
    }

    @Test
    void breakpointMovesWithUiScale() {
        // 界面放大后字段更宽，需要更宽的窗口才放得下左右分栏；缩小后则更早分栏。
        double scaledBreakpoint = UiScale.size(WorkspaceLayout.COMPACT_BREAKPOINT, UiScale.MAX_FACTOR);
        assertTrue(WorkspaceLayout.isCompact(scaledBreakpoint - 1, UiScale.MAX_FACTOR));
        assertFalse(WorkspaceLayout.isCompact(scaledBreakpoint, UiScale.MAX_FACTOR));

        double smallBreakpoint = UiScale.size(WorkspaceLayout.COMPACT_BREAKPOINT, UiScale.MIN_FACTOR);
        assertTrue(WorkspaceLayout.isCompact(smallBreakpoint - 1, UiScale.MIN_FACTOR));
        assertFalse(WorkspaceLayout.isCompact(smallBreakpoint, UiScale.MIN_FACTOR));
    }

    @Test
    void relaxesMinHeightsWhenStacked() {
        WorkspaceLayout.Plan wide = WorkspaceLayout.plan(1200, BASE);
        WorkspaceLayout.Plan compact = WorkspaceLayout.plan(880, BASE);

        assertTrue(compact.formMinHeight() < wide.formMinHeight(), "堆叠时表单最小高度应放宽");
        assertTrue(compact.logMinHeight() < wide.logMinHeight(), "堆叠时日志面板最小高度应放宽");
        assertTrue(compact.logAreaMinHeight() < wide.logAreaMinHeight(), "堆叠时日志文本区最小高度应放宽");
        assertTrue(compact.logAreaPrefHeight() < wide.logAreaPrefHeight(), "堆叠时日志首选高度应放宽");
    }

    @Test
    void heightsScaleWithFactor() {
        // 同一侧宽度下，几何只由缩放因子决定：字号放大多少，面板高度就放大多少。
        WorkspaceLayout.Plan base = WorkspaceLayout.plan(1400, BASE);
        WorkspaceLayout.Plan scaled = WorkspaceLayout.plan(1400, UiScale.MAX_FACTOR);

        assertEquals(base.formMinHeight() * UiScale.MAX_FACTOR, scaled.formMinHeight(), 1e-9);
        assertEquals(base.logMinHeight() * UiScale.MAX_FACTOR, scaled.logMinHeight(), 1e-9);
        assertEquals(base.logAreaMinHeight() * UiScale.MAX_FACTOR, scaled.logAreaMinHeight(), 1e-9);
        assertEquals(base.logAreaPrefHeight() * UiScale.MAX_FACTOR, scaled.logAreaPrefHeight(), 1e-9);
    }

    @Test
    void onlyLayoutChangesAcrossBreakpoint() {
        // 同一侧宽度的方案完全一致，HomeView 据此避免缩放时反复改写布局和分隔条。
        assertEquals(WorkspaceLayout.plan(901, BASE), WorkspaceLayout.plan(3000, BASE));
        assertEquals(WorkspaceLayout.plan(400, BASE), WorkspaceLayout.plan(899, BASE));
        assertNotEquals(WorkspaceLayout.plan(899, BASE), WorkspaceLayout.plan(900, BASE));
    }

    @Test
    void usesDistinctDividerPositions() {
        assertEquals(0.56, WorkspaceLayout.plan(1200, BASE).dividerPosition());
        assertEquals(0.62, WorkspaceLayout.plan(800, BASE).dividerPosition());
    }

    @Test
    void rejectsNonPositiveInput() {
        assertThrows(IllegalArgumentException.class, () -> WorkspaceLayout.plan(0, BASE));
        assertThrows(IllegalArgumentException.class, () -> WorkspaceLayout.plan(-1, BASE));
        assertThrows(IllegalArgumentException.class, () -> WorkspaceLayout.plan(1200, 0));
        assertThrows(IllegalArgumentException.class, () -> WorkspaceLayout.isCompact(1200, -1));
    }
}