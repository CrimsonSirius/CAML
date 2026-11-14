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
 * <p>页面内容向上淡出/淡入；可选侧边栏额外向左淡出，并从左侧淡入。
 * 动画只表达页面切换方向，不做装饰性运动。</p>
 */
public final class CamlPageHost extends StackPane {
    private static final Duration EXIT_DURATION = Duration.millis(90);
    private static final Duration ENTER_DURATION = Duration.millis(220);
    private static final Duration ENTER_DELAY = Duration.millis(30);
    private static final double ENTER_OFFSET_Y = -12;
    private static final double EXIT_OFFSET_Y = -4;
    private static final double SIDEBAR_EXIT_OFFSET_X = -16;
    private static final double SIDEBAR_ENTER_OFFSET_X = -16;
    private static final double SIDEBAR_EXIT_OFFSET_Y = 4;
    private static final double SIDEBAR_ENTER_OFFSET_Y = 12;
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("caml.reduceMotion");

    private Node currentPage;
    private Node currentSidebar;
    private ParallelTransition running;

    public CamlPageHost(Node initialPage) {
        getStyleClass().add("caml-page-host");
        setFocusTraversable(true);
        setPageImmediately(Objects.requireNonNull(initialPage, "initialPage"), null);
    }

    /** 切换到目标页面，不播放侧边栏动画。 */
    public void setPage(Node page) {
        setPage(page, null);
    }

    /**
     * 切换到目标页面；侧边栏可为空。
     *
     * @param page    新页面
     * @param sidebar 新页面的侧边栏节点，没有则传 {@code null}
     */
    public void setPage(Node page, Node sidebar) {
        Objects.requireNonNull(page, "page");
        if (REDUCE_MOTION) {
            if (running != null) {
                running.stop();
                running = null;
            }
            setPageImmediately(page, sidebar);
            return;
        }
        if (page == currentPage && sidebar == currentSidebar && getChildren().size() == 1) {
            return;
        }

        if (running != null) {
            running.stop();
            running = null;
            getChildren().setAll(currentPage);
            reset(currentPage);
            resetSidebar(currentSidebar);
        }
        if (page == currentPage && sidebar == currentSidebar) {
            return;
        }

        Node previous = currentPage;
        Node previousSidebar = currentSidebar;
        previous.setMouseTransparent(true);
        requestFocus();

        page.setOpacity(0);
        page.setTranslateY(ENTER_OFFSET_Y);
        if (sidebar != null) {
            sidebar.setOpacity(0);
            sidebar.setTranslateX(SIDEBAR_ENTER_OFFSET_X);
            sidebar.setTranslateY(SIDEBAR_ENTER_OFFSET_Y);
        }
        getChildren().add(page);

        FadeTransition pageFadeOut = new FadeTransition(EXIT_DURATION, previous);
        pageFadeOut.setToValue(0);

        TranslateTransition pageSlideOut = new TranslateTransition(EXIT_DURATION, previous);
        pageSlideOut.setToY(EXIT_OFFSET_Y);
        pageSlideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition pageFadeIn = new FadeTransition(ENTER_DURATION, page);
        pageFadeIn.setFromValue(0);
        pageFadeIn.setToValue(1);
        pageFadeIn.setDelay(ENTER_DELAY);

        TranslateTransition pageSlideIn = new TranslateTransition(ENTER_DURATION, page);
        pageSlideIn.setFromY(ENTER_OFFSET_Y);
        pageSlideIn.setToY(0);
        pageSlideIn.setInterpolator(Interpolator.EASE_OUT);
        pageSlideIn.setDelay(ENTER_DELAY);

        ParallelTransition exit = new ParallelTransition(pageFadeOut, pageSlideOut);
        ParallelTransition enter = new ParallelTransition(pageFadeIn, pageSlideIn);

        if (previousSidebar != null) {
            FadeTransition sidebarFadeOut = new FadeTransition(EXIT_DURATION, previousSidebar);
            sidebarFadeOut.setToValue(0);

            TranslateTransition sidebarSlideOut = new TranslateTransition(EXIT_DURATION, previousSidebar);
            sidebarSlideOut.setToX(SIDEBAR_EXIT_OFFSET_X);
            // 抵消页面整体上移，让侧边栏只向左淡出。
            sidebarSlideOut.setToY(SIDEBAR_EXIT_OFFSET_Y);
            sidebarSlideOut.setInterpolator(Interpolator.EASE_IN);

            exit.getChildren().addAll(sidebarFadeOut, sidebarSlideOut);
        }

        if (sidebar != null) {
            FadeTransition sidebarFadeIn = new FadeTransition(ENTER_DURATION, sidebar);
            sidebarFadeIn.setFromValue(0);
            sidebarFadeIn.setToValue(1);
            sidebarFadeIn.setDelay(ENTER_DELAY);

            TranslateTransition sidebarSlideIn = new TranslateTransition(ENTER_DURATION, sidebar);
            sidebarSlideIn.setFromX(SIDEBAR_ENTER_OFFSET_X);
            sidebarSlideIn.setToX(0);
            // 抵消页面从上方进入，让侧边栏保持垂直位置，只从左侧淡入。
            sidebarSlideIn.setFromY(SIDEBAR_ENTER_OFFSET_Y);
            sidebarSlideIn.setToY(0);
            sidebarSlideIn.setInterpolator(Interpolator.EASE_OUT);
            sidebarSlideIn.setDelay(ENTER_DELAY);

            enter.getChildren().addAll(sidebarFadeIn, sidebarSlideIn);
        }

        running = new ParallelTransition(exit, enter);
        running.setOnFinished(event -> {
            getChildren().remove(previous);
            reset(page);
            resetSidebar(sidebar);
            currentPage = page;
            currentSidebar = sidebar;
            running = null;
            requestFocus();
        });
        running.play();
    }

    private void setPageImmediately(Node page, Node sidebar) {
        currentPage = page;
        currentSidebar = sidebar;
        getChildren().setAll(page);
        reset(page);
        resetSidebar(sidebar);
        requestFocus();
    }

    private static void reset(Node page) {
        page.setOpacity(1);
        page.setTranslateY(0);
        page.setMouseTransparent(false);
    }

    private static void resetSidebar(Node sidebar) {
        if (sidebar != null) {
            sidebar.setOpacity(1);
            sidebar.setTranslateX(0);
            sidebar.setTranslateY(0);
        }
    }
}
