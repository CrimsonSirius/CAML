package org.chs.camlauncher.front.view;

import org.chs.camlauncher.front.UiScale;

/**
 * 主工作区的自适应规则。
 *
 * <p>断点来自 {@code design/ui.md} 与 {@code design/architecture.md}：窗口宽度
 * {@code >= 900}（乘以 UI 缩放因子）时左右分栏，日志作为持续可见的支持面板；
 * 更窄时上下堆叠，表单在上、日志在下，并放宽表单和日志的最小高度。</p>
 *
 * <p>高度同样乘以 {@link UiScale} 的缩放因子：字号放大后日志和表单需要更多空间，
 * 缩小后可以少留空间。缩放因子同时作用于断点，因此“字号变大导致分栏放不下”时
 * 会先切到上下堆叠，而不是让两栏互相挤压。该类只做纯计算，不依赖 JavaFX，便于单元测试。</p>
 */
final class WorkspaceLayout {

    /** 布局断点：与设计文档一致的 900px（设计基准像素）。 */
    static final double COMPACT_BREAKPOINT = 900;

    /** 宽屏分栏时表单区的最小高度，保证字段区可滚动而主操作可见。 */
    private static final double WIDE_FORM_MIN_HEIGHT = 320;
    /** 上下堆叠时放宽表单区最小高度，避免小屏上日志被挤出可见区域。 */
    private static final double COMPACT_FORM_MIN_HEIGHT = 240;
    /** 宽屏分栏时日志面板的最小高度。 */
    private static final double WIDE_LOG_MIN_HEIGHT = 220;
    /** 上下堆叠时放宽日志面板的最小高度。 */
    private static final double COMPACT_LOG_MIN_HEIGHT = 140;
    /** 日志文本区自身的最小高度，同样在堆叠时放宽。 */
    private static final double WIDE_LOG_AREA_MIN_HEIGHT = 200;
    private static final double COMPACT_LOG_AREA_MIN_HEIGHT = 120;
    /** 日志文本区的首选高度：宽屏留给日志更多空间。 */
    private static final double WIDE_LOG_AREA_PREF_HEIGHT = 460;
    private static final double COMPACT_LOG_AREA_PREF_HEIGHT = 260;
    /** 分隔条位置：宽屏表单略宽于日志，堆叠时表单约占六成高度。 */
    private static final double WIDE_DIVIDER_POSITION = 0.56;
    private static final double COMPACT_DIVIDER_POSITION = 0.62;

    /** 首帧宽度和缩放尚未确定时使用的宽屏规则。 */
    static final Plan WIDE = plan(COMPACT_BREAKPOINT, UiScale.DEFAULT_FACTOR);

    private WorkspaceLayout() {
    }

    /**
     * @param width  窗口内容宽度
     * @param factor UI 缩放因子
     * @return 该宽度是否使用上下堆叠布局
     * @throws IllegalArgumentException 宽度或因子不是正数
     */
    static boolean isCompact(double width, double factor) {
        if (width <= 0 || factor <= 0) {
            throw new IllegalArgumentException("窗口宽度和缩放因子必须为正数");
        }
        return width < UiScale.size(COMPACT_BREAKPOINT, factor);
    }

    /**
     * 计算某一窗口宽度与缩放因子下的工作区布局。
     *
     * @param width  窗口内容宽度
     * @param factor UI 缩放因子
     * @throws IllegalArgumentException 宽度或因子不是正数
     */
    static Plan plan(double width, double factor) {
        if (width <= 0 || factor <= 0) {
            throw new IllegalArgumentException("窗口宽度和缩放因子必须为正数");
        }
        boolean compact = isCompact(width, factor);
        return new Plan(
                compact,
                UiScale.size(compact ? COMPACT_FORM_MIN_HEIGHT : WIDE_FORM_MIN_HEIGHT, factor),
                UiScale.size(compact ? COMPACT_LOG_MIN_HEIGHT : WIDE_LOG_MIN_HEIGHT, factor),
                UiScale.size(compact ? COMPACT_LOG_AREA_MIN_HEIGHT : WIDE_LOG_AREA_MIN_HEIGHT, factor),
                UiScale.size(compact ? COMPACT_LOG_AREA_PREF_HEIGHT : WIDE_LOG_AREA_PREF_HEIGHT, factor),
                compact ? COMPACT_DIVIDER_POSITION : WIDE_DIVIDER_POSITION);
    }

    /**
     * 工作区布局参数。
     *
     * @param compact           是否上下堆叠
     * @param formMinHeight     表单面板最小高度
     * @param logMinHeight      日志面板最小高度
     * @param logAreaMinHeight  日志文本区最小高度
     * @param logAreaPrefHeight 日志文本区首选高度
     * @param dividerPosition   分隔条位置
     */
    record Plan(
            boolean compact,
            double formMinHeight,
            double logMinHeight,
            double logAreaMinHeight,
            double logAreaPrefHeight,
            double dividerPosition
    ) {
    }
}