package org.chs.camlauncher.front.view;

import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlTextArea;

/** 主页右侧的运行日志支持面板。 */
final class LogPaneView extends VBox {
    private final CamlTextArea logArea;

    LogPaneView() {
        Label heading = new Label(UiText.tr("log.title"));
        heading.getStyleClass().add("caml-title-large");

        Label subheading = new Label(UiText.tr("log.subtitle"));
        subheading.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

        logArea = new CamlTextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setMaxHeight(Double.MAX_VALUE);
        logArea.setAccessibleText(UiText.tr("log.accessible"));
        logArea.getStyleClass().add("app-log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // 内边距、间距和最小宽度来自 app.css 的 .app-log-pane / .app-log-heading（em），
        // 随根字号等比缩放；高度由 HomeView 按断点设置。
        getStyleClass().add("app-log-pane");
        VBox headingBlock = new VBox(heading, subheading);
        headingBlock.getStyleClass().add("app-log-heading");
        getChildren().addAll(headingBlock, logArea);
    }

    void append(String line) {
        logArea.appendText(line + "\n");
    }

    void clear() {
        logArea.clear();
    }

    CamlTextArea logArea() {
        return logArea;
    }
}