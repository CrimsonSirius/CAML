package org.chs.camlauncher.ui.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

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
        setMinHeight(28);
        setPrefHeight(28);
        setMaxWidth(Double.MAX_VALUE);

        track.getStyleClass().add("caml-slider-track");
        fill.getStyleClass().add("caml-slider-fill");
        thumb.getStyleClass().add("caml-slider-thumb");
        track.setMouseTransparent(true);
        fill.setMouseTransparent(true);
        thumb.setMouseTransparent(true);
        getChildren().addAll(track, fill, thumb);

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
        double trackStart = THUMB_SIZE / 2;
        double trackLength = Math.max(0, getWidth() - THUMB_SIZE);
        double centerX = pointFor(getValue(), trackStart, trackLength);
        double trackY = (getHeight() - TRACK_HEIGHT) / 2;

        track.resizeRelocate(trackStart, trackY, trackLength, TRACK_HEIGHT);
        fill.resizeRelocate(trackStart, trackY,
                Math.max(0, centerX - trackStart), TRACK_HEIGHT);
        thumb.resizeRelocate(
                centerX - THUMB_SIZE / 2,
                (getHeight() - THUMB_SIZE) / 2,
                THUMB_SIZE,
                THUMB_SIZE
        );
    }

    private double pointFor(int value, double trackStart, double trackLength) {
        double fraction = (value - min) / (double) (max - min);
        return trackStart + fraction * trackLength;
    }

    private void updateFromMouse(double x) {
        double trackStart = THUMB_SIZE / 2;
        double trackLength = Math.max(1, getWidth() - THUMB_SIZE);
        double fraction = Math.max(0, Math.min(1, (x - trackStart) / trackLength));
        setValue((int) Math.round(min + fraction * (max - min)));
    }
}
