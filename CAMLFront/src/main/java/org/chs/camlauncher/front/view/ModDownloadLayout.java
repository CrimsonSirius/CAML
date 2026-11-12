package org.chs.camlauncher.front.view;

/**
 * 模组下载页的布局规则。
 *
 * <p>尺寸来自 {@code design/ui.md} 的 1920×1080 设计基准：下载页左侧导航约占窗口宽度的
 * {@code 1/6}（限制在 {@code 180–260}），内容区左右留白 {@code 40}、上下 {@code 32}；
 * 搜索卡内边距 {@code 24}、内部间距 {@code 18}；结果行图标 {@code 88}、行高 {@code 112}，
 * 来源标签贴在行尾。</p>
 *
 * <p>遵循 {@code design/ui.md} 的既有约定——缩放窗口时只切换布局方向，不改变控件尺寸、
 * 间距和字号——搜索表单在三种方向之间切换：页面宽度 {@code >= 660} 使用“标签在左”的
 * 两行两列，{@code >= 520} 使用“标签在上”的两行两列，更窄时改为单列纵向堆叠。</p>
 *
 * <p>页面高度不足以同时容纳搜索卡、状态、结果和分页时，由整页纵向滚动接管；
 * 结果行因此不会被裁切，也不需要压缩行高和字号。该类只做纯计算，不依赖 JavaFX，
 * 便于单元测试。</p>
 */
final class ModDownloadLayout {

    /** 1080p 设计基准，所有尺寸都以该分辨率推导。 */
    static final double DESIGN_WIDTH = 1920;
    static final double DESIGN_HEIGHT = 1080;

    /** 搜索表单方向切换点，单位为页面内容宽度（已扣除左侧导航）。 */
    static final double INLINE_FORM_MIN_WIDTH = 660;
    static final double STACKED_FORM_MIN_WIDTH = 520;

    /** 下载页侧边栏占窗口宽度的比例，取自图示约 1/6。 */
    static final double SIDEBAR_RATIO = 1.0 / 6.0;
    static final double SIDEBAR_MIN_WIDTH = 180;
    static final double SIDEBAR_MAX_WIDTH = 260;

    /** 内容区留白。 */
    static final double CONTENT_PADDING_HORIZONTAL = 40;
    static final double CONTENT_PADDING_VERTICAL = 32;
    /**
     * 搜索卡与详情卡的内边距。
     *
     * <p>与 {@code app.css} 的 {@code .app-download-card} 保持一致：CSS 的
     * {@code -fx-padding} 会覆盖代码设置，因此这里必须是同一个值。</p>
     */
    static final double CARD_PADDING = 24;
    static final double CARD_SPACING = 18;
    /** 卡片、状态文字、列表和分页之间的间距。 */
    static final double SECTION_SPACING = 18;
    /** 搜索表单行列间距。 */
    static final double FORM_GAP = 16;
    /** 标签列宽度，保证两行字段左边缘对齐。 */
    static final double FIELD_LABEL_WIDTH = 64;
    /** 字段列与下拉列的宽度权重，对应图示约 2:1。 */
    static final double PRIMARY_FIELD_WEIGHT = 2;
    static final double SECONDARY_FIELD_WEIGHT = 1;
    /** 结果行：图标、行高和行内边距。 */
    static final double ICON_SIZE = 88;
    static final double ROW_MIN_HEIGHT = 112;
    /**
     * 结果行的内边距与强调条宽度。
     *
     * <p>垂直内边距 {@code 12} 与图标 {@code 88} 相加正好等于行高 {@code 112}，
     * 因此图标随行高收缩时不会把行撑高：{@code 12 + 88 + 12 = 112}。
     * 强调条贴住行框左边缘，正文左内边距在强调条之后。</p>
     */
    static final double ROW_PADDING_VERTICAL = 12;
    static final double ROW_PADDING_LEFT = 16;
    static final double ROW_PADDING_RIGHT = 20;
    static final double ACCENT_WIDTH = 4;
    static final double LIST_SPACING = 12;
    /** 分页按钮。 */
    static final double PAGINATION_SPACING = 16;
    static final double PAGINATION_BUTTON_WIDTH = 104;

    /** 搜索表单的方向。 */
    enum FormMode {
        /** 标签在左、控件在右，两行两列，字段列与下拉列按 2:1 分配。 */
        INLINE,
        /** 标签在上、控件在下，两行两列等宽。 */
        GRID,
        /** 标签在上、控件在下，单列纵向堆叠。 */
        STACKED
    }

    /** 首帧宽度尚未确定时使用的方向。 */
    static final Plan WIDE = create(FormMode.INLINE);

    private ModDownloadLayout() {
    }

    /**
     * 该内容宽度下使用的搜索表单方向。
     *
     * <p>阈值按控件可用宽度推导：{@code INLINE} 需要标签列 {@code 64}、列间距 {@code 16}
     * 之后下拉框仍能显示“全部来源”之类的短文案，因此要求页面宽度不低于 {@code 660}
     * （此时 2:1 权重下下拉框约 {@code 112–122}）；低于 {@code 520} 时两列会挤压标签，
     * 改为单列堆叠。</p>
     *
     * @param width 页面内容宽度（已扣除左侧导航）
     * @throws IllegalArgumentException 宽度不是正数
     */
    static FormMode formMode(double width) {
        if (width <= 0) {
            throw new IllegalArgumentException("内容宽度必须为正数");
        }
        if (width >= INLINE_FORM_MIN_WIDTH) {
            return FormMode.INLINE;
        }
        return width >= STACKED_FORM_MIN_WIDTH ? FormMode.GRID : FormMode.STACKED;
    }

    /**
     * 计算某一内容宽度下的模组页布局。
     *
     * @param width 页面内容宽度（已扣除左侧导航）
     * @throws IllegalArgumentException 宽度不是正数
     */
    static Plan plan(double width) {
        return create(formMode(width));
    }

    /**
     * 计算下载页侧边栏宽度。
     *
     * @param windowWidth 窗口宽度
     * @throws IllegalArgumentException 宽度不是正数
     */
    static double sidebarWidth(double windowWidth) {
        if (windowWidth <= 0) {
            throw new IllegalArgumentException("窗口宽度必须为正数");
        }
        return Math.clamp(windowWidth * SIDEBAR_RATIO, SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH);
    }

    private static Plan create(FormMode mode) {
        return new Plan(
                mode,
                CONTENT_PADDING_HORIZONTAL,
                CONTENT_PADDING_VERTICAL,
                CARD_PADDING,
                CARD_SPACING,
                SECTION_SPACING,
                FORM_GAP,
                FIELD_LABEL_WIDTH,
                PRIMARY_FIELD_WEIGHT,
                SECONDARY_FIELD_WEIGHT,
                ICON_SIZE,
                ROW_MIN_HEIGHT,
                ROW_PADDING_VERTICAL,
                ROW_PADDING_LEFT,
                ROW_PADDING_RIGHT,
                ACCENT_WIDTH,
                LIST_SPACING,
                PAGINATION_SPACING,
                PAGINATION_BUTTON_WIDTH);
    }

    /**
     * 模组下载页布局参数。
     *
     * @param mode                     搜索表单方向
     * @param contentPaddingHorizontal 内容区左右留白
     * @param contentPaddingVertical   内容区上下留白
     * @param cardPadding              搜索卡与详情卡内边距
     * @param cardSpacing              卡片内部间距
     * @param sectionSpacing           页面区块间距
     * @param formGap                  表单行列间距
     * @param fieldLabelWidth          标签列宽度
     * @param primaryFieldWeight       字段列权重
     * @param secondaryFieldWeight     下拉列权重
     * @param iconSize                 结果图标边长
     * @param rowMinHeight             结果行最小高度
     * @param rowPaddingVertical       结果行上下内边距
     * @param rowPaddingLeft           结果行左内边距
     * @param rowPaddingRight          结果行右内边距
     * @param accentWidth              强调条宽度
     * @param listSpacing              结果行间距
     * @param paginationSpacing        分页间距
     * @param paginationButtonWidth    分页按钮最小宽度
     */
    record Plan(
            FormMode mode,
            double contentPaddingHorizontal,
            double contentPaddingVertical,
            double cardPadding,
            double cardSpacing,
            double sectionSpacing,
            double formGap,
            double fieldLabelWidth,
            double primaryFieldWeight,
            double secondaryFieldWeight,
            double iconSize,
            double rowMinHeight,
            double rowPaddingVertical,
            double rowPaddingLeft,
            double rowPaddingRight,
            double accentWidth,
            double listSpacing,
            double paginationSpacing,
            double paginationButtonWidth
    ) {
    }
}