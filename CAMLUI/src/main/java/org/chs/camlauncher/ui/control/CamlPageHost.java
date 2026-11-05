package org.chs.camlauncher.ui.control;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Objects;

/**
 * 带页面切换动画的容器。
 *
 * <p>节奏参考 PCL：旧页面短暂淡出并上移，新页面延迟淡入并从上方向下归位。
 * 动画只用于表达页面切换的方向和连续性，不做装饰性运动。</p>
 */
public final class CamlPageHost extends StackPane {
    private static final Duration EXIT_DURATION = Duration.millis(90);
    private static final Duration ENTER_DURATION = Duration.millis(220);
    private static final Duration ENTER_DELAY = Duration.millis(30);
    private static final double ENTER_OFFSET_Y = -12;
    private static final double EXIT_OFFSET_Y = -4;
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("caml.reduceMotion");

    private Node currentPage;
    private ParallelTransition running;

    public CamlPageHost(Node initialPage) {
        getStyleClass().add("caml-page-host");
        setPageImmediately(Objects.requireNonNull(initialPage, "initialPage"));
    }

    /** 切换到目标页面；连续快速切换时会先停止上一次动画并回到稳定状态。 */
    public void setPage(Node page) {
        Objects.requireNonNull(page, "page");
        if (REDUCE_MOTION) {
            if (running != null) {
                running.stop();
                running = null;
            }
            setPageImmediately(page);
            return;
        }
        if (page == currentPage && getChildren().size() == 1) {
            return;
        }

        if (running != null) {
            running.stop();
            running = null;
            getChildren().setAll(currentPage);
            reset(currentPage);
        }
        if (page == currentPage) {
            return;
        }

        Node previous = currentPage;
        previous.setMouseTransparent(true);

        page.setOpacity(0);
        page.setTranslateY(ENTER_OFFSET_Y);
        getChildren().add(page);

        FadeTransition fadeOut = new FadeTransition(EXIT_DURATION, previous);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(EXIT_DURATION, previous);
        slideOut.setToY(EXIT_OFFSET_Y);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeIn = new FadeTransition(ENTER_DURATION, page);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setDelay(ENTER_DELAY);

        TranslateTransition slideIn = new TranslateTransition(ENTER_DURATION, page);
        slideIn.setFromY(ENTER_OFFSET_Y);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        slideIn.setDelay(ENTER_DELAY);

        running = new ParallelTransition(
                new ParallelTransition(fadeOut, slideOut),
                new ParallelTransition(fadeIn, slideIn)
        );
        running.setOnFinished(event -> {
            getChildren().remove(previous);
            reset(page);
            currentPage = page;
            running = null;
        });
        running.play();
    }

    private void setPageImmediately(Node page) {
        currentPage = page;
        getChildren().setAll(page);
        reset(page);
    }

    private static void reset(Node page) {
        page.setOpacity(1);
        page.setTranslateY(0);
        page.setMouseTransparent(false);
    }
}
