package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** 主页左侧的启动表单与启动交互。 */
final class LaunchFormView extends BorderPane {
    private static final List<String> DEFAULT_VERSIONS = List.of("1.21.11", "1.21.4", "1.20.4");
    private static final String DEFAULT_VERSION = "1.21.4";

    private final MainController controller;
    private final LaunchSettings launchSettings;
    private final Runnable onLaunchStarted;

    private final CamlComboBox<Path> gameDirectoryBox;
    private final CamlButton browseButton;
    private final Label gamePathHint;
    private final CamlComboBox<String> versionBox;
    private final Label savePathHint;
    private final CamlTextField usernameField;
    private final CamlButton launchBtn;
    private final Label statusLabel;

    private GameDirectoryInfo lastGameDirectoryInfo;

    LaunchFormView(MainController controller, LaunchSettings launchSettings, Runnable onLaunchStarted) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.launchSettings = Objects.requireNonNull(launchSettings, "launchSettings");
        this.onLaunchStarted = Objects.requireNonNull(onLaunchStarted, "onLaunchStarted");

        getStyleClass().add("app-form-pane");
        setPadding(new Insets(28, 40, 28, 40));
        setMinHeight(320);

        gameDirectoryBox = new CamlComboBox<>();
        gameDirectoryBox.setMinWidth(0);
        gameDirectoryBox.setMaxWidth(Double.MAX_VALUE);
        gameDirectoryBox.setTooltip(new Tooltip("选择 Minecraft 游戏目录，通常是 .minecraft 文件夹"));

        browseButton = CamlButton.outlined("浏览");
        browseButton.setMinWidth(64);
        browseButton.setMaxWidth(Region.USE_PREF_SIZE);
        browseButton.setTooltip(new Tooltip("选择本地已有的游戏目录"));

        gamePathHint = ViewSupport.hintLabel();

        versionBox = new CamlComboBox<>();
        versionBox.getItems().addAll(DEFAULT_VERSIONS);
        versionBox.setEditable(true);
        versionBox.setMaxWidth(Double.MAX_VALUE);
        setVersionValue(DEFAULT_VERSION);

        savePathHint = ViewSupport.hintLabel();

        usernameField = new CamlTextField("namelessgod");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label("准备就绪");
        statusLabel.getStyleClass().addAll("caml-label-large", "status-ready");
        statusLabel.setWrapText(true);

        launchBtn = CamlButton.filled("启动游戏");
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setDefaultButton(true);

        buildLayout();
        wireController();
        initializeDefaults();
    }

    private void buildLayout() {
        Label heading = new Label("启动游戏");
        heading.getStyleClass().add("caml-headline-small");

        Label subheading = new Label("选择游戏位置、版本与运行参数");
        subheading.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");

        VBox header = new VBox(6, heading, subheading);

        VBox fields = new VBox(16,
                buildPathField(),
                ViewSupport.buildField("游戏版本", versionBox, savePathHint),
                ViewSupport.buildField("离线用户名", usernameField, null)
        );
        fields.getStyleClass().add("app-fields");

        VBox actions = new VBox(12, statusLabel, launchBtn);
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

    private VBox buildPathField() {
        HBox pathRow = new HBox(8, gameDirectoryBox, browseButton);
        pathRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(gameDirectoryBox, Priority.ALWAYS);

        Label label = ViewSupport.fieldLabel("游戏位置", gameDirectoryBox);
        VBox field = new VBox(8, label, pathRow, gamePathHint);
        field.getStyleClass().add("app-field");
        return field;
    }

    private void wireController() {
        controller.setOnStatusChanged(status ->
                showStatus(status, status.contains("失败") || status.contains("错误")));

        controller.setOnLaunchFinished(() -> {
            launchBtn.setDisable(false);
            launchBtn.setText("启动游戏");
        });

        controller.setOnGameDirectoryInspected(this::applyGameDirectoryInfo);

        versionBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateSaveLocationHint());
        if (versionBox.getEditor() != null) {
            versionBox.getEditor().textProperty().addListener((observable, oldValue, newValue) ->
                    updateSaveLocationHint());
        }

        gameDirectoryBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                gameDirectoryBox.setTooltip(new Tooltip(newValue.toString()));
                controller.inspectGameDirectory(newValue);
            }
        });

        browseButton.setOnAction(event -> chooseGameDirectory());
        launchBtn.setOnAction(event -> launchGame());
    }

    private void initializeDefaults() {
        Path defaultGameDirectory = controller.defaultGameDirectory();
        gameDirectoryBox.getItems().add(defaultGameDirectory);
        gameDirectoryBox.setValue(defaultGameDirectory);
    }

    private void chooseGameDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择游戏位置");

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

        if (alreadySelected) {
            controller.inspectGameDirectory(path);
        }
    }

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
        onLaunchStarted.run();

        controller.startGame(version, username, launchSettings.maxMemoryMb(), gameDirectory);
    }

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
