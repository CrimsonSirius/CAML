package org.chs.camlauncher.front.view;

import org.chs.camlauncher.front.UiScale;

/**
 * 模组下载页的布局规则。
 *
 * <p>所有常量都是设计稿像素（{@code 1440×900} 窗口下的取值），实际使用前统一乘以
 * {@link UiScale} 给出的缩放因子，因此大屏和小屏上的模组页与设计稿保持同一套比例：
 * 下载页左侧导航约占窗口宽度的 {@code 1/6}（限制在 {@code 180–260} 设计像素），
 * 内容区左右留白 {@code 40}、上下 {@code 32}；搜索卡内边距 {@code 24}、内部间距 {@code 18}；
 * 结果行图标 {@code 88}、行高 {@code 112}，来源标签贴在行尾。</p>
 *
 * <p>搜索表单按缩放后的内容宽度在三种方向之间切换：{@code >= 660} 使用“标签在左”的
 * 两行两列，{@code >= 520} 使用“标签在上”的两行两列，更窄时改为单列纵向堆叠。
 * 断点同样乘以缩放因子——字号放大后字段需要更多空间，缩小后则更早进入两列布局。</p>
 *
 * <p>页面高度不足以同时容纳搜索卡、状态、结果和分页时，由整页纵向滚动接管；
 * 结果行因此不会被裁切。该类只做纯计算，不依赖 JavaFX，便于单元测试。</p>
 */
final class ModDownloadLayout {

    /** 搜索表单方向切换点，单位为缩放后的页面内容宽度（已扣除左侧导航）。 */
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
     * <p>模组页的卡片不写 {@code -fx-padding}：几何由本类按缩放因子给出，
     * 避免 CSS 与代码争抢同一属性。</p>
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

    /**
     * 首帧宽度和缩放因子尚未确定时使用的方向。
     *
     * <p>取设计基准因子，并假设内容宽度足够放两行两列，因此与宽屏结果一致，
     * 不会在首帧与真实布局之间重建两次表单。</p>
     */
    static final Plan WIDE = create(FormMode.INLINE, UiScale.DEFAULT_FACTOR);

    private ModDownloadLayout() {
    }

    /**
     * 该内容宽度与缩放因子下使用的搜索表单方向。
     *
     * <p>阈值按控件可用宽度推导：{@code INLINE} 需要标签列 {@code 64}、列间距 {@code 16}
     * 之后下拉框仍能显示“全部来源”之类的短文案，因此要求宽度不低于 {@code 660}；
     * 低于 {@code 520} 时两列会挤压标签，改为单列堆叠。两个阈值都乘以缩放因子，
     * 因为缩放同时作用于字号和控件尺寸。</p>
     *
     * @param width  页面内容宽度（已扣除左侧导航）
     * @param factor UI 缩放因子
     * @throws IllegalArgumentException 宽度或因子不是正数
     */
    static FormMode formMode(double width, double factor) {
        if (width <= 0 || factor <= 0) {
            throw new IllegalArgumentException("内容宽度和缩放因子必须为正数");
        }
        if (width >= UiScale.size(INLINE_FORM_MIN_WIDTH, factor)) {
            return FormMode.INLINE;
        }
        return width >= UiScale.size(STACKED_FORM_MIN_WIDTH, factor)
                ? FormMode.GRID
                : FormMode.STACKED;
    }

    /**
     * 计算某一内容宽度与缩放因子下的模组页布局。
     *
     * @param width  页面内容宽度（已扣除左侧导航）
     * @param factor UI 缩放因子
     * @throws IllegalArgumentException 宽度或因子不是正数
     */
    static Plan plan(double width, double factor) {
        return create(formMode(width, factor), factor);
    }

    /**
     * 计算下载页侧边栏宽度。
     *
     * <p>比例取窗口宽度的 {@code 1/6}，上下限同样随缩放因子移动，
     * 避免小屏上侧边栏占比过大、大屏上占比过小。</p>
     *
     * @param windowWidth 窗口宽度
     * @param factor      UI 缩放因子
     * @throws IllegalArgumentException 宽度或因子不是正数
     */
    static double sidebarWidth(double windowWidth, double factor) {
        if (windowWidth <= 0 || factor <= 0) {
            throw new IllegalArgumentException("窗口宽度和缩放因子必须为正数");
        }
        return Math.clamp(windowWidth * SIDEBAR_RATIO,
                UiScale.size(SIDEBAR_MIN_WIDTH, factor),
                UiScale.size(SIDEBAR_MAX_WIDTH, factor));
    }

    private static Plan create(FormMode mode, double factor) {
        return new Plan(
                mode,
                factor,
                UiScale.size(CONTENT_PADDING_HORIZONTAL, factor),
                UiScale.size(CONTENT_PADDING_VERTICAL, factor),
                UiScale.size(CARD_PADDING, factor),
                UiScale.size(CARD_SPACING, factor),
                UiScale.size(SECTION_SPACING, factor),
                UiScale.size(FORM_GAP, factor),
                UiScale.size(FIELD_LABEL_WIDTH, factor),
                PRIMARY_FIELD_WEIGHT,
                SECONDARY_FIELD_WEIGHT,
                UiScale.size(ICON_SIZE, factor),
                UiScale.size(ROW_MIN_HEIGHT, factor),
                UiScale.size(ROW_PADDING_VERTICAL, factor),
                UiScale.size(ROW_PADDING_LEFT, factor),
                UiScale.size(ROW_PADDING_RIGHT, factor),
                UiScale.size(ACCENT_WIDTH, factor),
                UiScale.size(LIST_SPACING, factor),
                UiScale.size(PAGINATION_SPACING, factor),
                UiScale.size(PAGINATION_BUTTON_WIDTH, factor));
    }

    /**
     * 模组下载页布局参数。
     *
     * @param mode                     搜索表单方向
     * @param factor                   UI 缩放因子
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
            double factor,
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
