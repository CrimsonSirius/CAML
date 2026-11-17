package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTextArea;

/** 设置页中的“额外 JVM 参数”分组。 */
final class JvmArgumentsSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final SettingsController controller;
    private final CamlTextArea argumentsArea;
    private final CamlButton saveButton;
    private final Label statusLabel;

    JvmArgumentsSettingsView(LaunchSettings launchSettings, SettingsController controller) {
        this.launchSettings = launchSettings;
        this.controller = controller;

        Label heading = new Label(UiText.tr("settings.game.jvm.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("settings.game.jvm.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label argumentsLabel = new Label(UiText.tr("settings.game.jvm.label"));
        argumentsLabel.getStyleClass().add("caml-label-medium");

        argumentsArea = new CamlTextArea(launchSettings.jvmArguments());
        argumentsArea.setPromptText(UiText.tr("settings.game.jvm.placeholder"));
        argumentsArea.setWrapText(true);
        argumentsArea.setPrefRowCount(4);
        argumentsArea.setMaxWidth(Double.MAX_VALUE);
        argumentsLabel.setLabelFor(argumentsArea);

        Label hint = new Label(UiText.tr("settings.game.jvm.hint"));
        hint.getStyleClass().addAll("caml-body-small", "field-hint");
        hint.setWrapText(true);

        saveButton = CamlButton.filled(UiText.tr("settings.game.jvm.save"));
        saveButton.getStyleClass().add("app-save-button");
        saveButton.setOnAction(event -> save());

        statusLabel = new Label();
        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        HBox actions = new HBox(saveButton);
        actions.getStyleClass().add("app-settings-field-row");
        actions.setAlignment(Pos.CENTER_RIGHT);

        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-group");
        getChildren().addAll(heading, description, argumentsLabel, argumentsArea, hint, actions, statusLabel);
    }

    private void save() {
        saveButton.setDisable(true);
        controller.saveJvmArgumentsAsync(argumentsArea.getText(),
                saved -> {
                    launchSettings.setJvmArguments(saved);
                    argumentsArea.setText(saved);
                    saveButton.setDisable(false);
                    showStatus(UiText.tr("settings.game.jvm.saved"), false);
                },
                message -> {
                    saveButton.setDisable(false);
                    showStatus(UiText.tr("settings.game.jvm.saveFailed", message), true);
                });
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("field-hint", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "field-hint");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
