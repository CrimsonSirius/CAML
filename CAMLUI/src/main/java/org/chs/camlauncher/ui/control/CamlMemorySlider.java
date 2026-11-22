package org.chs.camlauncher.ui.control;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.chs.camlauncher.ui.theme.CamlMetrics;
import org.chs.camlauncher.ui.theme.CamlMotion;

/**
 * CAML 内存分配滑块。
 *
 * <p>同一轨道分两层渲染：底层灰色条宽度为“系统已使用 + 游戏分配”，
 * 上层黄色分配线和空心小球覆盖底层左侧；因此灰色可见部分位于小球右侧，
 * 并随小球左右移动。底层剩余未使用部分为半粗淡灰色。</p>
 */
public final class CamlMemorySlider extends Pane {
    private static final double THICK_BAR_HEIGHT = 4;
    private static final double THIN_BAR_HEIGHT = 2;
    private static final double THUMB_SIZE = 18;
    private static final double PRESS_SCALE = 0.84;
    private final int min;
    private final int max;
    private final int step;
    private int usedMemoryMb;
    private final IntegerProperty value = new SimpleIntegerProperty(this, "value");
    private final Region freeTrack = new Region();
    private final Region usedGameBar = new Region();
    private final Region fill = new Region();
    private final Region thumb = new Region();
    private final ChangeListener<Number> metricsListener;
    private final WeakChangeListener<Number> weakMetricsListener;
    private ScaleTransition thumbScale;
    private double thickBarHeight = THICK_BAR_HEIGHT;
    private double thinBarHeight = THIN_BAR_HEIGHT;
    private double thumbSize = THUMB_SIZE;

    public CamlMemorySlider(int min, int max, int initialValue) {
        this(min, max, 256, initialValue, 0);
    }

    public CamlMemorySlider(int min, int max, int step, int initialValue, int usedMemoryMb) {
        if (min < 0 || max <= min) {
            throw new IllegalArgumentException("内存范围无效");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("步进必须为正数");
        }
        this.min = min;
        this.max = max;
        this.step = step;
        this.usedMemoryMb = Math.max(0, usedMemoryMb);

        getStyleClass().add("caml-memory-slider");
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.SLIDER);
        setAccessibleText("最大内存");
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        freeTrack.getStyleClass().add("caml-memory-slider-free");
        usedGameBar.getStyleClass().add("caml-memory-slider-used");
        fill.getStyleClass().add("caml-memory-slider-fill");
        thumb.getStyleClass().add("caml-memory-slider-thumb");
        freeTrack.setMouseTransparent(true);
        usedGameBar.setMouseTransparent(true);
        fill.setMouseTransparent(true);
        thumb.setMouseTransparent(true);
        // 从下到上：淡灰未使用底线 -> 灰色已使用+游戏分配 -> 黄色分配前景 -> 小球。
        getChildren().addAll(freeTrack, usedGameBar, fill, thumb);

        metricsListener = (observable, oldValue, newValue) -> applyMetrics();
        weakMetricsListener = new WeakChangeListener<>(metricsListener);
        CamlMetrics.scaleProperty().addListener(weakMetricsListener);
        applyMetrics();

        value.addListener((observable, oldValue, newValue) -> requestLayout());
        setValue(initialValue);

        setOnMousePressed(event -> {
            requestFocus();
            updateFromMouse(event.getX());
            animateThumb(PRESS_SCALE, Duration.millis(40));
            event.consume();
        });
        setOnMouseDragged(event -> {
            updateFromMouse(event.getX());
            event.consume();
        });
        setOnMouseReleased(event -> {
            animateThumb(1, Duration.millis(200));
            event.consume();
        });
        setOnKeyPressed(event -> {
            int next = switch (event.getCode()) {
                case LEFT, DOWN -> getValue() - step;
                case RIGHT, UP -> getValue() + step;
                case PAGE_DOWN -> getValue() - step * 10;
                case PAGE_UP -> getValue() + step * 10;
                case HOME -> min;
                case END -> max;
                default -> getValue();
            };
            if (next != getValue()) {
                setValue(next);
                event.consume();
            }
        });
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        int rounded = Math.round(value / (float) step) * step;
        this.value.set(Math.max(min, Math.min(max, rounded)));
    }

    public void setUsedMemoryMb(int usedMemoryMb) {
        this.usedMemoryMb = Math.max(0, usedMemoryMb);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double trackStart = thumbSize / 2;
        double trackLength = Math.max(0, getWidth() - thumbSize);
        double thickY = (getHeight() - thickBarHeight) / 2;
        double thinY = (getHeight() - thinBarHeight) / 2;

        double allocatedCenter = pointFor(getValue(), trackStart, trackLength);
        int usedGameMb = Math.min(max, usedMemoryMb + getValue());
        double usedGameCenter = pointFor(usedGameMb, trackStart, trackLength);

        freeTrack.resizeRelocate(trackStart, thinY, trackLength, thinBarHeight);
        usedGameBar.resizeRelocate(trackStart, thickY,
                Math.max(0, usedGameCenter - trackStart), thickBarHeight);
        fill.resizeRelocate(trackStart, thickY,
                Math.max(0, allocatedCenter - trackStart), thickBarHeight);
        thumb.resizeRelocate(allocatedCenter - thumbSize / 2, (getHeight() - thumbSize) / 2,
                thumbSize, thumbSize);
    }

    private double pointFor(int memoryMb, double trackStart, double trackLength) {
        double fraction = (memoryMb - min) / (double) (max - min);
        return trackStart + fraction * trackLength;
    }

    private void updateFromMouse(double x) {
        double trackStart = thumbSize / 2;
        double trackLength = Math.max(1, getWidth() - thumbSize);
        double fraction = Math.max(0, Math.min(1, (x - trackStart) / trackLength));
        setValue((int) Math.round(min + fraction * (max - min)));
    }

    private void applyMetrics() {
        thickBarHeight = CamlMetrics.size(THICK_BAR_HEIGHT);
        thinBarHeight = CamlMetrics.size(THIN_BAR_HEIGHT);
        thumbSize = CamlMetrics.size(THUMB_SIZE);
        setMinHeight(CamlMetrics.size(28));
        setPrefHeight(CamlMetrics.size(28));
        requestLayout();
    }

    private void animateThumb(double target, Duration duration) {
        if (CamlMotion.reduceMotion()) {
            return;
        }
        if (thumbScale != null) {
            thumbScale.stop();
        }
        thumbScale = new ScaleTransition(duration, thumb);
        thumbScale.setToX(target);
        thumbScale.setToY(target);
        thumbScale.setInterpolator(Interpolator.EASE_OUT);
        thumbScale.play();
    }
}
