package org.chs.camlauncher.front.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 design/ui.md 约定的模组下载页比例、表单方向与尺寸稳定性。
 */
class ModDownloadLayoutTest {

    @Test
    void usesInlineFormOnlyOnWidePages() {
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(660));
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(690));
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(1020));
        assertEquals(ModDownloadLayout.FormMode.GRID, ModDownloadLayout.formMode(659));
    }

    @Test
    void switchesToGridBeforeStacking() {
        assertEquals(ModDownloadLayout.FormMode.GRID, ModDownloadLayout.formMode(520));
        assertEquals(ModDownloadLayout.FormMode.GRID, ModDownloadLayout.formMode(600));
        assertEquals(ModDownloadLayout.FormMode.STACKED, ModDownloadLayout.formMode(519));
        assertEquals(ModDownloadLayout.FormMode.STACKED, ModDownloadLayout.formMode(360));
    }

    @Test
    void firstFramePlanMatchesWidePages() {
        // 首帧宽度未知时套用 WIDE；它与宽屏计算结果必须一致，否则表单会被重建两次。
        assertEquals(ModDownloadLayout.plan(ModDownloadLayout.DESIGN_WIDTH), ModDownloadLayout.WIDE);
    }

    @Test
    void keepsInlineFormOnScaled1080pScreens() {
        // 1080p 屏幕在 150% 缩放下逻辑宽度约 870，扣除侧边栏 180 后内容宽度约 690。
        // 这里必须仍是两行两列的 INLINE：单列堆叠会把搜索卡变高，进而挤掉结果列表。
        double windowWidth = 870;
        double contentWidth = windowWidth - ModDownloadLayout.sidebarWidth(windowWidth);
        assertEquals(690, contentWidth, 0.0001);
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(contentWidth));
    }

    @Test
    void onlyFormDirectionChangesAcrossBreakpoints() {
        // 缩放时控件尺寸、行高和字号保持稳定，避免结果列表在断点附近跳动。
        ModDownloadLayout.Plan wide = ModDownloadLayout.plan(1020);
        ModDownloadLayout.Plan grid = ModDownloadLayout.plan(600);
        ModDownloadLayout.Plan stacked = ModDownloadLayout.plan(420);

        for (ModDownloadLayout.Plan plan : new ModDownloadLayout.Plan[]{grid, stacked}) {
            assertEquals(wide.contentPaddingHorizontal(), plan.contentPaddingHorizontal());
            assertEquals(wide.contentPaddingVertical(), plan.contentPaddingVertical());
            assertEquals(wide.cardPadding(), plan.cardPadding());
            assertEquals(wide.cardSpacing(), plan.cardSpacing());
            assertEquals(wide.sectionSpacing(), plan.sectionSpacing());
            assertEquals(wide.formGap(), plan.formGap());
            assertEquals(wide.fieldLabelWidth(), plan.fieldLabelWidth());
            assertEquals(wide.iconSize(), plan.iconSize());
            assertEquals(wide.rowMinHeight(), plan.rowMinHeight());
            assertEquals(wide.rowPaddingVertical(), plan.rowPaddingVertical());
            assertEquals(wide.rowPaddingLeft(), plan.rowPaddingLeft());
            assertEquals(wide.rowPaddingRight(), plan.rowPaddingRight());
            assertEquals(wide.accentWidth(), plan.accentWidth());
            assertEquals(wide.listSpacing(), plan.listSpacing());
            assertEquals(wide.paginationButtonWidth(), plan.paginationButtonWidth());
        }
        assertNotEquals(wide.mode(), grid.mode());
        assertNotEquals(grid.mode(), stacked.mode());
    }

    @Test
    void allocatesFieldsInTwoToOneRatio() {
        ModDownloadLayout.Plan plan = ModDownloadLayout.plan(1020);
        assertEquals(2, plan.primaryFieldWeight() / plan.secondaryFieldWeight(), 0.0001);
    }

    @Test
    void keepsRowTallEnoughForIconAtAnyWidth() {
        for (double width : new double[]{360, 690, 1020, 1920}) {
            ModDownloadLayout.Plan plan = ModDownloadLayout.plan(width);
            assertEquals(88, plan.iconSize(), 0.0001);
            assertEquals(112, plan.rowMinHeight(), 0.0001);
            assertTrue(plan.rowMinHeight() >= plan.iconSize(), "行高需容纳图标");
        }
    }

    @Test
    void rowPaddingPlusIconMatchesMinHeight() {
        // 结果行正文高度必须正好等于行高，否则图标和文字会溢出到相邻行。
        for (double width : new double[]{360, 690, 1020, 1920}) {
            ModDownloadLayout.Plan plan = ModDownloadLayout.plan(width);
            assertEquals(plan.rowMinHeight(),
                    plan.rowPaddingVertical() * 2 + plan.iconSize(), 0.0001);
        }
    }

    @Test
    void scalesSidebarWithWindowWidth() {
        assertEquals(180, ModDownloadLayout.sidebarWidth(720), 0.0001);
        assertEquals(1305.0 / 6, ModDownloadLayout.sidebarWidth(1305), 0.0001);
        assertEquals(260, ModDownloadLayout.sidebarWidth(1920), 0.0001);
    }

    @Test
    void rejectsNonPositiveWidth() {
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.plan(0));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.plan(-1));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.formMode(0));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.sidebarWidth(0));
    }
}

