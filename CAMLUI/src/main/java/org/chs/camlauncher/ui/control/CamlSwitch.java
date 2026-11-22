package org.chs.camlauncher.ui.control;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.chs.camlauncher.ui.theme.CamlMetrics;
import org.chs.camlauncher.ui.theme.CamlMotion;

/** CAML 开关。JavaFX 没有原生 Switch，这里用轨道和滑块组合实现。 */
public final class CamlSwitch extends StackPane {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final double TRACK_WIDTH = 52;
    private static final double TRACK_HEIGHT = 32;
    private static final double THUMB_SIZE = 24;
    private static final double THUMB_TRAVEL = 20;

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected");
    private final Region track = new Region();
    private final Region thumb = new Region();
    private final TranslateTransition thumbAnimation = new TranslateTransition(Duration.millis(160), thumb);
    private final ChangeListener<Number> metricsListener;
    private final WeakChangeListener<Number> weakMetricsListener;
    private double thumbTravel = THUMB_TRAVEL;

    public CamlSwitch() {
        this(false);
    }

    public CamlSwitch(boolean selected) {
        getStyleClass().add("caml-switch");
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);

        track.getStyleClass().add("caml-switch-track");

        thumb.getStyleClass().add("caml-switch-thumb");
        thumb.setMouseTransparent(true);
        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);

        getChildren().addAll(track, thumb);

        this.selected.set(selected);
        this.selected.addListener((observable, oldValue, newValue) -> {
            pseudoClassStateChanged(SELECTED, newValue);
            animateThumb(newValue);
        });
        metricsListener = (observable, oldValue, newValue) -> applyMetrics();
        weakMetricsListener = new WeakChangeListener<>(metricsListener);
        CamlMetrics.scaleProperty().addListener(weakMetricsListener);
        applyMetrics();

        setOnMouseClicked(event -> {
            if (!isDisabled()) {
                requestFocus();
                setSelected(!isSelected());
            }
            event.consume();
        });
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                setSelected(!isSelected());
                event.consume();
            }
        });

        setAccessibleRole(AccessibleRole.CHECK_BOX);
        setAccessibleText("Switch");
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    private void animateThumb(boolean selected) {
        if (CamlMotion.reduceMotion()) {
            thumbAnimation.stop();
            thumb.setTranslateX(selected ? thumbTravel : 0);
            return;
        }
        thumbAnimation.stop();
        thumbAnimation.setToX(selected ? thumbTravel : 0);
        thumbAnimation.play();
    }

    private void updateThumb(boolean selected, boolean animate) {
        pseudoClassStateChanged(SELECTED, selected);
        if (animate) {
            animateThumb(selected);
        } else {
            thumb.setTranslateX(selected ? thumbTravel : 0);
        }
    }

    private void applyMetrics() {
        double trackWidth = CamlMetrics.size(TRACK_WIDTH);
        double trackHeight = CamlMetrics.size(TRACK_HEIGHT);
        double thumbSize = CamlMetrics.size(THUMB_SIZE);
        thumbTravel = CamlMetrics.size(THUMB_TRAVEL);
        setMinSize(trackWidth, trackHeight);
        setPrefSize(trackWidth, trackHeight);
        setMaxSize(trackWidth, trackHeight);
        track.setMinSize(trackWidth, trackHeight);
        track.setPrefSize(trackWidth, trackHeight);
        track.setMaxSize(trackWidth, trackHeight);
        thumb.setMinSize(thumbSize, thumbSize);
        thumb.setPrefSize(thumbSize, thumbSize);
        thumb.setMaxSize(thumbSize, thumbSize);
        StackPane.setMargin(thumb, new Insets(0, 0, 0, CamlMetrics.size(4)));
        updateThumb(isSelected(), false);
    }
}
