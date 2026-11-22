package org.chs.camlauncher.ui.control;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.chs.camlauncher.ui.theme.CamlMotion;

/** CAML 顶部主导航项目。 */
public final class CamlTopNavItem extends ToggleButton {
    private static final Duration PRESS_DURATION = Duration.millis(70);
    private static final Duration RELEASE_DURATION = Duration.millis(160);
    private static final double PRESS_SCALE = 0.96;
    private ScaleTransition scaleAnimation;

    public CamlTopNavItem(String text) {
        super(text);
        configure();
    }

    public CamlTopNavItem(Node icon, String text) {
        super(text, icon);
        setContentDisplay(ContentDisplay.LEFT);
        configure();
    }

    private void configure() {
        getStyleClass().add("caml-top-nav-item");
        setFocusTraversable(true);
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> animateScale(PRESS_SCALE, PRESS_DURATION));
        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> animateScale(1, RELEASE_DURATION));
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> animateScale(1, RELEASE_DURATION));
    }

    private void animateScale(double target, Duration duration) {
        if (CamlMotion.reduceMotion()) {
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
