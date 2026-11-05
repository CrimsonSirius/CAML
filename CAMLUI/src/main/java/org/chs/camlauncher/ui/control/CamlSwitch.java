package org.chs.camlauncher.ui.control;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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

    public CamlSwitch() {
        this(false);
    }

    public CamlSwitch(boolean selected) {
        getStyleClass().add("caml-switch");
        setAlignment(Pos.CENTER_LEFT);
        setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
        setFocusTraversable(true);

        track.getStyleClass().add("caml-switch-track");
        track.setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        track.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        track.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        thumb.getStyleClass().add("caml-switch-thumb");
        thumb.setMinSize(THUMB_SIZE, THUMB_SIZE);
        thumb.setPrefSize(THUMB_SIZE, THUMB_SIZE);
        thumb.setMaxSize(THUMB_SIZE, THUMB_SIZE);
        thumb.setMouseTransparent(true);
        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
        StackPane.setMargin(thumb, new Insets(0, 0, 0, 4));

        getChildren().addAll(track, thumb);

        this.selected.set(selected);
        updateThumb(selected, false);
        this.selected.addListener((observable, oldValue, newValue) -> {
            pseudoClassStateChanged(SELECTED, newValue);
            animateThumb(newValue);
        });

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
        thumbAnimation.stop();
        thumbAnimation.setToX(selected ? THUMB_TRAVEL : 0);
        thumbAnimation.play();
    }

    private void updateThumb(boolean selected, boolean animate) {
        pseudoClassStateChanged(SELECTED, selected);
        if (animate) {
            animateThumb(selected);
        } else {
            thumb.setTranslateX(selected ? THUMB_TRAVEL : 0);
        }
    }
}
