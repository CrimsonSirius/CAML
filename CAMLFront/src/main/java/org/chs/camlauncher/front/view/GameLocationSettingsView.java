package org.chs.camlauncher.front.view;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** 设置页中的“游戏位置”分组。草稿只有点击保存后才写入共享状态和后端。 */
final class GameLocationSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final MainController controller;
    private final CamlTextField pathField;
    private final Label statusLabel;
    private final CamlButton saveButton;

    GameLocationSettingsView(LaunchSettings launchSettings, MainController controller) {
        this.launchSettings = launchSettings;
        this.controller = controller;

        Label heading = new Label("游戏位置");
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label("设置 Minecraft 游戏数据目录。手动输入后必须点击保存才会生效。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        pathField = new CamlTextField();
        pathField.setMaxWidth(Double.MAX_VALUE);
        pathField.setText(launchSettings.gameDirectory().toString());
        HBox.setHgrow(pathField, Priority.ALWAYS);

        CamlButton browseButton = CamlButton.outlined("浏览");
        browseButton.setMinWidth(64);
        browseButton.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        browseButton.setOnAction(event -> chooseDirectory());

        saveButton = CamlButton.filled("保存");
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
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择游戏位置");
        File current = launchSettings.gameDirectory().toFile();
        if (current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }

        File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) {
            pathField.setText(selected.toPath().toAbsolutePath().normalize().toString());
            showStatus("尚未保存", false);
        }
    }

    private void saveDirectory() {
        String raw = pathField.getText().trim();
        if (raw.isEmpty()) {
            showStatus("游戏位置不能为空", true);
            return;
        }

        Path path;
        try {
            path = Path.of(raw).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            showStatus("游戏位置格式无效", true);
            return;
        }

        saveButton.setDisable(true);
        showStatus("正在检查目录...", false);
        controller.inspectGameDirectory(path, info -> {
            saveButton.setDisable(false);
            if (!info.directoryExists()) {
                showStatus("目录不存在：" + path, true);
                return;
            }
            launchSettings.setGameDirectory(path);
            controller.saveGameDirectoryAsync(path);
            pathField.setText(path.toString());
            showStatus("游戏位置已保存", false);
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
