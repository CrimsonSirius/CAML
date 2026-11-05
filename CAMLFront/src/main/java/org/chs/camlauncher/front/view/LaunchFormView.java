package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.Objects;

/** 主页左侧的启动表单与启动交互。 */
final class LaunchFormView extends BorderPane {
    private final MainController controller;
    private final LaunchSettings launchSettings;
    private final Runnable onLaunchStarted;

    private final Label instanceLabel;
    private final Label gameLocationLabel;
    private final Label saveLocationLabel;
    private final CamlTextField usernameField;
    private final CamlButton launchBtn;
    private final Label statusLabel;

    private GameDirectoryInfo lastGameDirectoryInfo;

    LaunchFormView(MainController controller, LaunchSettings launchSettings,
                   Runnable onLaunchStarted, Runnable onChooseInstance, Runnable onManageInstances) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.launchSettings = Objects.requireNonNull(launchSettings, "launchSettings");
        this.onLaunchStarted = Objects.requireNonNull(onLaunchStarted, "onLaunchStarted");

        getStyleClass().add("app-form-pane");
        setPadding(new Insets(28, 40, 28, 40));
        setMinHeight(320);

        instanceLabel = new Label();
        instanceLabel.getStyleClass().add("app-instance-info-current");
        instanceLabel.setWrapText(true);

        gameLocationLabel = new Label();
        gameLocationLabel.getStyleClass().add("app-instance-info-detail");
        gameLocationLabel.setWrapText(true);

        saveLocationLabel = new Label();
        saveLocationLabel.getStyleClass().add("app-instance-info-detail");
        saveLocationLabel.setWrapText(true);

        usernameField = new CamlTextField("namelessgod");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label();
        statusLabel.getStyleClass().addAll("caml-label-large", "status-ready");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        launchBtn = CamlButton.filled("启动游戏");
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setDefaultButton(true);

        buildLayout(onChooseInstance, onManageInstances);
        wireController();
        initializeDefaults();
    }

    private void buildLayout(Runnable onChooseInstance, Runnable onManageInstances) {
        Label heading = new Label("启动游戏");
        heading.getStyleClass().add("caml-headline-small");

        Label subheading = new Label("选择游戏实例与运行参数");
        subheading.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");

        VBox header = new VBox(6, heading, subheading);

        VBox instanceInfo = new VBox(10, instanceLabel, gameLocationLabel, saveLocationLabel);
        instanceInfo.getStyleClass().add("app-instance-info-group");
        VBox fields = new VBox(16,
                instanceInfo,
                ViewSupport.buildField("离线用户名", usernameField, null)
        );
        fields.getStyleClass().add("app-fields");

        CamlButton chooseInstanceButton = CamlButton.outlined("浏览游戏实例");
        chooseInstanceButton.setMinWidth(0);
        chooseInstanceButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chooseInstanceButton, Priority.ALWAYS);
        chooseInstanceButton.setOnAction(event -> onChooseInstance.run());

        CamlButton manageInstancesButton = CamlButton.outlined("管理实例");
        manageInstancesButton.setMinWidth(0);
        manageInstancesButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(manageInstancesButton, Priority.ALWAYS);
        manageInstancesButton.setOnAction(event -> onManageInstances.run());

        HBox instanceActions = new HBox(8, chooseInstanceButton, manageInstancesButton);
        instanceActions.setAlignment(Pos.CENTER_LEFT);
        instanceActions.setMaxWidth(Double.MAX_VALUE);

        VBox actions = new VBox(12, instanceActions, statusLabel, launchBtn);
        actions.getStyleClass().add("app-actions");

        ScrollPane fieldScroll = new ScrollPane(fields);
        fieldScroll.setFitToWidth(true);
        fieldScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fieldScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fieldScroll.getStyleClass().add("app-field-scroll");

        setTop(header);
        setCenter(fieldScroll);
        setBottom(actions);
        BorderPane.setMargin(header, new Insets(0, 0, 20, 0));
        BorderPane.setMargin(actions, new Insets(20, 0, 0, 0));
    }

    private void wireController() {
        controller.setOnStatusChanged(status ->
                showStatus(status, status.contains("失败") || status.contains("错误")));

        controller.setOnLaunchFinished(() -> {
            launchBtn.setDisable(false);
            launchBtn.setText("启动游戏");
        });

        controller.setOnGameDirectoryInspected(this::applyGameDirectoryInfo);

        launchSettings.selectedVersionProperty().addListener((observable, oldValue, newValue) ->
                updateInstanceInfo());

        launchBtn.setOnAction(event -> launchGame());
    }

    private void initializeDefaults() {
        updateInstanceInfo();
        refreshGameDirectory(launchSettings.gameDirectory());
    }

    void refreshGameDirectory(Path gameDirectory) {
        if (gameDirectory == null) {
            updateInstanceInfo();
            return;
        }
        controller.inspectGameDirectory(gameDirectory);
    }

    private void launchGame() {
        Path gameDirectory = launchSettings.gameDirectory();
        String version = launchSettings.selectedVersion();
        String username = usernameField.getText().trim();

        if (gameDirectory == null) {
            showStatus("请先在设置中保存游戏位置", true);
            return;
        }
        if (lastGameDirectoryInfo != null && !lastGameDirectoryInfo.directoryExists()) {
            showStatus("游戏位置不存在，请到设置中保存有效目录", true);
            return;
        }
        if (version == null || version.isBlank()) {
            showStatus("请先浏览并选择游戏实例", true);
            return;
        }
        if (username.isEmpty()) {
            showStatus("请填写离线用户名", true);
            return;
        }

        launchBtn.setDisable(true);
        launchBtn.setText("启动中...");
        onLaunchStarted.run();

        controller.startGame(version, username, launchSettings.maxMemoryMb(), gameDirectory);
    }

    private void applyGameDirectoryInfo(GameDirectoryInfo info) {
        lastGameDirectoryInfo = info;

        if (!info.directoryExists()) {
            updateInstanceInfo();
            return;
        }

        if (info.hasInstalledVersions()) {
            String current = launchSettings.selectedVersion();
            if (current == null || current.isBlank() || !info.installedVersions().contains(current)) {
                launchSettings.setSelectedVersion(
                        info.installedVersions().get(info.installedVersions().size() - 1));
            }
        } else {
            launchSettings.setSelectedVersion("");
        }

        updateInstanceInfo();
    }

    private void updateInstanceInfo() {
        String version = launchSettings.selectedVersion();
        instanceLabel.setText(version == null || version.isBlank()
                ? "当前实例：未选择"
                : "当前实例：" + version);
        Path gameDirectory = launchSettings.gameDirectory();
        if (gameDirectory == null) {
            gameLocationLabel.setText("游戏位置：未设置");
            styleDetail(gameLocationLabel, true);
            saveLocationLabel.setText("存档位置：未设置");
            styleDetail(saveLocationLabel, true);
            return;
        }

        boolean missing = lastGameDirectoryInfo != null && !lastGameDirectoryInfo.directoryExists();
        gameLocationLabel.setText(missing
                ? "游戏位置：目录不存在 · " + gameDirectory
                : "游戏位置：" + gameDirectory);
        styleDetail(gameLocationLabel, missing);

        if (missing) {
            saveLocationLabel.setText("存档位置：请先修复游戏位置");
            styleDetail(saveLocationLabel, true);
            return;
        }
        if (version == null || version.isBlank()) {
            saveLocationLabel.setText("存档位置：请先选择游戏实例");
            styleDetail(saveLocationLabel, false);
            return;
        }

        boolean isolated = lastGameDirectoryInfo != null && lastGameDirectoryInfo.isIsolated(version);
        Path savePath = isolated
                ? gameDirectory.resolve("versions").resolve(version).resolve("saves")
                : gameDirectory.resolve("saves");
        saveLocationLabel.setText("存档位置：" + savePath + (isolated ? "（版本隔离）" : ""));
        styleDetail(saveLocationLabel, false);
    }

    private void showStatus(String status, boolean error) {
        statusLabel.setText(status);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.getStyleClass().removeAll("status-ready", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ready");
    }

    private void styleDetail(Label label, boolean error) {
        label.getStyleClass().removeAll("app-instance-info-detail", "app-instance-info-error");
        label.getStyleClass().add(error ? "app-instance-info-error" : "app-instance-info-detail");
    }
}
