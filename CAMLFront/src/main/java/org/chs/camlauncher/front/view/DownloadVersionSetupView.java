package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.metadata.VersionManifest;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;

/** 选择版本后的下载设置页：版本信息、加载器选择条和原版下载按钮。 */
final class DownloadVersionSetupView extends VBox {
    private final Label statusLabel = new Label("当前仅下载原版游戏，加载器选择暂不生效。");
    private final CamlTextField instanceNameField;

    DownloadVersionSetupView(VersionManifest.Version version, MainController controller,
                             LaunchSettings launchSettings, Runnable onCancel) {
        getStyleClass().add("app-download-setup-page");
        setPadding(new Insets(32, 40, 32, 40));
        setSpacing(12);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton cancel = CamlButton.outlined("取消");
        cancel.getStyleClass().add("app-pill-button");
        cancel.setOnAction(event -> onCancel.run());

        Label nameLabel = new Label("实例名字");
        nameLabel.getStyleClass().add("app-download-option-title");
        instanceNameField = new CamlTextField(version.id());
        instanceNameField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(instanceNameField, Priority.ALWAYS);
        HBox nameRow = new HBox(12, cancel, nameLabel, instanceNameField);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        String date = ViewSupport.releaseDate(version.releaseTime());
        String versionInfo = "Minecraft " + version.id() + " · 正式版"
                + (date.isBlank() ? "" : " · " + date);

        VBox options = new VBox(12,
                optionBar("版本信息", valueLabel(versionInfo)),
                optionBar("选择 Forge 版本", loaderSelector("请选择 Forge 版本")),
                optionBar("选择 Fabric 版本", loaderSelector("请选择 Fabric 版本")),
                optionBar("选择 NeoForge 版本", loaderSelector("请选择 NeoForge 版本"))
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);

        CamlButton download = CamlButton.filled("下载实例");
        download.getStyleClass().add("app-download-primary-button");
        download.setMaxWidth(Double.MAX_VALUE);
        download.setOnAction(event -> startDownload(
                version, controller, launchSettings, cancel, download));

        getChildren().addAll(nameRow, options, spacer, statusLabel, download);
    }

    private void startDownload(VersionManifest.Version version, MainController controller,
                               LaunchSettings launchSettings, CamlButton cancel,
                               CamlButton download) {
        Path gameDirectory = launchSettings.gameDirectory();
        String instanceId = instanceNameField.getText().trim();
        if (instanceId.isEmpty()) {
            showStatus("实例名字不能为空。", true);
            return;
        }
        if (gameDirectory == null) {
            showStatus("尚未设置游戏位置，请先到设置中保存游戏位置。", true);
            return;
        }

        cancel.setDisable(true);
        instanceNameField.setDisable(true);
        download.setDisable(true);
        download.setText("正在下载...");
        showStatus("正在下载 Minecraft " + version.id() + "，实例名：" + instanceId, false);

        controller.downloadVersion(version.id(), instanceId, gameDirectory, () -> {
            launchSettings.setSelectedVersion(instanceId);
            download.setText("下载完成");
            cancel.setDisable(false);
            showStatus("下载完成：实例 " + instanceId + "（Minecraft " + version.id()
                    + "）已安装到 " + gameDirectory, false);
        }, message -> {
            download.setDisable(false);
            download.setText("下载实例");
            cancel.setDisable(false);
            instanceNameField.setDisable(false);
            showStatus("下载失败：" + message, true);
        });
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("field-hint", "field-hint-error");
        statusLabel.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }

    private static HBox optionBar(String title, Node control) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("app-download-option-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(16, titleLabel, spacer, control);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("app-download-option-bar");
        return bar;
    }

    private static Label valueLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("app-download-option-value");
        label.setWrapText(true);
        return label;
    }

    private static CamlComboBox<String> loaderSelector(String prompt) {
        CamlComboBox<String> selector = new CamlComboBox<>();
        selector.setPromptText(prompt);
        selector.setPrefWidth(280);
        selector.setMaxWidth(280);
        return selector;
    }
}
