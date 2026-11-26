package org.chs.camlauncher.backend.settings;

/** 启动器窗口的持久化位置、尺寸和最大化状态。 */
public record WindowState(double x, double y, double width, double height, boolean maximized) {
    public WindowState {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || !Double.isFinite(height)
                || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("窗口位置和尺寸无效");
        }
    }
}
