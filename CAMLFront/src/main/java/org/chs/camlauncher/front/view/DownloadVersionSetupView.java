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
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.List;

/** 选择版本后的下载设置页：实例名、版本信息和加载器选择。 */
final class DownloadVersionSetupView extends VBox {
    private final Label statusLabel = new Label("未选择加载器时下载原版游戏。");
    private final CamlTextField instanceNameField;
    private final CamlComboBox<FabricLoaderVersion> fabricLoaderSelector = new CamlComboBox<>();
    private final CamlComboBox<FabricApiVersion> fabricApiSelector = new CamlComboBox<>();
    private final CamlComboBox<ForgeVersion> forgeSelector = new CamlComboBox<>();
    private final CamlComboBox<NeoForgeVersion> neoForgeSelector = new CamlComboBox<>();
    private final String gameVersion;
    private boolean downloading;
    private boolean updatingLoaders;
    private String lastAutoName;

    DownloadVersionSetupView(VersionManifest.Version version, DownloadsController controller,
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

        gameVersion = version.id();
        lastAutoName = gameVersion;
        configureSelectors(version);
        loadLoaderVersions(version.id(), controller);

        String date = ViewSupport.releaseDate(version.releaseTime());
        String versionInfo = "Minecraft " + version.id() + " · 正式版"
                + (date.isBlank() ? "" : " · " + date);

        VBox options = new VBox(12,
                optionBar("版本信息", valueLabel(versionInfo)),
                optionBar("选择 Forge 版本", forgeSelector),
                optionBar("选择 Fabric 版本", fabricLoaderSelector),
                optionBar("选择 Fabric API 版本", fabricApiSelector),
                optionBar("选择 NeoForge 版本", neoForgeSelector)
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

    private void configureSelectors(VersionManifest.Version version) {
        configureFabricSelectors();
        configureForgeSelector(version);
        configureNeoForgeSelector(version);
    }

    private void configureFabricSelectors() {
        fabricLoaderSelector.setPrefWidth(280);
        fabricLoaderSelector.setMaxWidth(280);
        fabricLoaderSelector.setPromptText("正在获取 Fabric 版本...");
        fabricLoaderSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(FabricLoaderVersion loader) {
                return loader == null ? null : loader.version();
            }

            @Override
            public FabricLoaderVersion fromString(String value) {
                return findByLabel(fabricLoaderSelector.getItems(), value, FabricLoaderVersion::version);
            }
        });
        fabricLoaderSelector.valueProperty().addListener((observable, oldValue, loader) -> {
            if (updatingLoaders) {
                return;
            }
            if (loader == null) {
                updateFabricApiState();
                return;
            }
            clearOtherLoaders(fabricLoaderSelector);
            updateFabricApiState();
            autoUpdateInstanceName("fabric-loader-" + loader.version() + "-" + gameVersion);
        });

        fabricApiSelector.setPrefWidth(280);
        fabricApiSelector.setMaxWidth(280);
        fabricApiSelector.setPromptText("请先选择 Fabric 版本");
        fabricApiSelector.setDisable(true);
        fabricApiSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(FabricApiVersion api) {
                return api == null ? null : api.versionNumber();
            }

            @Override
            public FabricApiVersion fromString(String value) {
                return findByLabel(fabricApiSelector.getItems(), value, FabricApiVersion::versionNumber);
            }
        });
    }

    private void configureForgeSelector(VersionManifest.Version version) {
        forgeSelector.setPrefWidth(280);
        forgeSelector.setMaxWidth(280);
        forgeSelector.setPromptText("正在获取 Forge 版本...");
        forgeSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ForgeVersion forge) {
                return forge == null ? null : forge.version();
            }

            @Override
            public ForgeVersion fromString(String value) {
                return findByLabel(forgeSelector.getItems(), value, ForgeVersion::version);
            }
        });
        forgeSelector.valueProperty().addListener((observable, oldValue, forge) -> {
            if (updatingLoaders || forge == null) {
                return;
            }
            updatingLoaders = true;
            clearSelection(fabricLoaderSelector, "请选择 Fabric 版本");
            clearSelection(fabricApiSelector, "请先选择 Fabric 版本");
            clearSelection(neoForgeSelector, "请选择 NeoForge 版本");
            updatingLoaders = false;
            updateFabricApiState();
            autoUpdateInstanceName(forge.versionId());
        });
    }

    private void configureNeoForgeSelector(VersionManifest.Version version) {
        neoForgeSelector.setPrefWidth(280);
        neoForgeSelector.setMaxWidth(280);
        neoForgeSelector.setPromptText("正在获取 NeoForge 版本...");
        neoForgeSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(NeoForgeVersion neoForge) {
                return neoForge == null ? null : neoForge.version();
            }

            @Override
            public NeoForgeVersion fromString(String value) {
                return findByLabel(neoForgeSelector.getItems(), value, NeoForgeVersion::version);
            }
        });
        neoForgeSelector.valueProperty().addListener((observable, oldValue, neoForge) -> {
            if (updatingLoaders || neoForge == null) {
                return;
            }
            updatingLoaders = true;
            clearSelection(forgeSelector, "请选择 Forge 版本");
            clearSelection(fabricLoaderSelector, "请选择 Fabric 版本");
            clearSelection(fabricApiSelector, "请先选择 Fabric 版本");
            updatingLoaders = false;
            updateFabricApiState();
            autoUpdateInstanceName(neoForge.versionId());
        });
    }

    private void autoUpdateInstanceName(String newName) {
        if (instanceNameField.getText().trim().equals(lastAutoName)) {
            instanceNameField.setText(newName);
            lastAutoName = newName;
        }
    }

    private void clearOtherLoaders(CamlComboBox<?> selected) {
        updatingLoaders = true;
        if (selected != forgeSelector) {
            clearSelection(forgeSelector, "请选择 Forge 版本");
        }
        if (selected != neoForgeSelector) {
            clearSelection(neoForgeSelector, "请选择 NeoForge 版本");
        }
        if (selected != fabricLoaderSelector) {
            clearSelection(fabricLoaderSelector, "请选择 Fabric 版本");
            clearSelection(fabricApiSelector, "请先选择 Fabric 版本");
        }
        updatingLoaders = false;
    }

    private static void clearSelection(CamlComboBox<?> selector, String prompt) {
        selector.getSelectionModel().clearSelection();
        selector.setValue(null);

        String targetPrompt = prompt;
        String currentPrompt = selector.getPromptText();
        if (selector.getItems().isEmpty() && currentPrompt != null && !currentPrompt.isBlank()) {
            targetPrompt = currentPrompt;
        }
        // JavaFX 在 promptText 未变化时不会刷新按钮单元格；先清空再设置可强制恢复占位文字。
        selector.setPromptText(null);
        selector.setPromptText(targetPrompt);
        selector.refreshDisplay();
    }

    private static <T> T findByLabel(List<T> items, String value,
                                     java.util.function.Function<T, String> label) {
        return items.stream()
                .filter(item -> label.apply(item).equals(value))
                .findFirst()
                .orElse(null);
    }

    private void loadLoaderVersions(String gameVersion, DownloadsController controller) {
        controller.fetchFabricLoaderVersions(gameVersion, this::showFabricLoaders,
                message -> fabricLoaderSelector.setPromptText("获取 Fabric 版本失败"));
        controller.fetchFabricApiVersions(gameVersion, this::showFabricApiVersions,
                message -> fabricApiSelector.setPromptText("获取 Fabric API 版本失败"));
        controller.fetchForgeVersions(gameVersion, this::showForgeVersions,
                message -> forgeSelector.setPromptText("获取 Forge 版本失败"));
        controller.fetchNeoForgeVersions(gameVersion, this::showNeoForgeVersions,
                message -> neoForgeSelector.setPromptText("获取 NeoForge 版本失败"));
    }

    private void showFabricLoaders(List<FabricLoaderVersion> loaders) {
        fabricLoaderSelector.getItems().setAll(loaders);
        fabricLoaderSelector.setPromptText(loaders.isEmpty()
                ? "没有可用的 Fabric 版本"
                : "请选择 Fabric 版本");
        updateFabricApiState();
    }

    private void showFabricApiVersions(List<FabricApiVersion> versions) {
        fabricApiSelector.getItems().setAll(versions);
        fabricApiSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 Fabric API 版本"
                : "请选择 Fabric API 版本");
        updateFabricApiState();
    }

    private void showForgeVersions(List<ForgeVersion> versions) {
        forgeSelector.getItems().setAll(versions);
        forgeSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 Forge 版本"
                : "请选择 Forge 版本");
    }

    private void showNeoForgeVersions(List<NeoForgeVersion> versions) {
        neoForgeSelector.getItems().setAll(versions);
        neoForgeSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 NeoForge 版本"
                : "请选择 NeoForge 版本");
    }

    private void updateFabricApiState() {
        boolean hasFabric = fabricLoaderSelector.getValue() != null
                && forgeSelector.getValue() == null
                && neoForgeSelector.getValue() == null;
        if (!hasFabric) {
            clearSelection(fabricApiSelector, "请先选择 Fabric 版本");
        } else if (fabricApiSelector.getValue() == null && !fabricApiSelector.getItems().isEmpty()) {
            fabricApiSelector.setPromptText("请选择 Fabric API 版本");
        }
        fabricApiSelector.setDisable(
                downloading || !hasFabric || fabricApiSelector.getItems().isEmpty());
    }

    private void startDownload(VersionManifest.Version version, DownloadsController controller,
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

        ForgeVersion forge = forgeSelector.getValue();
        FabricLoaderVersion fabricLoader = fabricLoaderSelector.getValue();
        FabricApiVersion fabricApi = fabricLoader == null ? null : fabricApiSelector.getValue();
        NeoForgeVersion neoForge = neoForgeSelector.getValue();
        boolean loaderSelected = forge != null || fabricLoader != null || neoForge != null;
        if (loaderSelected && instanceId.equals(version.id())) {
            showStatus("安装加载器时实例名不能与游戏版本号相同。", true);
            return;
        }

        cancel.setDisable(true);
        downloading = true;
        instanceNameField.setDisable(true);
        forgeSelector.setDisable(true);
        fabricLoaderSelector.setDisable(true);
        fabricApiSelector.setDisable(true);
        neoForgeSelector.setDisable(true);
        download.setDisable(true);
        download.setText("正在下载...");

        if (forge != null) {
            showStatus("正在安装 Forge " + forge.version() + "，实例名：" + instanceId, false);
            controller.downloadForgeVersion(version.id(), instanceId, forge, gameDirectory,
                    () -> completeDownload(launchSettings, instanceId, gameDirectory, cancel, download,
                            "下载完成：实例 " + instanceId + " 已安装 Forge " + forge.version()),
                    message -> failDownload(message, cancel, download));
        } else if (fabricLoader != null) {
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
        } else if (neoForge != null) {
            showStatus("正在安装 NeoForge " + neoForge.version()
                    + "，实例名：" + instanceId, false);
            controller.downloadNeoForgeVersion(version.id(), instanceId, neoForge, gameDirectory,
                    () -> completeDownload(launchSettings, instanceId, gameDirectory, cancel, download,
                            "下载完成：实例 " + instanceId + " 已安装 NeoForge " + neoForge.version()),
                    message -> failDownload(message, cancel, download));
        } else {
            showStatus("正在下载 Minecraft " + version.id() + "，实例名：" + instanceId, false);
            controller.downloadVersion(version.id(), instanceId, gameDirectory,
                    () -> completeDownload(launchSettings, instanceId, gameDirectory, cancel, download,
                            "下载完成：实例 " + instanceId + "（Minecraft " + version.id()
                                    + "）已安装到 " + gameDirectory),
                    message -> failDownload(message, cancel, download));
        }
    }

    private void completeDownload(LaunchSettings launchSettings, String instanceId,
                                  Path gameDirectory, CamlButton cancel,
                                  CamlButton download, String message) {
        launchSettings.setSelectedVersion(instanceId);
        download.setText("下载完成");
        cancel.setDisable(false);
        showStatus(message + "（游戏位置：" + gameDirectory + "）", false);
    }

    private void failDownload(String message, CamlButton cancel, CamlButton download) {
        downloading = false;
        download.setDisable(false);
        download.setText("下载实例");
        cancel.setDisable(false);
        instanceNameField.setDisable(false);
        forgeSelector.setDisable(false);
        fabricLoaderSelector.setDisable(false);
        neoForgeSelector.setDisable(false);
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
}
