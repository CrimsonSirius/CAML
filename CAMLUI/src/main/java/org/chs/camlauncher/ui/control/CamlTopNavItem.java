package org.chs.camlauncher.ui.control;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/** CAML 顶部主导航项目。 */
public final class CamlTopNavItem extends ToggleButton {
    private static final Duration PRESS_DURATION = Duration.millis(70);
    private static final Duration RELEASE_DURATION = Duration.millis(160);
    private static final double PRESS_SCALE = 0.96;
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("caml.reduceMotion");

    private ScaleTransition scaleAnimation;

    public CamlTopNavItem(String text) {
        super(text);
        getStyleClass().add("caml-top-nav-item");
        setFocusTraversable(true);
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
