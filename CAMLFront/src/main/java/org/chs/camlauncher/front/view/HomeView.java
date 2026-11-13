package org.chs.camlauncher.front.view;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.ui.control.CamlPageHost;

/**
 * 主页：启动表单 + 运行日志，以及二级游戏实例选择页。
 */
final class HomeView extends StackPane implements PageLifecycle {
    private final HomeController controller;
    private final LaunchSettings launchSettings;
    private final LaunchFormView formPane;
    private final LogPaneView logPane;
    private final SplitPane workspace;
    private final CamlPageHost pageHost;
    private WorkspaceLayout.Plan appliedLayout;
    private PageLifecycle activeSubPage;
    private boolean disposed;

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

        // 首帧宽度与缩放尚未确定，先按宽屏规则摆放；尺寸可用后由 updateWorkspaceLayout 校正。
        applyWorkspaceLayout(WorkspaceLayout.WIDE);
        // 缩放因子同时取决于窗口宽高，因此监听组件尺寸与场景尺寸，而不是只监听宽度。
        ViewSupport.onLayoutMetricsChanged(this, this::updateWorkspaceLayout);

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
        setSubPage(picker);
    }

    private void showManageInstances() {
        setSubPage(new ManageInstancesView(this::showWorkspace));
    }

    private void showWorkspace() {
        setSubPage(workspace);
    }

    private void setSubPage(Node next) {
        if (disposed) {
            return;
        }
        PageLifecycle previous = activeSubPage;
        if (previous != null) {
            previous.onHide();
        }
        pageHost.setPage(next);
        if (previous != null) {
            previous.dispose();
        }
        activeSubPage = next instanceof PageLifecycle lifecycle ? lifecycle : null;
        if (activeSubPage != null) {
            activeSubPage.onShow();
        }
    }

    @Override
    public void onShow() {
        if (disposed) {
            return;
        }
        if (activeSubPage != null) {
            activeSubPage.onShow();
        }
    }

    @Override
    public void onHide() {
        if (disposed) {
            return;
        }
        if (activeSubPage != null) {
            activeSubPage.onHide();
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        if (activeSubPage != null) {
            activeSubPage.onHide();
            activeSubPage.dispose();
            activeSubPage = null;
        }
    }

    private void updateWorkspaceLayout() {
        double width = getWidth();
        if (width <= 0) {
            return;
        }
        applyWorkspaceLayout(WorkspaceLayout.plan(width, ViewSupport.uiFactor(this)));
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