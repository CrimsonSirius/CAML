package org.chs.camlauncher.ui.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.chs.camlauncher.ui.theme.CamlMetrics;

/** CAML 整数滑块：一条轨道、一个填充段和一个圆形滑球。 */
public final class CamlSlider extends Pane {
    private static final double TRACK_HEIGHT = 4;
    private static final double THUMB_SIZE = 20;

    private final int min;
    private final int max;
    private final int step;
    private final IntegerProperty value = new SimpleIntegerProperty(this, "value");
    private final Region track = new Region();
    private final Region fill = new Region();
    private final Region thumb = new Region();
    private final ChangeListener<Number> metricsListener;
    private final WeakChangeListener<Number> weakMetricsListener;
    private double trackHeight = TRACK_HEIGHT;
    private double thumbSize = THUMB_SIZE;

    public CamlSlider(int min, int max, int initialValue) {
        if (max <= min) {
            throw new IllegalArgumentException("滑块范围无效");
        }
        this.min = min;
        this.max = max;
        this.step = 1;

        getStyleClass().add("caml-slider");
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.SLIDER);
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        track.getStyleClass().add("caml-slider-track");
        fill.getStyleClass().add("caml-slider-fill");
        thumb.getStyleClass().add("caml-slider-thumb");
        track.setMouseTransparent(true);
        fill.setMouseTransparent(true);
        thumb.setMouseTransparent(true);
        getChildren().addAll(track, fill, thumb);

        metricsListener = (observable, oldValue, newValue) -> applyMetrics();
        weakMetricsListener = new WeakChangeListener<>(metricsListener);
        CamlMetrics.scaleProperty().addListener(weakMetricsListener);
        applyMetrics();

        value.addListener((observable, oldValue, newValue) -> requestLayout());
        setValue(initialValue);

        setOnMousePressed(event -> {
            requestFocus();
            updateFromMouse(event.getX());
            event.consume();
        });
        setOnMouseDragged(event -> {
            updateFromMouse(event.getX());
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

    @Override
    protected void layoutChildren() {
        double trackStart = thumbSize / 2;
        double trackLength = Math.max(0, getWidth() - thumbSize);
        double centerX = pointFor(getValue(), trackStart, trackLength);
        double trackY = (getHeight() - trackHeight) / 2;

        track.resizeRelocate(trackStart, trackY, trackLength, trackHeight);
        fill.resizeRelocate(trackStart, trackY,
                Math.max(0, centerX - trackStart), trackHeight);
        thumb.resizeRelocate(
                centerX - thumbSize / 2,
                (getHeight() - thumbSize) / 2,
                thumbSize,
                thumbSize
        );
    }

    private double pointFor(int value, double trackStart, double trackLength) {
        double fraction = (value - min) / (double) (max - min);
        return trackStart + fraction * trackLength;
    }

    private void updateFromMouse(double x) {
        double trackStart = thumbSize / 2;
        double trackLength = Math.max(1, getWidth() - thumbSize);
        double fraction = Math.max(0, Math.min(1, (x - trackStart) / trackLength));
        setValue((int) Math.round(min + fraction * (max - min)));
    }

    private void applyMetrics() {
        trackHeight = CamlMetrics.size(TRACK_HEIGHT);
        thumbSize = CamlMetrics.size(THUMB_SIZE);
        setMinHeight(CamlMetrics.size(28));
        setPrefHeight(CamlMetrics.size(28));
        requestLayout();
    }
}
