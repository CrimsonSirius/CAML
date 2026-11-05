package org.chs.camlauncher.ui.control;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/** CAML 侧边导航项目。 */
public final class CamlSideNavItem extends Button {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final Duration PRESS_DURATION = Duration.millis(70);
    private static final Duration RELEASE_DURATION = Duration.millis(160);
    private static final double PRESS_SCALE = 0.96;
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("caml.reduceMotion");

    private boolean selected;
    private ScaleTransition scaleAnimation;

    public CamlSideNavItem(Node icon, String label) {
        super(label, icon);
        getStyleClass().add("caml-side-nav-item");
        setContentDisplay(ContentDisplay.TOP);
        setGraphicTextGap(4);
        setAlignment(Pos.CENTER);
        setMaxWidth(Double.MAX_VALUE);
        setFocusTraversable(true);
        installPressAnimation();
    }

    public CamlSideNavItem(String label) {
        super(label);
        getStyleClass().add("caml-side-nav-item");
        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);
        setFocusTraversable(true);
        installPressAnimation();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        pseudoClassStateChanged(SELECTED, selected);
    }

    private void installPressAnimation() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> animateScale(PRESS_SCALE, PRESS_DURATION));
        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> animateScale(1, RELEASE_DURATION));
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> animateScale(1, RELEASE_DURATION));
    }

    private void animateScale(double target, Duration duration) {
        if (REDUCE_MOTION) {
            return;
        }
        if (scaleAnimation != null) {
            scaleAnimation.stop();
        }
        scaleAnimation = new ScaleTransition(duration, this);
        scaleAnimation.setToX(target);
        scaleAnimation.setToY(target);
        scaleAnimation.setInterpolator(Interpolator.EASE_OUT);
        scaleAnimation.play();
    }
}
