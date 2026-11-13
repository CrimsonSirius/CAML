package org.chs.camlauncher.front;

/**
 * 根据屏幕可用区域计算启动器窗口的初始尺寸和最小尺寸。
 *
 * <p>启动器不使用固定窗口大小，而是让窗口与 {@link UiScale} 的缩放范围对应：
 * 初始尺寸取屏幕可用区域的固定比例，既不会在小屏幕上溢出桌面，也不会在大屏幕上显得过小。
 * 上限是设计基准 {@code 1440×900} 放大到最大缩放因子后的尺寸，下限是同一基准缩小到
 * 最小缩放因子后的尺寸——因此窗口在任何屏幕上都只落在“界面能按比例完整显示”的区间内。</p>
 *
 * <p>初始位置居中，最小尺寸同样跟随屏幕收缩，保证笔记本等小屏设备的窗口始终放得下。
 * 该类只做纯计算，不依赖 JavaFX，便于单元测试。</p>
 */
final class WindowSizePlanner {
    /** 初始窗口占屏幕可用宽度的比例。 */
    static final double PREFERRED_WIDTH_RATIO = 0.68;
    /** 初始窗口占屏幕可用高度的比例。 */
    static final double PREFERRED_HEIGHT_RATIO = 0.74;
    /** 初始窗口最多占屏幕的比例，用于保留桌面边距。 */
    static final double MAX_WIDTH_RATIO = 0.92;
    static final double MAX_HEIGHT_RATIO = 0.90;
    /**
     * 大屏幕上的上限：设计基准放大到最大缩放因子。
     *
     * <p>上限与 {@code UiScale.MAX_FACTOR} 绑定，因此窗口放大时界面同步放大，
     * 不会出现“窗口变大但内容仍是设计稿大小”的比例失衡。</p>
     */
    static final double MAX_WIDTH = Math.floor(UiScale.maxWindowWidth());
    static final double MAX_HEIGHT = Math.floor(UiScale.maxWindowHeight());
    /**
     * 设计上的最小可用尺寸：设计基准缩小到最小缩放因子。
     *
     * <p>再窄就会触发缩放下限，界面比例无法保持，因此把它作为窗口的最小宽度和高度；
     * 屏幕本身更小时按 {@link #MIN_SCREEN_RATIO} 继续收缩，保证窗口仍能放进桌面。</p>
     */
    static final double DESIGN_MIN_WIDTH = Math.floor(UiScale.size(UiScale.DESIGN_WIDTH, UiScale.MIN_FACTOR));
    static final double DESIGN_MIN_HEIGHT = Math.floor(UiScale.size(UiScale.DESIGN_HEIGHT, UiScale.MIN_FACTOR));
    /** 小屏幕上最小尺寸最多占屏幕的比例，保证窗口仍能放进桌面。 */
    static final double MIN_SCREEN_RATIO = 0.90;

    private WindowSizePlanner() {
    }

    /**
     * 计算窗口几何信息。
     *
     * @param screenX      屏幕可用区域左上角 X
     * @param screenY      屏幕可用区域左上角 Y
     * @param screenWidth  屏幕可用区域宽度
     * @param screenHeight 屏幕可用区域高度
     */
    static WindowMetrics compute(double screenX, double screenY, double screenWidth, double screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("屏幕可用区域必须为正数");
        }

        double maxWidth = Math.floor(Math.min(MAX_WIDTH, screenWidth * MAX_WIDTH_RATIO));
        double maxHeight = Math.floor(Math.min(MAX_HEIGHT, screenHeight * MAX_HEIGHT_RATIO));

        // 最小尺寸不能超过当前屏幕能提供的上限，否则窗口会大于桌面。
        double minWidth = Math.min(Math.floor(Math.min(DESIGN_MIN_WIDTH, screenWidth * MIN_SCREEN_RATIO)), maxWidth);
        double minHeight = Math.min(Math.floor(Math.min(DESIGN_MIN_HEIGHT, screenHeight * MIN_SCREEN_RATIO)),
                maxHeight);

        double width = Math.floor(Math.clamp(screenWidth * PREFERRED_WIDTH_RATIO, minWidth, maxWidth));
        double height = Math.floor(Math.clamp(screenHeight * PREFERRED_HEIGHT_RATIO, minHeight, maxHeight));

        double x = screenX + Math.floor((screenWidth - width) / 2);
        double y = screenY + Math.floor((screenHeight - height) / 2);
        return new WindowMetrics(new WindowGeometry(x, y, width, height), minWidth, minHeight);
    }

    /** 窗口位置与尺寸。 */
    record WindowGeometry(double x, double y, double width, double height) {
    }

    /** 初始窗口几何信息与最小尺寸。 */
    record WindowMetrics(WindowGeometry window, double minWidth, double minHeight) {
    }
}