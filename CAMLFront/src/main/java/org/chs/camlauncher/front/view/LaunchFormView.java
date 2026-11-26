package org.chs.camlauncher.front.view;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.auth.AccountSummary;
import org.chs.camlauncher.backend.instance.GameDirectoryInfo;
import org.chs.camlauncher.backend.runtime.JavaCompatibilityCheck;
import org.chs.camlauncher.front.controller.AccountController;
import org.chs.camlauncher.front.task.AsyncTaskHandle;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.text.AccountChoice;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlIcon;
import org.chs.camlauncher.ui.control.CamlSelectableText;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 主页左侧的启动表单与启动交互。 */
final class LaunchFormView extends BorderPane {
    private final HomeController controller;
    private final AccountController accountController;
    private final LaunchSettings launchSettings;
    private final Runnable onLaunchStarted;
    private final Consumer<Node> showOverlay;
    private final Runnable hideOverlay;

    private final Label instanceLabel;
    private final Label gameLocationLabel;
    private final Label saveLocationLabel;
    private final CamlTextField usernameField;
    private final CamlComboBox<String> accountBox;
    private final Label managedUsernameHint = new Label();
    private final CamlButton launchBtn;
    private final Label statusLabel;

    private javafx.scene.Node usernameFieldRow;
    private List<AccountSummary> accounts = List.of();
    private String defaultAccountId;
    private GameDirectoryInfo lastGameDirectoryInfo;
    private AsyncTaskHandle<?> launchTask;

    LaunchFormView(HomeController controller, AccountController accountController,
                   LaunchSettings launchSettings, Runnable onLaunchStarted,
                   Runnable onChooseInstance, Runnable onManageInstances,
                   Runnable onManageAccounts, Consumer<Node> showOverlay, Runnable hideOverlay) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.accountController = Objects.requireNonNull(accountController, "accountController");
        this.launchSettings = Objects.requireNonNull(launchSettings, "launchSettings");
        this.onLaunchStarted = Objects.requireNonNull(onLaunchStarted, "onLaunchStarted");
        this.showOverlay = Objects.requireNonNull(showOverlay, "showOverlay");
        this.hideOverlay = Objects.requireNonNull(hideOverlay, "hideOverlay");

        getStyleClass().add("app-form-pane");

        instanceLabel = new Label();
        instanceLabel.getStyleClass().add("app-home-instance-title");
        instanceLabel.setWrapText(true);

        gameLocationLabel = new Label();
        gameLocationLabel.getStyleClass().addAll(
                "app-instance-info-detail", "app-home-meta");
        gameLocationLabel.setWrapText(true);

        saveLocationLabel = new Label();
        saveLocationLabel.getStyleClass().addAll(
                "app-instance-info-detail", "app-home-meta");
        saveLocationLabel.setWrapText(true);

        accountBox = new CamlComboBox<>();
        accountBox.setMaxWidth(Double.MAX_VALUE);
        accountBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String value) {
                return AccountChoice.label(value == null ? AccountChoice.DEFAULT : value, accounts);
            }

            @Override
            public String fromString(String value) {
                return accountBox.getItems().stream()
                        .filter(item -> toString(item).equals(value))
                        .findFirst()
                        .orElse(AccountChoice.DEFAULT);
            }
        });

        usernameField = new CamlTextField("player");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.textProperty().bindBidirectional(launchSettings.offlineUsernameProperty());

        statusLabel = new Label();
        statusLabel.getStyleClass().addAll("caml-label-large", "status-ready");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        launchBtn = CamlButton.filled(UiText.tr("home.launch"))
                .withGraphic(new CamlIcon(CamlIcon.Glyph.PLAY, 20));
        launchBtn.getStyleClass().add("app-launch-button");
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setDefaultButton(true);

        buildLayout(onChooseInstance, onManageInstances, onManageAccounts);
        wireController();
        initializeDefaults();
        refreshAccounts();
    }

    private void buildLayout(Runnable onChooseInstance, Runnable onManageInstances,
                             Runnable onManageAccounts) {
        StackPane heroIcon = new StackPane(new CamlIcon(CamlIcon.Glyph.GRID, 32));
        heroIcon.getStyleClass().add("app-home-hero-icon");

        VBox heroCopy = new VBox(instanceLabel, gameLocationLabel, saveLocationLabel);
        heroCopy.getStyleClass().add("app-home-hero-copy");
        HBox.setHgrow(heroCopy, Priority.ALWAYS);

        HBox heroHeader = new HBox(heroIcon, heroCopy);
        heroHeader.setAlignment(Pos.CENTER_LEFT);
        heroHeader.getStyleClass().add("app-home-hero-header");

        VBox instanceInfo = new VBox(heroHeader);
        instanceInfo.getStyleClass().add("app-home-hero");
        instanceInfo.setMaxWidth(Double.MAX_VALUE);

        CamlButton manageAccountsButton = CamlButton.outlined(UiText.tr("home.manageAccounts"))
                .withGraphic(new CamlIcon(CamlIcon.Glyph.ACCOUNT));
        manageAccountsButton.setMinWidth(0);
        manageAccountsButton.setOnAction(event -> onManageAccounts.run());

        HBox accountRow = new HBox(accountBox, manageAccountsButton);
        accountRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(accountBox, Priority.ALWAYS);

        // 离线用户名只在“离线用户名”模式下有意义；选择账号时整行隐藏，
        // 改显示一行说明，避免用户误以为这里填的昵称会生效。
        usernameFieldRow = ViewSupport.buildField(UiText.tr("home.username"), usernameField, null);
        managedUsernameHint.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        managedUsernameHint.setWrapText(true);
        managedUsernameHint.setVisible(false);
        managedUsernameHint.setManaged(false);

        VBox fields = new VBox(
                instanceInfo,
                ViewSupport.buildField(UiText.tr("home.account"), accountRow, null),
                usernameFieldRow,
                managedUsernameHint
        );
        fields.getStyleClass().add("app-fields");

        CamlButton chooseInstanceButton = CamlButton.outlined(UiText.tr("home.browseInstances"))
                .withGraphic(new CamlIcon(CamlIcon.Glyph.FOLDER));
        chooseInstanceButton.setMinWidth(0);
        chooseInstanceButton.setMaxWidth(Double.MAX_VALUE);
        chooseInstanceButton.setOnAction(event -> onChooseInstance.run());

        CamlButton manageInstancesButton = CamlButton.outlined(UiText.tr("home.manageInstances"))
                .withGraphic(new CamlIcon(CamlIcon.Glyph.GRID));
        manageInstancesButton.setMinWidth(0);
        manageInstancesButton.setMaxWidth(Double.MAX_VALUE);
        manageInstancesButton.setOnAction(event -> onManageInstances.run());

        GridPane instanceActions = new GridPane();
        instanceActions.getStyleClass().add("app-instance-actions");
        instanceActions.setAlignment(Pos.CENTER_LEFT);
        instanceActions.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        instanceActions.getColumnConstraints().addAll(leftColumn, rightColumn);
        instanceActions.add(chooseInstanceButton, 0, 0);
        instanceActions.add(manageInstancesButton, 1, 0);
        GridPane.setHgrow(chooseInstanceButton, Priority.ALWAYS);
        GridPane.setHgrow(manageInstancesButton, Priority.ALWAYS);

        VBox actions = new VBox(instanceActions, statusLabel, launchBtn);
        actions.getStyleClass().add("app-actions");

        ScrollPane fieldScroll = new ScrollPane(fields);
        fieldScroll.setFitToWidth(true);
        fieldScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fieldScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fieldScroll.getStyleClass().add("app-field-scroll");

        setCenter(fieldScroll);
        setBottom(actions);
    }

    private void wireController() {
        controller.setOnStatusChanged(this::showStatus);

        controller.setOnLaunchFinished(() -> {
            launchTask = null;
            setLaunchState(LaunchUiState.IDLE);
        });
        controller.setOnGameStarted(() -> setLaunchState(LaunchUiState.RUNNING));
        controller.setOnLaunchFailed(this::showLaunchError);

        controller.setOnGameDirectoryInspected(this::applyGameDirectoryInfo);

        launchSettings.selectedVersionProperty().addListener((observable, oldValue, newValue) ->
                updateInstanceInfo());
        launchSettings.fullVersionIsolationProperty().addListener((observable, oldValue, newValue) ->
                updateInstanceInfo());
        launchSettings.selectedAccountChoiceProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(accountBox.getValue())) {
                accountBox.setValue(newValue);
            }
        });
        accountBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                launchSettings.setSelectedAccountChoice(newValue);
            }
            applyAccountMode();
        });

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
        controller.inspectGameDirectory(gameDirectory, this::applyGameDirectoryInfo,
                message -> showStatus(UiText.tr("home.error.inspectDirectory", message), true));
    }

    private void launchGame() {
        if (launchTask != null && !launchTask.isDone()) {
            return;
        }
        Path gameDirectory = launchSettings.gameDirectory();
        String version = launchSettings.selectedVersion();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String accountChoice = launchSettings.selectedAccountChoice();
        boolean offline = AccountChoice.OFFLINE.equals(accountChoice);

        if (gameDirectory == null) {
            showStatus(UiText.tr("home.error.noGameDirectory"), true);
            return;
        }
        if (lastGameDirectoryInfo != null && !lastGameDirectoryInfo.directoryExists()) {
            showStatus(UiText.tr("home.error.missingGameDirectory"), true);
            return;
        }
        if (version == null || version.isBlank()) {
            showStatus(UiText.tr("home.error.noInstance"), true);
            return;
        }
        if (offline && username.isEmpty()) {
            showStatus(UiText.tr("home.error.noUsername"), true);
            return;
        }

        setLaunchState(LaunchUiState.PREPARING);

        launchTask = controller.checkJavaCompatibility(version, gameDirectory, check -> {
            if (check.compatible()) {
                startActualLaunch(version, username, accountChoice, gameDirectory, false);
            } else if (check.hasJava()) {
                showJavaWarning(check, () -> startActualLaunch(
                        version, username, accountChoice, gameDirectory, true));
            } else {
                setLaunchState(LaunchUiState.IDLE);
                showLaunchError(UiText.tr("launch.javaRequirementNotFound",
                        check.requiredMajorVersion()));
            }
        }, message -> {
            setLaunchState(LaunchUiState.IDLE);
            showLaunchError(message);
        });
    }

    private void startActualLaunch(String version, String username, String accountChoice,
                                   Path gameDirectory, boolean allowIncompatibleJava) {
        setLaunchState(LaunchUiState.PREPARING);
        onLaunchStarted.run();
        launchTask = controller.startGame(version, username, accountChoice,
                launchSettings.maxMemoryMb(), gameDirectory,
                launchSettings.fullVersionIsolation(), launchSettings.jvmArguments(),
                allowIncompatibleJava);
        controller.checkXrandrWarning(version, gameDirectory, warning -> {
            if (Boolean.TRUE.equals(warning)) {
                showXrandrWarning();
            }
        });
    }

    private void setLaunchState(LaunchUiState state) {
        launchBtn.setDisable(state.disablesLaunch());
        launchBtn.setText(UiText.tr(state.textKey()));
    }

    private void showLaunchError(String message) {
        if (getScene() == null) {
            return;
        }
        Dialog<ButtonType> dialog = LaunchErrorDialog.create(message);
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait().ifPresent(result -> {
            if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                launchGame();
            }
        });
    }

    private void showXrandrWarning() {
        if (getScene() == null) {
            return;
        }
        Label title = new Label(UiText.tr("launch.warning.xrandr.title"));
        title.getStyleClass().add("app-launch-warning-title");

        Label message = new Label(UiText.tr("launch.warning.xrandr.message"));
        message.getStyleClass().addAll("caml-body-large", "caml-on-surface");
        message.setWrapText(true);

        CamlSelectableText commands = new CamlSelectableText(
                UiText.tr("launch.warning.xrandr.commands"));
        commands.setMaxWidth(Double.MAX_VALUE);
        commands.getStyleClass().add("app-launch-warning-commands");

        CamlButton copy = CamlButton.outlined(UiText.tr("common.copy"));
        copy.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(commands.getText());
            Clipboard.getSystemClipboard().setContent(content);
            copy.setText(UiText.tr("common.copied"));
        });
        CamlButton close = CamlButton.filled(UiText.tr("common.close"));
        HBox actions = new HBox(copy, close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("app-launch-warning-actions");
        close.setOnAction(event -> hideOverlay.run());

        VBox dialog = new VBox(title, message, commands, actions);
        dialog.getStyleClass().add("app-launch-warning-dialog");
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        showOverlay.accept(dialog);
        Platform.runLater(close::requestFocus);
    }

    private void showJavaWarning(JavaCompatibilityCheck check, Runnable forceLaunch) {
        Label title = new Label(UiText.tr("launch.warning.java.title"));
        title.getStyleClass().add("app-launch-warning-title");

        Label message = new Label(UiText.tr("launch.warning.java.message",
                check.requiredMajorVersion(), check.selectedMajorVersion()));
        message.getStyleClass().addAll("caml-body-large", "caml-on-surface");
        message.setWrapText(true);

        CamlButton stop = CamlButton.outlined(UiText.tr("launch.warning.java.stop"));
        stop.setOnAction(event -> {
            hideOverlay.run();
            launchTask = null;
            setLaunchState(LaunchUiState.IDLE);
        });
        CamlButton force = CamlButton.filled(UiText.tr("launch.warning.java.force"));
        force.setOnAction(event -> {
            hideOverlay.run();
            forceLaunch.run();
        });
        HBox actions = new HBox(stop, force);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("app-launch-warning-actions");

        VBox dialog = new VBox(title, message, actions);
        dialog.getStyleClass().addAll("app-launch-warning-dialog", "app-overlay-blocking");
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        showOverlay.accept(dialog);
        Platform.runLater(stop::requestFocus);
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
                ? UiText.tr("home.instance.current.none")
                : UiText.tr("home.instance.current", version));
        Path gameDirectory = launchSettings.gameDirectory();
        if (gameDirectory == null) {
            gameLocationLabel.setText(UiText.tr("home.gameDirectory.none"));
            styleDetail(gameLocationLabel, true);
            saveLocationLabel.setText(UiText.tr("home.saveLocation.none"));
            styleDetail(saveLocationLabel, true);
            return;
        }

        boolean missing = lastGameDirectoryInfo != null && !lastGameDirectoryInfo.directoryExists();
        gameLocationLabel.setText(missing
                ? UiText.tr("home.gameDirectory.missing", gameDirectory)
                : UiText.tr("home.gameDirectory.value", gameDirectory));
        styleDetail(gameLocationLabel, missing);

        if (missing) {
            saveLocationLabel.setText(UiText.tr("home.saveLocation.fixDirectory"));
            styleDetail(saveLocationLabel, true);
            return;
        }
        if (version == null || version.isBlank()) {
            saveLocationLabel.setText(UiText.tr("home.saveLocation.selectInstance"));
            styleDetail(saveLocationLabel, false);
            return;
        }

        boolean isolated = launchSettings.fullVersionIsolation();
        Path savePath = isolated
                ? gameDirectory.resolve("versions").resolve(version).resolve("saves")
                : gameDirectory.resolve("saves");
        String suffix = isolated ? UiText.tr("home.saveLocation.isolated") : "";
        saveLocationLabel.setText(UiText.tr("home.saveLocation.value", savePath) + suffix);
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

    /**
     * 用户名输入框只在“离线用户名”模式下出现。
     *
     * <p>选择账号（含“跟随默认账号”）时整行隐藏并显示说明，避免用户以为这里
     * 输入的昵称会覆盖账号；这也和启动时的真实行为一致——账号模式下该字段被忽略。</p>
     */
    private void applyAccountMode() {
        if (usernameFieldRow == null) {
            return;
        }
        String choice = launchSettings.selectedAccountChoice();
        boolean offline = choice == null || AccountChoice.OFFLINE.equals(choice);
        usernameFieldRow.setVisible(offline);
        usernameFieldRow.setManaged(offline);
        managedUsernameHint.setVisible(!offline);
        managedUsernameHint.setManaged(!offline);
        if (!offline) {
            managedUsernameHint.setText(UiText.tr("home.username.managed", describeChoice(choice)));
        }
    }

    private String describeChoice(String choice) {
        if (AccountChoice.DEFAULT.equals(choice)) {
            for (AccountSummary account : accounts) {
                if (account.id().equals(defaultAccountId)) {
                    return AccountChoice.label(account);
                }
            }
            return UiText.tr("account.choice.default");
        }
        for (AccountSummary account : accounts) {
            if (account.id().equals(choice)) {
                return AccountChoice.label(account);
            }
        }
        return choice;
    }

    private void refreshAccounts() {
        accountController.loadAccounts(loaded -> {
            this.accounts = loaded == null ? List.of() : loaded;
            rebuildAccountChoices();
            loadDefaultAccount();
        }, error -> {
            showStatus(UiText.tr("home.error.loadAccounts", error), true);
            this.accounts = List.of();
            rebuildAccountChoices();
        });
    }

    /**
     * 重建账号下拉框。
     *
     * <p>已保存的选择对应的账号可能已经在设置页被删除：这时回退到“跟随默认账号”，
     * 否则下拉框会显示空白，用户直到启动时才看到“账号不存在”。回写会经过
     * {@code accountBox} 的监听器同步到 {@code launchSettings}。</p>
     */
    private void rebuildAccountChoices() {
        List<String> items = new ArrayList<>();
        items.add(AccountChoice.DEFAULT);
        items.add(AccountChoice.OFFLINE);
        for (AccountSummary account : accounts) {
            items.add(account.id());
        }
        String saved = launchSettings.selectedAccountChoice();
        accountBox.getItems().setAll(items);
        accountBox.setValue(saved != null && items.contains(saved)
                ? saved
                : AccountChoice.DEFAULT);
        applyAccountMode();
    }

    /** 读取全局默认账号，用于在“跟随默认账号”时显示具体账号名。 */
    private void loadDefaultAccount() {
        accountController.loadDefaultAccountId(id -> {
            defaultAccountId = id;
            applyAccountMode();
        }, error -> { });
    }

    /** 账号或实例变化后重新加载账号列表（设置页新增账号后回到主页即可见）。 */
    void reloadAccounts() {
        refreshAccounts();
    }
}
