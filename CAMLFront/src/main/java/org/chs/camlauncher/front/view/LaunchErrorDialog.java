package org.chs.camlauncher.front.view;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.text.UiText;

/** 启动失败对话框；返回 RETRY 表示立即重试。 */
final class LaunchErrorDialog {
    private LaunchErrorDialog() {
    }

    static Dialog<ButtonType> create(String message) {
        ButtonType retry = new ButtonType(
                UiText.tr("common.retry"), ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType(
                UiText.tr("common.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(UiText.tr("home.launchFailedTitle"));
        dialog.setHeaderText(UiText.tr("home.launchFailedTitle"));
        Label detail = new Label(message);
        detail.setWrapText(true);
        detail.setMaxWidth(520);
        dialog.getDialogPane().setContent(new VBox(detail));
        dialog.getDialogPane().getButtonTypes().setAll(retry, close);
        return dialog;
    }
}
