package org.chs.camlauncher.front.view;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.language.Language;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** 设置页中的“游戏位置”分组。草稿只有点击保存后才写入共享状态和后端。 */
final class GameLocationSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final SettingsController controller;
    private final CamlTextField pathField;
    private final Label statusLabel;
    private final CamlButton browseButton;
    private final CamlButton saveButton;

    GameLocationSettingsView(LaunchSettings launchSettings, SettingsController controller) {
        this.launchSettings = launchSettings;
        this.controller = controller;

        Label heading = new Label(Language.tr("settings.game.location.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(Language.tr("settings.game.location.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        pathField = new CamlTextField();
        pathField.setMaxWidth(Double.MAX_VALUE);
        pathField.setText(launchSettings.gameDirectory().toString());
        HBox.setHgrow(pathField, Priority.ALWAYS);

        browseButton = CamlButton.outlined(Language.tr("settings.game.location.browse"));
        browseButton.setMinWidth(64);
        browseButton.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        browseButton.setOnAction(event -> chooseDirectory());

        saveButton = CamlButton.filled(Language.tr("settings.game.location.save"));
        saveButton.setMinWidth(72);
        saveButton.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        saveButton.setOnAction(event -> saveDirectory());

        statusLabel = new Label();
        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        HBox row = new HBox(8, pathField, browseButton, saveButton);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        setSpacing(16);
        setPadding(new javafx.geometry.Insets(24));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-group");
        getChildren().addAll(heading, description, row, statusLabel);

        launchSettings.gameDirectoryProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pathField.setText(newValue.toString());
            }
        });
    }

    private void chooseDirectory() {
        Path current = launchSettings.gameDirectory();
        if (current == null) {
            showDirectoryChooser(null);
            return;
        }

        browseButton.setDisable(true);
        controller.checkDirectory(current,
                exists -> {
                    browseButton.setDisable(false);
                    showDirectoryChooser(exists ? current : null);
                },
                message -> {
                    browseButton.setDisable(false);
                    showDirectoryChooser(null);
                });
    }

    private void showDirectoryChooser(Path initialDirectory) {
        if (getScene() == null) {
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Language.tr("settings.game.location.chooserTitle"));
        if (initialDirectory != null) {
            chooser.setInitialDirectory(initialDirectory.toFile());
        }

        File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) {
            pathField.setText(selected.toPath().toAbsolutePath().normalize().toString());
            showStatus(Language.tr("settings.game.location.notSaved"), false);
        }
    }

    private void saveDirectory() {
        String raw = pathField.getText().trim();
        if (raw.isEmpty()) {
            showStatus(Language.tr("settings.game.location.empty"), true);
            return;
        }

        Path path;
        try {
            path = Path.of(raw).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            showStatus(Language.tr("settings.game.location.invalid"), true);
            return;
        }

        saveButton.setDisable(true);
        showStatus(Language.tr("settings.game.location.checking"), false);
        controller.inspectGameDirectory(path, info -> {
            saveButton.setDisable(false);
            if (!info.directoryExists()) {
                showStatus(Language.tr("settings.game.location.missing", path), true);
                return;
            }
            launchSettings.setGameDirectory(path);
            controller.saveGameDirectoryAsync(path);
            pathField.setText(path.toString());
            showStatus(Language.tr("settings.game.location.saved"), false);
        }, message -> {
            saveButton.setDisable(false);
            showStatus(Language.tr("settings.game.location.checkFailed", message), true);
        });
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.getStyleClass().removeAll("field-hint", "field-hint-error");
        statusLabel.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }
}
