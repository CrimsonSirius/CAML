package org.chs.camlauncher.front.view;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import org.chs.camlauncher.front.controller.MainController;

/**
 * 主页：左侧启动表单，右侧运行日志。
 *
 * <p>该视图只负责工作区布局和 900px 断点切换；启动表单与日志各自拆分。</p>
 */
final class HomeView extends SplitPane {
    private final LaunchFormView formPane;
    private final LogPaneView logPane;
    private WorkspaceLayout.Plan appliedLayout;

    HomeView(MainController controller, LaunchSettings launchSettings) {
        logPane = new LogPaneView();
        controller.setOnLogReceived(logPane::append);
        formPane = new LaunchFormView(controller, launchSettings, logPane::clear);

        getItems().addAll(formPane, logPane);
        getStyleClass().add("app-workspace");

        // 首帧宽度尚未确定，先按宽屏规则摆放；宽度可用后由 updateWorkspaceLayout 校正。
        applyWorkspaceLayout(WorkspaceLayout.WIDE);
        widthProperty().addListener((observable, oldValue, newValue) ->
                updateWorkspaceLayout(newValue.doubleValue()));
        // 从占位页切回主页或重新挂载场景时，按当前宽度重新确认布局。
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateWorkspaceLayout(getWidth());
            }
        });
    }

    private void updateWorkspaceLayout(double width) {
        if (width <= 0) {
            return;
        }
        applyWorkspaceLayout(WorkspaceLayout.plan(width));
    }

    private void applyWorkspaceLayout(WorkspaceLayout.Plan plan) {
        if (plan.equals(appliedLayout)) {
            return;
        }
        appliedLayout = plan;

        formPane.setMinHeight(plan.formMinHeight());
        logPane.setMinHeight(plan.logMinHeight());
        logPane.logArea().setMinHeight(plan.logAreaMinHeight());
        logPane.logArea().setPrefHeight(plan.logAreaPrefHeight());

        Orientation target = plan.compact() ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        if (getOrientation() != target) {
            setOrientation(target);
        }
        // 只在跨断点切换方案时重置分隔条，用户手动拖动的位置在缩放过程中保持不变。
        setDividerPositions(plan.dividerPosition());
    }
}
