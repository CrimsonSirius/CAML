package org.chs.camlauncher.front.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 design/ui.md 与 design/architecture.md 规定的 900px 断点行为。
 */
class WorkspaceLayoutTest {

    @Test
    void stacksVerticallyBelowBreakpoint() {
        assertTrue(WorkspaceLayout.isCompact(899));
        assertTrue(WorkspaceLayout.plan(899).compact());
        assertTrue(WorkspaceLayout.isCompact(720));
    }

    @Test
    void splitsSideBySideFromBreakpointOnwards() {
        assertFalse(WorkspaceLayout.isCompact(900));
        assertFalse(WorkspaceLayout.plan(900).compact());
        assertFalse(WorkspaceLayout.isCompact(1600));
    }

    @Test
    void relaxesMinHeightsWhenStacked() {
        WorkspaceLayout.Plan wide = WorkspaceLayout.plan(1200);
        WorkspaceLayout.Plan compact = WorkspaceLayout.plan(880);

        assertTrue(compact.formMinHeight() < wide.formMinHeight(), "堆叠时表单最小高度应放宽");
        assertTrue(compact.logMinHeight() < wide.logMinHeight(), "堆叠时日志面板最小高度应放宽");
        assertTrue(compact.logAreaMinHeight() < wide.logAreaMinHeight(), "堆叠时日志文本区最小高度应放宽");
        assertTrue(compact.logAreaPrefHeight() < wide.logAreaPrefHeight(), "堆叠时日志首选高度应放宽");
    }

    @Test
    void onlyLayoutChangesAcrossBreakpoint() {
        // 同一侧宽度的方案完全一致，MainView 据此避免缩放时反复改写布局和分隔条。
        assertEquals(WorkspaceLayout.plan(901), WorkspaceLayout.plan(3000));
        assertEquals(WorkspaceLayout.plan(400), WorkspaceLayout.plan(899));
    }

    @Test
    void usesDistinctDividerPositions() {
        assertEquals(0.56, WorkspaceLayout.plan(1200).dividerPosition());
        assertEquals(0.62, WorkspaceLayout.plan(800).dividerPosition());
    }

    @Test
    void rejectsNonPositiveWidth() {
        assertThrows(IllegalArgumentException.class, () -> WorkspaceLayout.plan(0));
        assertThrows(IllegalArgumentException.class, () -> WorkspaceLayout.plan(-1));
    }
}
