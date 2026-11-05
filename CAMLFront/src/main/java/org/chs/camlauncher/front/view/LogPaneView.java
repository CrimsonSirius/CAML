package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlTextArea;

/** 主页右侧的运行日志支持面板。 */
final class LogPaneView extends VBox {
    private final CamlTextArea logArea;

    LogPaneView() {
        Label heading = new Label("运行日志");
        heading.getStyleClass().add("caml-title-large");

        Label subheading = new Label("下载进度与游戏进程输出");
        subheading.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

        logArea = new CamlTextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setMaxHeight(Double.MAX_VALUE);
        logArea.setAccessibleText("运行日志");
        logArea.getStyleClass().add("app-log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        setSpacing(16);
        setMinWidth(320);
        setMinHeight(220);
        setPadding(new Insets(32));
        getStyleClass().add("app-log-pane");
        getChildren().addAll(new VBox(4, heading, subheading), logArea);
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
