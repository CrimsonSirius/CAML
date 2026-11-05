package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlSpinner;
import org.chs.camlauncher.ui.control.CamlTextArea;
import org.chs.camlauncher.ui.control.CamlTextField;
import org.chs.camlauncher.ui.control.CamlTopBar;
import org.chs.camlauncher.ui.control.CamlTopNav;
import org.chs.camlauncher.ui.control.CamlTopNavItem;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * 主界面。
 *
 * <p>CAMLFront 只负责展示和交互：所有目录探测、下载和启动都由
 * {@link MainController} 转发给后端完成。界面组件来自 CAMLUI，
 * 不包含任何文件系统、网络或进程逻辑。</p>
 */
public final class MainView extends BorderPane {

    /** 没有检测到本地版本时给出的常用版本建议。 */
    private static final List<String> DEFAULT_VERSIONS = List.of("1.21.11", "1.21.4", "1.20.4");
    private static final String DEFAULT_VERSION = "1.21.4";
    private static final double COMPACT_BREAKPOINT = 900;

    private final MainController controller;

    private final CamlComboBox<Path> gameDirectoryBox;
    private final CamlButton browseButton;
    private final Label gamePathHint;
    private final CamlComboBox<String> versionBox;
    private final Label savePathHint;
    private final CamlTextField usernameField;
    private final CamlSpinner memorySpinner;
    private final CamlButton launchBtn;
    private final Label statusLabel;
    private final CamlTextArea logArea;
    private final CamlComboBox<ThemeInfo> themeBox;
    private final SplitPane workspace;
    private final Node homePage;
    private final Node downloadsPage;
    private final Node settingsPage;
    private final CamlPageHost pageHost;

    /** 最近一次后端探测结果，用于决定目录选择器的初始位置。 */
    private GameDirectoryInfo lastGameDirectoryInfo;

    public MainView(MainController controller) {
        this.controller = controller;

        getStyleClass().add("app-root");

        gameDirectoryBox = new CamlComboBox<>();
        gameDirectoryBox.setMaxWidth(Double.MAX_VALUE);
        gameDirectoryBox.setTooltip(new Tooltip("选择 Minecraft 游戏目录，通常是 .minecraft 文件夹"));

        browseButton = CamlButton.outlined("浏览...");
        browseButton.setTooltip(new Tooltip("选择本地已有的游戏目录"));

        gamePathHint = hintLabel();

        versionBox = new CamlComboBox<>();
        versionBox.getItems().addAll(DEFAULT_VERSIONS);
        versionBox.setEditable(true);
        versionBox.setMaxWidth(Double.MAX_VALUE);
        setVersionValue(DEFAULT_VERSION);

        savePathHint = hintLabel();

        usernameField = new CamlTextField("namelessgod");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        memorySpinner = new CamlSpinner(1024, 32768, 4096, 1024);
        memorySpinner.setEditable(true);
        memorySpinner.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label("准备就绪");
        statusLabel.getStyleClass().addAll("caml-label-large", "status-ready");
        statusLabel.setWrapText(true);

        launchBtn = CamlButton.filled("启动游戏");
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setDefaultButton(true);

        logArea = new CamlTextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setMaxHeight(Double.MAX_VALUE);
        logArea.setMinHeight(200);
        logArea.setAccessibleText("运行日志");
        logArea.getStyleClass().add("app-log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        themeBox = new CamlComboBox<>();
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setAccessibleText("主题");

        workspace = new SplitPane(buildFormPane(), buildLogPane());
        workspace.getStyleClass().add("app-workspace");
        workspace.setDividerPositions(0.56);
        widthProperty().addListener((observable, oldValue, newValue) ->
                updateWorkspaceLayout(newValue.doubleValue()));

        homePage = workspace;
        downloadsPage = buildPlaceholderPage(
                "下载",
                "下载功能将在后续步骤接入。当前可以先在主页选择已安装版本并启动。");
        settingsPage = buildSettingsPage();
        pageHost = new CamlPageHost(homePage);

        setTop(buildTopBar());
        setCenter(pageHost);
        setBottom(buildFooter());

        wireController();
        initializeDefaults();
    }

    private CamlTopBar buildTopBar() {
        CamlTopBar bar = new CamlTopBar("CAML", "Minecraft Launcher");

        CamlTopNav navigation = new CamlTopNav();
        CamlTopNavItem home = navigation.addItem("主页");
        CamlTopNavItem downloads = navigation.addItem("下载");
        CamlTopNavItem settings = navigation.addItem("设置");
        navigation.select(home);
        navigation.selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected == home) {
                showPage(homePage);
            } else if (selected == downloads) {
                showPage(downloadsPage);
            } else if (selected == settings) {
                showPage(settingsPage);
            }
        });
        bar.addNavigation(navigation);
        return bar;
    }

    private void showPage(Node page) {
        pageHost.setPage(page);
    }

    private VBox buildPlaceholderPage(String title, String description) {
        Label heading = new Label(title);
        heading.getStyleClass().add("caml-headline-small");

        Label body = new Label(description);
        body.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        body.setWrapText(true);

        VBox content = new VBox(10, heading, body);
        content.setMaxWidth(640);

        VBox page = new VBox(content);
        page.getStyleClass().add("app-placeholder-page");
        page.setPadding(new Insets(48));
        page.setAlignment(Pos.TOP_LEFT);
        return page;
    }

    private BorderPane buildFormPane() {
        Label heading = new Label("启动游戏");
        heading.getStyleClass().add("caml-headline-small");

        Label subheading = new Label("选择游戏位置、版本与运行参数");
        subheading.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");

        VBox header = new VBox(6, heading, subheading);

        VBox fields = new VBox(16,
                buildPathField(),
                buildField("游戏版本", versionBox, savePathHint),
                buildField("离线用户名", usernameField, null)
        );
        fields.getStyleClass().add("app-fields");

        VBox actions = new VBox(12, statusLabel, launchBtn);
        actions.getStyleClass().add("app-actions");

        ScrollPane fieldScroll = new ScrollPane(fields);
        fieldScroll.setFitToWidth(true);
        fieldScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fieldScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fieldScroll.getStyleClass().add("app-field-scroll");

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("app-form-pane");
        pane.setPadding(new Insets(28, 40, 28, 40));
        pane.setMinHeight(320);
        pane.setTop(header);
        pane.setCenter(fieldScroll);
        pane.setBottom(actions);
        BorderPane.setMargin(header, new Insets(0, 0, 20, 0));
        BorderPane.setMargin(actions, new Insets(20, 0, 0, 0));
        return pane;
    }

    private VBox buildSettingsPage() {
        Label heading = new Label("设置");
        heading.getStyleClass().add("caml-headline-small");

        Label subheading = new Label("管理外观与游戏运行参数");
        subheading.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");

        VBox header = new VBox(6, heading, subheading);

        Label appearanceTitle = new Label("外观");
        appearanceTitle.getStyleClass().add("caml-title-medium");

        Label themeHint = new Label("主题来自 ~/.caml/themes，切换后立即生效");
        themeHint.getStyleClass().addAll("caml-body-small", "field-hint");
        themeHint.setWrapText(true);

        VBox appearance = new VBox(12,
                appearanceTitle,
                buildField("主题", themeBox, themeHint)
        );

        Label runtimeTitle = new Label("运行");
        runtimeTitle.getStyleClass().add("caml-title-medium");

        Label memoryHint = new Label("启动游戏时使用该最大堆内存");
        memoryHint.getStyleClass().addAll("caml-body-small", "field-hint");
        memoryHint.setWrapText(true);

        VBox runtime = new VBox(12,
                runtimeTitle,
                buildField("最大内存 (MB)", memorySpinner, memoryHint)
        );

        VBox content = new VBox(28, header, appearance, runtime);
        content.setMaxWidth(560);

        VBox page = new VBox(content);
        page.getStyleClass().add("app-settings-page");
        page.setPadding(new Insets(48));
        page.setAlignment(Pos.TOP_LEFT);
        return page;
    }

    private VBox buildPathField() {
        HBox pathRow = new HBox(8, gameDirectoryBox, browseButton);
        pathRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(gameDirectoryBox, Priority.ALWAYS);

        Label label = fieldLabel("游戏位置", gameDirectoryBox);
        VBox field = new VBox(8, label, pathRow, gamePathHint);
        field.getStyleClass().add("app-field");
        return field;
    }

    private VBox buildField(String labelText, Node control, Label helper) {
        Label label = fieldLabel(labelText, control);
        VBox field = new VBox(8, label, control);
        if (helper != null) {
            field.getChildren().add(helper);
        }
        field.getStyleClass().add("app-field");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return field;
    }

    private Label fieldLabel(String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("caml-label-medium");
        label.setLabelFor(control);
        return label;
    }

    private VBox buildLogPane() {
        Label heading = new Label("运行日志");
        heading.getStyleClass().add("caml-title-large");

        Label subheading = new Label("下载进度与游戏进程输出");
        subheading.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

        VBox header = new VBox(4, heading, subheading);
        VBox pane = new VBox(16, header, logArea);
        pane.getStyleClass().add("app-log-pane");
        pane.setPadding(new Insets(32));
        pane.setMinWidth(320);
        pane.setMinHeight(220);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        return pane;
    }

    private Label buildFooter() {
        BackendStatus backendStatus = controller.status();
        Label footer = new Label(String.format(
                "CAML %s · 后端%s · 数据目录: %s",
                backendStatus.version(),
                backendStatus.ready() ? "已就绪" : "未就绪",
                backendStatus.dataDirectory()));
        footer.getStyleClass().addAll("caml-body-small", "app-footer-bar");
        footer.setWrapText(true);
        footer.setMaxWidth(Double.MAX_VALUE);
        return footer;
    }

    private Label hintLabel() {
        Label label = new Label();
        label.getStyleClass().addAll("caml-body-small", "field-hint");
        label.setWrapText(true);
        return label;
    }

    /** 宽屏左右分栏；窗口变窄时改为上下堆叠，保证表单不被压扁。 */
    private void updateWorkspaceLayout(double width) {
        if (width <= 0) {
            return;
        }
        Orientation target = width < COMPACT_BREAKPOINT
                ? Orientation.VERTICAL
                : Orientation.HORIZONTAL;
        if (workspace.getOrientation() != target) {
            workspace.setOrientation(target);
            workspace.setDividerPositions(target == Orientation.VERTICAL ? 0.62 : 0.56);
        }
    }

    /** 填充主题选择器；切换时由应用入口负责加载 CSS 并持久化。 */
    public void configureThemes(List<ThemeInfo> themes, ThemeInfo active, Consumer<ThemeInfo> onSelected) {
        themeBox.getItems().setAll(themes);
        themeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeInfo theme) {
                return theme == null ? "" : theme.id();
            }

            @Override
            public ThemeInfo fromString(String value) {
                return themes.stream()
                        .filter(theme -> theme.id().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        themeBox.setValue(active);
        themeBox.setDisable(themes.isEmpty());
        themeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                onSelected.accept(newValue);
            }
        });
    }

    private void wireController() {
        controller.setOnLogReceived(log -> logArea.appendText(log + "\n"));

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

    /** 打开系统目录选择器，并把用户选择的目录加入“游戏位置”列表。 */
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
