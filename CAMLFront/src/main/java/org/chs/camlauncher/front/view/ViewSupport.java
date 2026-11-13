package org.chs.camlauncher.front.view;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.UiScale;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** 跨页面复用的轻量布局工具。 */
final class ViewSupport {
    private ViewSupport() {
    }

    static Label hintLabel() {
        Label label = new Label();
        label.getStyleClass().addAll("caml-body-small", "field-hint");
        label.setWrapText(true);
        return label;
    }

    static Label fieldLabel(String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("caml-label-medium");
        label.setLabelFor(control);
        return label;
    }

    static VBox buildField(String labelText, Node control, Label helper) {
        Label label = fieldLabel(labelText, control);
        // 间距来自 app.css 的 .app-field（em），随根字号等比缩放。
        VBox field = new VBox(label, control);
        if (helper != null) {
            field.getChildren().add(helper);
        }
        field.getStyleClass().add("app-field");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return field;
    }

    static VBox emptySidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().addAll("app-sidebar", "app-sidebar-empty");
        sidebar.setMaxHeight(Double.MAX_VALUE);
        return sidebar;
    }

    /**
     * 当前场景的 UI 缩放因子。
     *
     * <p>因子由窗口尺寸决定（见 {@link UiScale}），所以只读取场景尺寸，不读取单个视图的
     * 宽度：宽或高任一变化都会改变整体缩放。尚未挂载场景时按设计基准 {@code 1} 处理。</p>
     */
    static double uiFactor(Node node) {
        Scene scene = node.getScene();
        if (scene == null || scene.getWidth() <= 0 || scene.getHeight() <= 0) {
            return UiScale.DEFAULT_FACTOR;
        }
        return UiScale.factor(scene.getWidth(), scene.getHeight());
    }

    /**
     * 在窗口尺寸或组件尺寸变化时执行回调。
     *
     * <p>UI 缩放同时取决于窗口宽高，只监听组件宽度会漏掉“只改高度”的情况，
     * 因此在组件尺寸与场景尺寸上同时挂监听，并在挂载/卸载场景时重新绑定。</p>
     */
    static void onLayoutMetricsChanged(Region node, Runnable action) {
        ChangeListener<Number> listener = (observable, oldValue, newValue) -> action.run();
        node.widthProperty().addListener(listener);
        node.heightProperty().addListener(listener);
        node.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.widthProperty().removeListener(listener);
                oldScene.heightProperty().removeListener(listener);
            }
            if (newScene != null) {
                newScene.widthProperty().addListener(listener);
                newScene.heightProperty().addListener(listener);
            }
            action.run();
        });
    }

    static CamlButton backButton(Runnable onBack) {
        CamlButton button = CamlButton.outlined(UiText.tr("common.back"));
        button.getStyleClass().addAll("app-pill-button", "app-instance-item");
        button.setOnAction(event -> onBack.run());
        return button;
    }

    static String releaseDate(String releaseTime) {
        if (releaseTime == null || releaseTime.isBlank()) {
            return "";
        }
        try {
            return OffsetDateTime.parse(releaseTime).toLocalDate().toString();
        } catch (DateTimeParseException ignored) {
            return releaseTime.length() >= 10 ? releaseTime.substring(0, 10) : releaseTime;
        }
    }
}