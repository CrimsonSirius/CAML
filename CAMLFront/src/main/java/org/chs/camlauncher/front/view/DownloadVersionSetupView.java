package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.loader.fabric.FabricApiVersion;
import org.chs.camlauncher.backend.loader.fabric.FabricLoaderVersion;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.loader.neoforge.NeoForgeVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineVersion;
import org.chs.camlauncher.backend.loader.optifine.OptiFineCompatibility;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 选择版本后的下载设置页：实例名、版本信息和加载器选择。 */
final class DownloadVersionSetupView extends VBox implements PageLifecycle {
    private final Label statusLabel = new Label("未选择加载器时下载原版游戏。");
    private final CamlTextField instanceNameField;
    private final Label instanceNameHint = new Label();
    private final CamlButton downloadButton;
    private final PauseTransition instanceNameDebounce = new PauseTransition(Duration.millis(500));
    private final CamlComboBox<FabricLoaderVersion> fabricLoaderSelector = new CamlComboBox<>();
    private final CamlComboBox<FabricApiVersion> fabricApiSelector = new CamlComboBox<>();
    private final CamlComboBox<ForgeVersion> forgeSelector = new CamlComboBox<>();
    private final CamlComboBox<OptiFineVersion> optiFineSelector = new CamlComboBox<>();
    private final CamlComboBox<NeoForgeVersion> neoForgeSelector = new CamlComboBox<>();
    private final VersionManifest.Version version;
    private final String gameVersion;
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final Consumer<DownloadRequest> onStartDownload;
    private boolean updatingLoaders;
    private String lastAutoName;
    private String availableInstanceName = "";
    private int instanceNameCheckGeneration;
    private final List<FrontTask<?>> loaderTasks = new ArrayList<>();
    private FrontTask<?> instanceNameTask;
    private boolean disposed;

    DownloadVersionSetupView(VersionManifest.Version version, DownloadsController controller,
                             LaunchSettings launchSettings, Runnable onCancel,
                             Consumer<DownloadRequest> onStartDownload) {
        this.version = version;
        this.controller = controller;
        this.launchSettings = launchSettings;
        this.onStartDownload = onStartDownload;
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
        instanceNameHint.getStyleClass().addAll("caml-body-small", "field-hint");
        instanceNameHint.setWrapText(true);
        instanceNameHint.setVisible(false);
        instanceNameHint.setManaged(false);
        VBox nameInput = new VBox(6, instanceNameField, instanceNameHint);
        HBox.setHgrow(nameInput, Priority.ALWAYS);
        HBox nameRow = new HBox(12, cancel, nameLabel, nameInput);
        nameRow.setAlignment(Pos.TOP_LEFT);

        gameVersion = version.id();
        lastAutoName = gameVersion;
        configureSelectors(version);
        loadLoaderVersions(version.id(), controller);

        String date = ViewSupport.releaseDate(version.releaseTime());
        String versionInfo = "Minecraft " + version.id() + " · "
                + VersionCategory.labelOfType(version.type())
                + (date.isBlank() ? "" : " · " + date);

        VBox options = new VBox(12,
                optionBar("版本信息", valueLabel(versionInfo)),
                optionBar("选择 Forge 版本", forgeSelector),
                optionBar("选择 OptiFine 版本", optiFineSelector),
                optionBar("选择 Fabric 版本", fabricLoaderSelector),
                optionBar("选择 Fabric API 版本", fabricApiSelector),
                optionBar("选择 NeoForge 版本", neoForgeSelector)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);

        downloadButton = CamlButton.filled("下载实例");
        downloadButton.getStyleClass().add("app-download-primary-button");
        downloadButton.setMaxWidth(Double.MAX_VALUE);
        downloadButton.setDisable(true);
        downloadButton.setOnAction(event -> startDownload());

        getChildren().addAll(nameRow, options, spacer, statusLabel, downloadButton);

        instanceNameDebounce.setOnFinished(event -> checkInstanceNameNow());
        instanceNameField.textProperty().addListener((observable, oldValue, newValue) ->
                scheduleInstanceNameCheck());
        launchSettings.gameDirectoryProperty().addListener((observable, oldValue, newValue) ->
                scheduleInstanceNameCheck());
        scheduleInstanceNameCheck();
    }

    private void configureSelectors(VersionManifest.Version version) {
        configureFabricSelectors();
        configureForgeSelector(version);
        configureOptiFineSelector();
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
            scheduleInstanceNameCheck();
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
            scheduleInstanceNameCheck();
            OptiFineVersion optiFine = optiFineSelector.getValue();
            if (optiFine != null && !OptiFineCompatibility.isForgeCompatible(forge, optiFine)) {
                clearSelection(optiFineSelector, "当前 Forge 与 OptiFine 不兼容");
                updateOptiFineState();
                autoUpdateInstanceName(forge.versionId());
                showStatus(OptiFineCompatibility.incompatibleMessage(forge, optiFine), true);
                return;
            }
            updatingLoaders = true;
            clearSelection(fabricLoaderSelector, "请选择 Fabric 版本");
            clearSelection(fabricApiSelector, "请先选择 Fabric 版本");
            clearSelection(neoForgeSelector, "请选择 NeoForge 版本");
            updatingLoaders = false;
            updateFabricApiState();
            updateOptiFineState();
            autoUpdateInstanceName(forge.versionId()
                    + (optiFine == null ? "" : "-optifine-" + optiFine.version()));
        });
    }

    private void configureOptiFineSelector() {
        optiFineSelector.setPrefWidth(280);
        optiFineSelector.setMaxWidth(280);
        optiFineSelector.setPromptText("请先选择 Forge 版本");
        optiFineSelector.setDisable(true);
        optiFineSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(OptiFineVersion optiFine) {
                return optiFine == null ? null : optiFine.version();
            }

            @Override
            public OptiFineVersion fromString(String value) {
                return findByLabel(optiFineSelector.getItems(), value, OptiFineVersion::version);
            }
        });
        optiFineSelector.valueProperty().addListener((observable, oldValue, optiFine) -> {
            if (updatingLoaders || optiFine == null) {
                return;
            }
            ForgeVersion forge = forgeSelector.getValue();
            if (forge == null) {
                clearSelection(optiFineSelector, "请先选择 Forge 版本");
                return;
            }
            if (!OptiFineCompatibility.isForgeCompatible(forge, optiFine)) {
                ForgeVersion compatible = forgeSelector.getItems().stream()
                        .filter(candidate -> OptiFineCompatibility.isForgeCompatible(candidate, optiFine))
                        .findFirst()
                        .orElse(null);
                if (compatible == null) {
                    clearSelection(optiFineSelector, "没有兼容当前 Forge 的 OptiFine 版本");
                    showStatus(OptiFineCompatibility.incompatibleMessage(forge, optiFine), true);
                    return;
                }
                updatingLoaders = true;
                forgeSelector.setValue(compatible);
                updatingLoaders = false;
                forge = compatible;
                showStatus("Forge 与 OptiFine 不兼容，已自动切换 Forge " + compatible.version(), false);
            }
            autoUpdateInstanceName(forge.versionId() + "-optifine-" + optiFine.version());
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
            scheduleInstanceNameCheck();
            updatingLoaders = true;
            clearSelection(forgeSelector, "请选择 Forge 版本");
            clearSelection(fabricLoaderSelector, "请选择 Fabric 版本");
            clearSelection(fabricApiSelector, "请先选择 Fabric 版本");
            clearSelection(optiFineSelector, "请先选择 Forge 版本");
            updatingLoaders = false;
            updateFabricApiState();
            updateOptiFineState();
            autoUpdateInstanceName(neoForge.versionId());
        });
    }

    private void autoUpdateInstanceName(String newName) {
        if (instanceNameField.getText().trim().equals(lastAutoName)) {
            instanceNameField.setText(newName);
            lastAutoName = newName;
        }
    }

    private void scheduleInstanceNameCheck() {
        instanceNameCheckGeneration++;
        availableInstanceName = "";
        downloadButton.setDisable(true);
        String name = instanceNameField.getText().trim();
        if (name.isEmpty()) {
            instanceNameDebounce.stop();
            showInstanceNameHint("实例名字不能为空", true);
            return;
        }

        showInstanceNameHint("", false);
        instanceNameDebounce.playFromStart();
    }

    private void checkInstanceNameNow() {
        String name = instanceNameField.getText().trim();
        Path gameDirectory = launchSettings.gameDirectory();
        int generation = instanceNameCheckGeneration;
        if (name.isEmpty()) {
            showInstanceNameHint("实例名字不能为空", true);
            return;
        }
        if (gameDirectory == null) {
            showInstanceNameHint("尚未设置游戏位置", true);
            return;
        }

        showInstanceNameHint("正在检查实例名...", false);
        if (instanceNameTask != null) {
            instanceNameTask.cancel();
        }
        instanceNameTask = controller.checkInstanceName(gameDirectory, name, check -> {
            if (disposed) {
                return;
            }
            if (generation != instanceNameCheckGeneration
                    || !name.equals(instanceNameField.getText().trim())) {
                return;
            }
            if (!check.available()) {
                showInstanceNameHint(check.message(), true);
                return;
            }
            if (hasLoaderSelected() && name.equals(gameVersion)) {
                showInstanceNameHint("安装加载器时实例名不能与游戏版本号相同", true);
                return;
            }
            availableInstanceName = name;
            downloadButton.setDisable(false);
            showInstanceNameHint("实例名可用", false);
        }, message -> {
            if (generation != instanceNameCheckGeneration
                    || !name.equals(instanceNameField.getText().trim())) {
                return;
            }
            showInstanceNameHint("检查实例名失败：" + message, true);
        });
    }

    private void showInstanceNameHint(String text, boolean error) {
        instanceNameHint.setText(text == null ? "" : text);
        boolean visible = text != null && !text.isBlank();
        instanceNameHint.setVisible(visible);
        instanceNameHint.setManaged(visible);
        instanceNameHint.getStyleClass().removeAll("field-hint", "field-hint-error");
        instanceNameHint.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }

    private boolean hasLoaderSelected() {
        return forgeSelector.getValue() != null
                || fabricLoaderSelector.getValue() != null
                || neoForgeSelector.getValue() != null;
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
        if (selected != forgeSelector) {
            clearSelection(optiFineSelector, "请先选择 Forge 版本");
        }
        updatingLoaders = false;
        updateOptiFineState();
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
        loaderTasks.add(controller.fetchFabricLoaderVersions(gameVersion, this::showFabricLoaders,
                message -> fabricLoaderSelector.setPromptText("获取 Fabric 版本失败")));
        loaderTasks.add(controller.fetchFabricApiVersions(gameVersion, this::showFabricApiVersions,
                message -> fabricApiSelector.setPromptText("获取 Fabric API 版本失败")));
        loaderTasks.add(controller.fetchForgeVersions(gameVersion, this::showForgeVersions,
                message -> forgeSelector.setPromptText("获取 Forge 版本失败")));
        loaderTasks.add(controller.fetchNeoForgeVersions(gameVersion, this::showNeoForgeVersions,
                message -> neoForgeSelector.setPromptText("获取 NeoForge 版本失败")));
        loaderTasks.add(controller.fetchOptiFineVersions(gameVersion, this::showOptiFineVersions,
                message -> {
                    optiFineSelector.setPromptText("获取 OptiFine 版本失败");
                    optiFineSelector.setDisable(true);
                }));
    }

    private void showFabricLoaders(List<FabricLoaderVersion> loaders) {
        if (disposed) {
            return;
        }
        fabricLoaderSelector.getItems().setAll(loaders);
        fabricLoaderSelector.setPromptText(loaders.isEmpty()
                ? "没有可用的 Fabric 版本"
                : "请选择 Fabric 版本");
        updateFabricApiState();
    }

    private void showFabricApiVersions(List<FabricApiVersion> versions) {
        if (disposed) {
            return;
        }
        fabricApiSelector.getItems().setAll(versions);
        fabricApiSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 Fabric API 版本"
                : "请选择 Fabric API 版本");
        updateFabricApiState();
    }

    private void showForgeVersions(List<ForgeVersion> versions) {
        if (disposed) {
            return;
        }
        forgeSelector.getItems().setAll(versions);
        forgeSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 Forge 版本"
                : "请选择 Forge 版本");
    }

    private void showNeoForgeVersions(List<NeoForgeVersion> versions) {
        if (disposed) {
            return;
        }
        neoForgeSelector.getItems().setAll(versions);
        neoForgeSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 NeoForge 版本"
                : "请选择 NeoForge 版本");
    }

    private void showOptiFineVersions(List<OptiFineVersion> versions) {
        if (disposed) {
            return;
        }
        optiFineSelector.getItems().setAll(versions);
        optiFineSelector.setPromptText(versions.isEmpty()
                ? "没有可用的 OptiFine 版本"
                : "请先选择 Forge 版本");
        updateOptiFineState();
    }

    @Override
    public void dispose() {
        disposed = true;
        instanceNameDebounce.stop();
        if (instanceNameTask != null) {
            instanceNameTask.cancel();
            instanceNameTask = null;
        }
        for (FrontTask<?> task : loaderTasks) {
            task.cancel();
        }
        loaderTasks.clear();
    }

    private void updateOptiFineState() {
        boolean hasForge = forgeSelector.getValue() != null
                && fabricLoaderSelector.getValue() == null
                && neoForgeSelector.getValue() == null;
        if (!hasForge) {
            clearSelection(optiFineSelector, "请先选择 Forge 版本");
            optiFineSelector.setDisable(true);
            return;
        }

        optiFineSelector.setDisable(optiFineSelector.getItems().isEmpty());
        if (!optiFineSelector.getItems().isEmpty() && optiFineSelector.getValue() == null) {
            optiFineSelector.setPromptText("请选择 OptiFine 版本");
        }
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
        fabricApiSelector.setDisable(!hasFabric || fabricApiSelector.getItems().isEmpty());
    }

    private void startDownload() {
        Path gameDirectory = launchSettings.gameDirectory();
        String instanceId = instanceNameField.getText().trim();
        if (!instanceId.equals(availableInstanceName)) {
            showStatus("实例名尚未通过检查，请稍候再试。", true);
            return;
        }
        if (gameDirectory == null) {
            showStatus("尚未设置游戏位置，请先到设置中保存游戏位置。", true);
            return;
        }

        ForgeVersion forge = forgeSelector.getValue();
        FabricLoaderVersion fabricLoader = fabricLoaderSelector.getValue();
        FabricApiVersion fabricApi = fabricLoader == null ? null : fabricApiSelector.getValue();
        OptiFineVersion optiFine = optiFineSelector.getValue();
        NeoForgeVersion neoForge = neoForgeSelector.getValue();
        if (optiFine != null && forge == null) {
            showStatus("OptiFine 只能与 Forge 一起安装。", true);
            return;
        }
        if (optiFine != null && !OptiFineCompatibility.isForgeCompatible(forge, optiFine)) {
            showStatus(OptiFineCompatibility.incompatibleMessage(forge, optiFine), true);
            return;
        }
        if (hasLoaderSelected() && instanceId.equals(version.id())) {
            showStatus("安装加载器时实例名不能与游戏版本号相同。", true);
            return;
        }

        onStartDownload.accept(new DownloadRequest(
                version, instanceId, gameDirectory, forge, fabricLoader, fabricApi,
                optiFine, neoForge));
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
