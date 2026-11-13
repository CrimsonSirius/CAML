package org.chs.camlauncher.front.view;

import org.chs.camlauncher.front.UiScale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 design/ui.md 约定的模组下载页比例、表单方向与尺寸稳定性，
 * 以及全部尺寸随 {@link UiScale} 缩放因子等比变化的规则。
 */
class ModDownloadLayoutTest {
    private static final double BASE = UiScale.DEFAULT_FACTOR;

    @Test
    void usesInlineFormOnlyOnWidePages() {
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(660, BASE));
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(690, BASE));
        assertEquals(ModDownloadLayout.FormMode.INLINE, ModDownloadLayout.formMode(1020, BASE));
        assertEquals(ModDownloadLayout.FormMode.GRID, ModDownloadLayout.formMode(659, BASE));
    }

    @Test
    void switchesToGridBeforeStacking() {
        assertEquals(ModDownloadLayout.FormMode.GRID, ModDownloadLayout.formMode(520, BASE));
        assertEquals(ModDownloadLayout.FormMode.GRID, ModDownloadLayout.formMode(600, BASE));
        assertEquals(ModDownloadLayout.FormMode.STACKED, ModDownloadLayout.formMode(519, BASE));
        assertEquals(ModDownloadLayout.FormMode.STACKED, ModDownloadLayout.formMode(360, BASE));
    }

    @Test
    void firstFramePlanMatchesWidePages() {
        // 首帧宽度未知时套用 WIDE；它与宽屏计算结果必须一致，否则表单会被重建两次。
        assertEquals(ModDownloadLayout.plan(UiScale.DESIGN_WIDTH, BASE), ModDownloadLayout.WIDE);
    }

    @Test
    void keepsInlineFormOnScaledLaptopScreens() {
        // 150% 缩放的 1080p 屏幕只有约 1280x720 逻辑像素：因子 0.8125，侧边栏同样按因子钳制。
        // 这里必须仍是两行两列的 INLINE：单列堆叠会把搜索卡变高，进而挤掉结果列表。
        double factor = UiScale.factor(1280, 720);
        double windowWidth = 1280;
        double sidebar = ModDownloadLayout.sidebarWidth(windowWidth, factor);
        double contentWidth = windowWidth - sidebar;

        assertEquals(UiScale.size(ModDownloadLayout.SIDEBAR_MAX_WIDTH, factor), sidebar, 0.0001);
        assertEquals(ModDownloadLayout.FormMode.INLINE,
                ModDownloadLayout.formMode(contentWidth, factor));
    }

    @Test
    void formBreakpointsScaleWithFactor() {
        // 字号放大后字段更宽，更早切到单列；缩小后能在更窄的宽度上保持两列。
        double scaledInline = UiScale.size(ModDownloadLayout.INLINE_FORM_MIN_WIDTH, UiScale.MAX_FACTOR);
        assertEquals(ModDownloadLayout.FormMode.GRID,
                ModDownloadLayout.formMode(scaledInline - 1, UiScale.MAX_FACTOR));
        assertEquals(ModDownloadLayout.FormMode.INLINE,
                ModDownloadLayout.formMode(scaledInline, UiScale.MAX_FACTOR));

        double smallStacked = UiScale.size(ModDownloadLayout.STACKED_FORM_MIN_WIDTH, UiScale.MIN_FACTOR);
        assertEquals(ModDownloadLayout.FormMode.STACKED,
                ModDownloadLayout.formMode(smallStacked - 1, UiScale.MIN_FACTOR));
        assertEquals(ModDownloadLayout.FormMode.GRID,
                ModDownloadLayout.formMode(smallStacked, UiScale.MIN_FACTOR));
    }

    @Test
    void onlyFormDirectionChangesAcrossBreakpoints() {
        // 同一缩放因子下切换方向只改变摆放方式，控件尺寸、行高和字号保持稳定。
        ModDownloadLayout.Plan wide = ModDownloadLayout.plan(1020, BASE);
        ModDownloadLayout.Plan grid = ModDownloadLayout.plan(600, BASE);
        ModDownloadLayout.Plan stacked = ModDownloadLayout.plan(420, BASE);

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
        ModDownloadLayout.Plan plan = ModDownloadLayout.plan(1020, BASE);
        assertEquals(2, plan.primaryFieldWeight() / plan.secondaryFieldWeight(), 0.0001);
    }

    @Test
    void keepsRowTallEnoughForIconAtAnyWidth() {
        for (double width : new double[]{360, 690, 1020, 1920}) {
            ModDownloadLayout.Plan plan = ModDownloadLayout.plan(width, BASE);
            assertEquals(ModDownloadLayout.ICON_SIZE, plan.iconSize(), 0.0001);
            assertEquals(ModDownloadLayout.ROW_MIN_HEIGHT, plan.rowMinHeight(), 0.0001);
            assertTrue(plan.rowMinHeight() >= plan.iconSize(), "行高需容纳图标");
        }
    }

    @Test
    void rowPaddingPlusIconMatchesMinHeight() {
        // 结果行正文高度必须正好等于行高，否则图标和文字会溢出到相邻行。
        for (double width : new double[]{360, 690, 1020, 1920}) {
            for (double factor : new double[]{UiScale.MIN_FACTOR, 1, UiScale.MAX_FACTOR}) {
                ModDownloadLayout.Plan plan = ModDownloadLayout.plan(width, factor);
                assertEquals(plan.rowMinHeight(),
                        plan.rowPaddingVertical() * 2 + plan.iconSize(), 0.0001);
            }
        }
    }

    @Test
    void allSizesScaleWithFactor() {
        // 缩放因子同时作用于字号（CSS em）与代码几何，两者必须成同一比例。
        ModDownloadLayout.Plan base = ModDownloadLayout.plan(1020, BASE);
        ModDownloadLayout.Plan scaled = ModDownloadLayout.plan(1020, UiScale.MAX_FACTOR);

        assertEquals(base.contentPaddingHorizontal() * UiScale.MAX_FACTOR,
                scaled.contentPaddingHorizontal(), 1e-9);
        assertEquals(base.cardPadding() * UiScale.MAX_FACTOR, scaled.cardPadding(), 1e-9);
        assertEquals(base.formGap() * UiScale.MAX_FACTOR, scaled.formGap(), 1e-9);
        assertEquals(base.fieldLabelWidth() * UiScale.MAX_FACTOR, scaled.fieldLabelWidth(), 1e-9);
        assertEquals(base.iconSize() * UiScale.MAX_FACTOR, scaled.iconSize(), 1e-9);
        assertEquals(base.rowMinHeight() * UiScale.MAX_FACTOR, scaled.rowMinHeight(), 1e-9);
        assertEquals(base.accentWidth() * UiScale.MAX_FACTOR, scaled.accentWidth(), 1e-9);
        assertEquals(base.paginationButtonWidth() * UiScale.MAX_FACTOR,
                scaled.paginationButtonWidth(), 1e-9);
    }

    @Test
    void scalesSidebarWithWindowWidth() {
        assertEquals(180, ModDownloadLayout.sidebarWidth(720, BASE), 0.0001);
        assertEquals(1305.0 / 6, ModDownloadLayout.sidebarWidth(1305, BASE), 0.0001);
        assertEquals(260, ModDownloadLayout.sidebarWidth(1920, BASE), 0.0001);
        // 上下限随因子移动：小屏上侧边栏不会显得过宽，大屏上不会显得过窄。
        assertEquals(UiScale.size(ModDownloadLayout.SIDEBAR_MIN_WIDTH, UiScale.MIN_FACTOR),
                ModDownloadLayout.sidebarWidth(720, UiScale.MIN_FACTOR), 0.0001);
        assertEquals(UiScale.size(ModDownloadLayout.SIDEBAR_MAX_WIDTH, UiScale.MAX_FACTOR),
                ModDownloadLayout.sidebarWidth(5000, UiScale.MAX_FACTOR), 0.0001);
    }

    @Test
    void rejectsNonPositiveInput() {
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.plan(0, BASE));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.plan(-1, BASE));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.plan(600, 0));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.formMode(0, BASE));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.formMode(600, -1));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.sidebarWidth(0, BASE));
        assertThrows(IllegalArgumentException.class, () -> ModDownloadLayout.sidebarWidth(720, 0));
    }
}