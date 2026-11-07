package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.metadata.VersionManifest;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.List;

/** 选择版本后的下载设置页：实例名、版本信息和加载器选择。 */
final class DownloadVersionSetupView extends VBox {
    private final Label statusLabel = new Label("未选择 Fabric 版本时下载原版游戏。");
    private final CamlTextField instanceNameField;
    private final CamlComboBox<FabricLoaderVersion> fabricLoaderSelector = new CamlComboBox<>();
    private final CamlComboBox<FabricApiVersion> fabricApiSelector = new CamlComboBox<>();
    private boolean downloading;

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

        configureFabricSelectors(version);
        loadFabricVersions(version.id(), controller);

        String date = ViewSupport.releaseDate(version.releaseTime());
        String versionInfo = "Minecraft " + version.id() + " · 正式版"
                + (date.isBlank() ? "" : " · " + date);

        VBox options = new VBox(12,
                optionBar("版本信息", valueLabel(versionInfo)),
                optionBar("选择 Forge 版本", loaderSelector("请选择 Forge 版本")),
                optionBar("选择 Fabric 版本", fabricLoaderSelector),
                optionBar("选择 Fabric API 版本", fabricApiSelector),
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

    private void configureFabricSelectors(VersionManifest.Version version) {
        fabricLoaderSelector.setPrefWidth(280);
        fabricLoaderSelector.setMaxWidth(280);
        fabricLoaderSelector.setPromptText("正在获取 Fabric 版本...");
        fabricLoaderSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(FabricLoaderVersion loader) {
                return loader == null ? "" : loader.version();
            }

            @Override
            public FabricLoaderVersion fromString(String value) {
                return fabricLoaderSelector.getItems().stream()
                        .filter(loader -> loader.version().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        fabricLoaderSelector.valueProperty().addListener((observable, oldValue, loader) -> {
            String currentName = instanceNameField.getText().trim();
            if (loader != null
                    && (currentName.equals(version.id())
                    || (currentName.startsWith("fabric-loader-")
                    && currentName.endsWith("-" + version.id())))) {
                instanceNameField.setText("fabric-loader-" + loader.version() + "-" + version.id());
            }
            updateFabricApiState();
        });

        fabricApiSelector.setPrefWidth(280);
        fabricApiSelector.setMaxWidth(280);
        fabricApiSelector.setPromptText("请先选择 Fabric 版本");
        fabricApiSelector.setDisable(true);
        fabricApiSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(FabricApiVersion api) {
                return api == null ? "" : api.versionNumber();
            }

            @Override
            public FabricApiVersion fromString(String value) {
                return fabricApiSelector.getItems().stream()
                        .filter(api -> api.versionNumber().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void loadFabricVersions(String gameVersion, MainController controller) {
        controller.fetchFabricLoaderVersions(gameVersion, this::showFabricLoaders,
                message -> fabricLoaderSelector.setPromptText("获取 Fabric 版本失败"));
        controller.fetchFabricApiVersions(gameVersion, this::showFabricApiVersions,
                message -> fabricApiSelector.setPromptText("获取 Fabric API 版本失败"));
    }

    private void showFabricLoaders(List<FabricLoaderVersion> loaders) {
        fabricLoaderSelector.getItems().setAll(loaders);
        fabricLoaderSelector.setPromptText(loaders.isEmpty()
                ? "没有可用的 Fabric 版本"
                : "请选择 Fabric 版本");
    }

    private void showFabricApiVersions(List<FabricApiVersion> versions) {
        fabricApiSelector.getItems().setAll(versions);
        fabricApiSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 Fabric API 版本"
                : "请选择 Fabric API 版本");
        updateFabricApiState();
    }

    private void updateFabricApiState() {
        boolean hasLoader = fabricLoaderSelector.getValue() != null;
        if (!hasLoader) {
            fabricApiSelector.setValue(null);
        }
        fabricApiSelector.setDisable(
                downloading || !hasLoader || fabricApiSelector.getItems().isEmpty());
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

        FabricLoaderVersion fabricLoader = fabricLoaderSelector.getValue();
        FabricApiVersion fabricApi = fabricLoader == null ? null : fabricApiSelector.getValue();
        if (fabricLoader != null && instanceId.equals(version.id())) {
            showStatus("安装 Fabric 时实例名不能与游戏版本号相同。", true);
            return;
        }

        cancel.setDisable(true);
        downloading = true;
        instanceNameField.setDisable(true);
        fabricLoaderSelector.setDisable(true);
        fabricApiSelector.setDisable(true);
        download.setDisable(true);
        download.setText("正在下载...");

        if (fabricLoader == null) {
            showStatus("正在下载 Minecraft " + version.id() + "，实例名：" + instanceId, false);
            controller.downloadVersion(version.id(), instanceId, gameDirectory,
                    () -> completeDownload(launchSettings, instanceId, gameDirectory, cancel, download,
                            "下载完成：实例 " + instanceId + "（Minecraft " + version.id() + "）已安装到 " + gameDirectory),
                    message -> failDownload(message, cancel, download));
            return;
        }

        String apiText = fabricApi == null ? "" : "，Fabric API：" + fabricApi.versionNumber();
        showStatus("正在安装 Fabric Loader " + fabricLoader.version()
                + apiText + "，实例名：" + instanceId, false);
        controller.downloadFabricVersion(
                version.id(),
                instanceId,
                fabricLoader.version(),
                fabricApi,
                gameDirectory,
                launchSettings.fullVersionIsolation(),
                () -> completeDownload(launchSettings, instanceId, gameDirectory, cancel, download,
                        "下载完成：实例 " + instanceId + " 已安装 Fabric Loader "
                                + fabricLoader.version() + apiText),
                message -> failDownload(message, cancel, download)
        );
    }

    private void completeDownload(LaunchSettings launchSettings, String instanceId,
                                  Path gameDirectory, CamlButton cancel,
                                  CamlButton download, String message) {
        launchSettings.setSelectedVersion(instanceId);
        downloading = false;
        download.setText("下载完成");
        cancel.setDisable(false);
        showStatus(message + "（游戏位置：" + gameDirectory + "）", false);
    }

    private void failDownload(String message, CamlButton cancel, CamlButton download) {
        download.setDisable(false);
        downloading = false;
        download.setText("下载实例");
        cancel.setDisable(false);
        instanceNameField.setDisable(false);
        fabricLoaderSelector.setDisable(false);
        updateFabricApiState();
        showStatus("下载失败：" + message, true);
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
