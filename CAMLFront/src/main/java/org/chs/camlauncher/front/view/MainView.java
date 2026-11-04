package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.front.controller.MainController;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * 主界面。
 *
 * <p>CAMLFront 只负责展示和交互：所有目录探测、下载和启动都由
 * {@link MainController} 转发给后端完成。这里唯一与文件系统相关的是
 * JavaFX 自带的 {@link DirectoryChooser}，它属于界面交互组件。</p>
 */
public final class MainView extends VBox {

    /** 没有检测到本地版本时给出的常用版本建议。 */
    private static final List<String> DEFAULT_VERSIONS = List.of("1.21.11", "1.21.4", "1.20.4");
    private static final String DEFAULT_VERSION = "1.21.4";

    private final MainController controller;

    private final ComboBox<Path> gameDirectoryBox;
    private final Button browseButton;
    private final Label gamePathHint;
    private final ComboBox<String> versionBox;
    private final Label savePathHint;
    private final TextField usernameField;
    private final Spinner<Integer> memorySpinner;
    private final Button launchBtn;
    private final Label statusLabel;
    private final TextArea logArea;

    /** 最近一次后端探测结果，用于决定目录选择器的初始位置。 */
    private GameDirectoryInfo lastGameDirectoryInfo;

    public MainView(MainController controller) {
        this.controller = controller;

        // 1. 应用全局根样式
        this.getStyleClass().add("app-root");
        this.setSpacing(10);
        this.setAlignment(Pos.TOP_CENTER);

        // 2. 顶部标题区域
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(40, 0, 20, 0));

        Label titleLabel = new Label("CAML");
        titleLabel.getStyleClass().add("title");

        Label subtitleLabel = new Label("Minecraft Launcher");
        subtitleLabel.getStyleClass().add("subtitle");

        header.getChildren().addAll(titleLabel, subtitleLabel);

        // 3. 中间表单区域
        GridPane formGrid = new GridPane();
        formGrid.getStyleClass().add("info-grid");
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        int controlWidth = 220;

        // 3.1 游戏位置：下拉框保存默认目录和用户手动添加的目录，按钮打开系统目录选择器。
        Label pathLabel = new Label("游戏位置");
        pathLabel.getStyleClass().add("field-label");

        gameDirectoryBox = new ComboBox<>();
        gameDirectoryBox.getStyleClass().add("form-input");
        gameDirectoryBox.setPrefWidth(150);
        gameDirectoryBox.setTooltip(new Tooltip("选择 Minecraft 游戏数据目录，通常是 .minecraft 文件夹"));

        browseButton = new Button("浏览...");
        browseButton.getStyleClass().add("secondary-button");
        browseButton.setTooltip(new Tooltip("选择本地已有的游戏目录"));

        HBox pathRow = new HBox(8, gameDirectoryBox, browseButton);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        gamePathHint = new Label();
        gamePathHint.getStyleClass().add("field-hint");
        gamePathHint.setWrapText(true);
        gamePathHint.setMaxWidth(controlWidth);

        VBox pathCell = new VBox(4, pathRow, gamePathHint);

        // 3.2 版本、用户名和内存
        Label vLabel = new Label("游戏版本");
        vLabel.getStyleClass().add("field-label");

        versionBox = new ComboBox<>();
        versionBox.getItems().addAll(DEFAULT_VERSIONS);
        // 允许直接输入已安装目录中的其他版本 ID。
        versionBox.setEditable(true);
        versionBox.setPrefWidth(controlWidth);
        versionBox.getStyleClass().add("form-input");
        setVersionValue(DEFAULT_VERSION);

        savePathHint = new Label();
        savePathHint.getStyleClass().add("field-hint");
        savePathHint.setWrapText(true);
        savePathHint.setMaxWidth(controlWidth);
        VBox versionCell = new VBox(4, versionBox, savePathHint);

        Label uLabel = new Label("离线用户名");
        uLabel.getStyleClass().add("field-label");

        usernameField = new TextField("namelessgod");
        usernameField.setPrefWidth(controlWidth);
        usernameField.getStyleClass().add("form-input");

        Label mLabel = new Label("最大内存 (MB)");
        mLabel.getStyleClass().add("field-label");

        memorySpinner = new Spinner<>(1024, 32768, 4096, 1024);
        memorySpinner.setEditable(true);
        memorySpinner.setPrefWidth(controlWidth);
        memorySpinner.getStyleClass().add("form-input");

        formGrid.add(pathLabel, 0, 0);
        formGrid.add(pathCell, 1, 0);
        formGrid.add(vLabel, 0, 1);
        formGrid.add(versionCell, 1, 1);
        formGrid.add(uLabel, 0, 2);
        formGrid.add(usernameField, 1, 2);
        formGrid.add(mLabel, 0, 3);
        formGrid.add(memorySpinner, 1, 3);

        // 4. 状态与启动按钮
        statusLabel = new Label("准备就绪");
        statusLabel.getStyleClass().add("status-ready");

        launchBtn = new Button("启动游戏");
        launchBtn.getStyleClass().add("launch-button");

        // 5. 日志区
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setMaxWidth(600);
        logArea.getStyleClass().add("log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox contentBox = new VBox(20, formGrid, statusLabel, launchBtn, logArea);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getStyleClass().add("content");

        // 6. 底部信息：展示后端状态、版本和数据目录（design/Implement.md 3.11）
        BackendStatus backendStatus = controller.status();
        Label footer = new Label(String.format(
                "CAML %s · 后端%s · 数据目录: %s",
                backendStatus.version(),
                backendStatus.ready() ? "已就绪" : "未就绪",
                backendStatus.dataDirectory()));
        footer.getStyleClass().add("footer");
        footer.setWrapText(true);
        footer.setMaxWidth(680);
        VBox.setMargin(footer, new Insets(20, 0, 10, 0));

        this.getChildren().addAll(header, contentBox, footer);

        // ==========================================
        // 绑定 Controller 回调
        // ==========================================

        controller.setOnLogReceived(log -> logArea.appendText(log + "\n"));

        controller.setOnStatusChanged(status ->
                showStatus(status, status.contains("失败") || status.contains("错误")));

        controller.setOnLaunchFinished(() -> {
            launchBtn.setDisable(false);
            launchBtn.setText("启动游戏");
        });

        controller.setOnGameDirectoryInspected(this::applyGameDirectoryInfo);

        // 版本变化时刷新“存档位置”提示。
        versionBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateSaveLocationHint());
        if (versionBox.getEditor() != null) {
            versionBox.getEditor().textProperty().addListener((observable, oldValue, newValue) ->
                    updateSaveLocationHint());
        }


        // 游戏位置切换后，重新探测目录并刷新版本列表。
        gameDirectoryBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // 路径可能很长，用 Tooltip 展示完整路径。
                gameDirectoryBox.setTooltip(new Tooltip(newValue.toString()));
                controller.inspectGameDirectory(newValue);
            }
        });

        // 默认使用后端配置的游戏目录。
        Path defaultGameDirectory = controller.defaultGameDirectory();
        gameDirectoryBox.getItems().add(defaultGameDirectory);
        gameDirectoryBox.setValue(defaultGameDirectory);

        // 目录选择按钮：打开系统目录选择器，支持添加用户本地已有的游戏路径。
        browseButton.setOnAction(event -> chooseGameDirectory());

        // 启动按钮
        launchBtn.setOnAction(event -> launchGame());
    }

    /** 打开系统目录选择器，并把用户选择的目录加入“游戏位置”列表。 */
    private void chooseGameDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择游戏位置");

        // 只有确认过目录存在时才设置初始位置，避免把无效路径交给 JavaFX。
        if (lastGameDirectoryInfo != null && lastGameDirectoryInfo.directoryExists()) {
            chooser.setInitialDirectory(lastGameDirectoryInfo.directory().toFile());
        }

        File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return;
        }

        Path path = selected.toPath().toAbsolutePath().normalize();
        boolean alreadySelected = path.equals(gameDirectoryBox.getValue());
        if (!gameDirectoryBox.getItems().contains(path)) {
            gameDirectoryBox.getItems().add(path);
        }
        gameDirectoryBox.setValue(path);

        // 重复选择同一路径不会触发 value 监听器，这里手动刷新一次。
        if (alreadySelected) {
            controller.inspectGameDirectory(path);
        }
    }

    /** 校验输入并派发启动。 */
    private void launchGame() {
        Path gameDirectory = gameDirectoryBox.getValue();
        String version = currentVersionInput();
        String username = usernameField.getText().trim();

        if (gameDirectory == null) {
            showStatus("请先选择游戏位置", true);
            return;
        }
        if (version.isEmpty()) {
            showStatus("请填写游戏版本", true);
            return;
        }
        if (username.isEmpty()) {
            showStatus("请填写离线用户名", true);
            return;
        }

        launchBtn.setDisable(true);
        launchBtn.setText("启动中...");
        logArea.clear();

        controller.startGame(version, username, memorySpinner.getValue(), gameDirectory);
    }

    /**
     * 处理后端返回的目录探测结果。
     * 已安装版本存在时用它们刷新版本下拉框，否则保留常用版本建议。
     */
    private void applyGameDirectoryInfo(GameDirectoryInfo info) {
        lastGameDirectoryInfo = info;

        if (!info.directoryExists()) {
            setGamePathHint("目录不存在：" + info.directory(), true);
            return;
        }

        String previous = currentVersionInput();

        if (info.hasInstalledVersions()) {
            versionBox.getItems().setAll(info.installedVersions());
            String target = info.installedVersions().contains(previous)
                    ? previous
                    : info.installedVersions().get(info.installedVersions().size() - 1);
            setVersionValue(target);
            setGamePathHint("已检测到 " + info.installedVersions().size()
                    + " 个已安装版本，可直接启动", false);
        } else {
            versionBox.getItems().setAll(DEFAULT_VERSIONS);
            setVersionValue(previous.isEmpty() ? DEFAULT_VERSION : previous);
            setGamePathHint("未检测到已安装版本，启动时将自动下载", false);
        }

        updateSaveLocationHint();
    }

    /** 读取可编辑版本框中的实际文本。 */
    private String currentVersionInput() {
        TextField editor = versionBox.getEditor();
        if (editor == null) {
            return versionBox.getValue() == null ? "" : versionBox.getValue().trim();
        }
        return editor.getText() == null ? "" : editor.getText().trim();
    }

    private void setGamePathHint(String text, boolean error) {
        gamePathHint.setText(text);
        gamePathHint.getStyleClass().removeAll("field-hint", "field-hint-error");
        gamePathHint.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }

    private void showStatus(String status, boolean error) {
        statusLabel.setText(status);
        statusLabel.getStyleClass().removeAll("status-ready", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ready");
    }

    private void setVersionValue(String version) {
        versionBox.setValue(version);
        versionBox.getEditor().setText(version);
    }

    /**
     * 刷新“存档位置”提示。
     *
     * <p>启用版本隔离的版本（PCL/HMCL 默认行为）存档在 {@code versions/<id>/saves}，
     * 否则在 {@code .minecraft/saves}。后端负责实际判定，前端只做展示。</p>
     */
    private void updateSaveLocationHint() {
        Path gameDirectory = gameDirectoryBox.getValue();
        String version = currentVersionInput();
        if (gameDirectory == null || version.isEmpty()) {
            savePathHint.setText("");
            return;
        }

        boolean isolated = lastGameDirectoryInfo != null && lastGameDirectoryInfo.isIsolated(version);
        if (isolated) {
            savePathHint.setText("版本隔离：存档在 versions/" + version + "/saves");
        } else {
            savePathHint.setText("存档位置：" + gameDirectory.resolve("saves"));
        }
        savePathHint.getStyleClass().removeAll("field-hint", "field-hint-error");
        savePathHint.getStyleClass().add("field-hint");
    }
}