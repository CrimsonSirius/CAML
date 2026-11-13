package org.chs.camlauncher.front;

/**
 * 界面缩放规则：把窗口尺寸换算成统一的缩放因子。
 *
 * <p>设计基准是 {@code 1440×900} 的逻辑窗口（图示按 16:10 比例绘制），因子为 {@code 1} 时
 * 根字号 {@code 16px}，CSS 中的 {@code 1em} 等于设计稿的 {@code 16px}。窗口比基准小就整体缩小，
 * 比基准大就整体放大，因此大屏和小屏看到的字号、控件尺寸与留白比例都与设计稿一致，
 * 不会出现“窗口变大但文字不变”或“小屏塞不下”的情况。</p>
 *
 * <p>因子取宽、高两个方向比例的较小值，并量化到 {@code 1/16}（根字号取整数像素），
 * 避免半像素字号带来的抖动，也让 CSS（{@code em}）与代码里按因子换算的几何完全一致。
 * 因子限制在 {@code 0.6875–1.375}（根字号 {@code 11–22px}）：低于下限时字太小无法阅读，
 * 高于上限时单行文字过长、图标过于笨重。</p>
 *
 * <p>{@link WindowSizePlanner} 用 {@link #MAX_FACTOR} 推导窗口的绝对上限，
 * {@code ModDownloadLayout}、{@code WorkspaceLayout} 与 {@code ViewSupport} 用同一因子换算
 * 无法由 CSS 表达的几何。该类只做纯计算，不依赖 JavaFX，便于单元测试。</p>
 */
public final class UiScale {

    /** 设计基准窗口尺寸，缩放因子为 1 时对应这个窗口。 */
    public static final double DESIGN_WIDTH = 1440;
    public static final double DESIGN_HEIGHT = 900;

    /** 设计基准下的根字号。CSS 中的 1em 等于该值乘以缩放因子。 */
    public static final double BASE_FONT_PX = 16;

    /** 缩放因子下限与上限，对应根字号 11px 与 22px。 */
    public static final double MIN_FACTOR = 11.0 / BASE_FONT_PX;
    public static final double MAX_FACTOR = 22.0 / BASE_FONT_PX;

    /** 首帧尚未拿到窗口尺寸时使用的因子（等于设计基准）。 */
    public static final double DEFAULT_FACTOR = 1;

    private UiScale() {
    }

    /**
     * 计算窗口对应的缩放因子。
     *
     * <p>取宽、高两个方向比例的较小值：窗口变窄或变矮都会等比缩小整个界面，
     * 保证内容区始终放得下，也不会在某一方向上被拉伸变形。</p>
     *
     * @param windowWidth  窗口内容宽度
     * @param windowHeight 窗口内容高度
     * @throws IllegalArgumentException 宽或高不是正数
     */
    public static double factor(double windowWidth, double windowHeight) {
        if (windowWidth <= 0 || windowHeight <= 0) {
            throw new IllegalArgumentException("窗口尺寸必须为正数");
        }
        double ratio = Math.min(windowWidth / DESIGN_WIDTH, windowHeight / DESIGN_HEIGHT);
        return quantize(Math.clamp(ratio, MIN_FACTOR, MAX_FACTOR));
    }

    /**
     * 把原始比例量化成根字号为整数像素的因子。
     *
     * @param ratio 未量化的比例
     */
    public static double quantize(double ratio) {
        return Math.round(ratio * BASE_FONT_PX) / BASE_FONT_PX;
    }

    /** @return 该缩放因子对应的根字号（整数像素） */
    public static double baseFontPx(double factor) {
        return quantize(factor) * BASE_FONT_PX;
    }

    /**
     * 按缩放因子换算设计稿中的像素值。
     *
     * <p>工作区几何需要与 CSS 的 {@code em} 折算保持一致，因此直接复用同一因子。</p>
     */
    public static double size(double designPx, double factor) {
        return designPx * factor;
    }

    /** @return 设计基准下的完整窗口宽度放大到最大因子时的像素宽度 */
    public static double maxWindowWidth() {
        return DESIGN_WIDTH * MAX_FACTOR;
    }

    /** @return 设计基准下的完整窗口高度放大到最大因子时的像素高度 */
    public static double maxWindowHeight() {
        return DESIGN_HEIGHT * MAX_FACTOR;
    }
}

