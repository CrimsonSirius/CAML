package org.chs.camlauncher.front.view;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.ui.control.CamlPageHost;

/**
 * 主页：启动表单 + 运行日志，以及二级游戏实例选择页。
 */
final class HomeView extends StackPane {
    private final HomeController controller;
    private final LaunchSettings launchSettings;
    private final LaunchFormView formPane;
    private final LogPaneView logPane;
    private final SplitPane workspace;
    private final CamlPageHost pageHost;
    private WorkspaceLayout.Plan appliedLayout;

    HomeView(HomeController controller, LaunchSettings launchSettings) {
        this.controller = controller;
        this.launchSettings = launchSettings;

        logPane = new LogPaneView();
        controller.setOnLogReceived(logPane::append);
        formPane = new LaunchFormView(controller, launchSettings, logPane::clear,
                this::showInstancePicker, this::showManageInstances);

        workspace = new SplitPane(formPane, logPane);
        workspace.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        workspace.getStyleClass().add("app-workspace");
        pageHost = new CamlPageHost(workspace);
        pageHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getChildren().setAll(pageHost);

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

        launchSettings.gameDirectoryProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                formPane.refreshGameDirectory(newValue);
            }
        });
    }

    private void showInstancePicker() {
        InstancePickerView picker = new InstancePickerView(
                controller,
                launchSettings.gameDirectory(),
                version -> {
                    launchSettings.setSelectedVersion(version);
                    showWorkspace();
                },
                this::showWorkspace
        );
        pageHost.setPage(picker);
    }

    private void showManageInstances() {
        pageHost.setPage(new ManageInstancesView(this::showWorkspace));
    }

    private void showWorkspace() {
        pageHost.setPage(workspace);
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
        if (workspace.getOrientation() != target) {
            workspace.setOrientation(target);
        }
        // 只在跨断点切换方案时重置分隔条，用户手动拖动的位置在缩放过程中保持不变。
        workspace.setDividerPositions(plan.dividerPosition());
    }
}
