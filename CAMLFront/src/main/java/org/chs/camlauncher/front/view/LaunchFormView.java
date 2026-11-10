package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.GameDirectoryInfo;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.language.Language;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.Objects;

/** 主页左侧的启动表单与启动交互。 */
final class LaunchFormView extends BorderPane {
    private final HomeController controller;
    private final LaunchSettings launchSettings;
    private final Runnable onLaunchStarted;

    private final Label instanceLabel;
    private final Label gameLocationLabel;
    private final Label saveLocationLabel;
    private final CamlTextField usernameField;
    private final CamlButton launchBtn;
    private final Label statusLabel;

    private GameDirectoryInfo lastGameDirectoryInfo;
    private FrontTask<?> launchTask;

    LaunchFormView(HomeController controller, LaunchSettings launchSettings,
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

        launchBtn = CamlButton.filled(Language.tr("home.launch"));
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setDefaultButton(true);

        buildLayout(onChooseInstance, onManageInstances);
        wireController();
        initializeDefaults();
    }

    private void buildLayout(Runnable onChooseInstance, Runnable onManageInstances) {
        VBox instanceInfo = new VBox(10, instanceLabel, gameLocationLabel, saveLocationLabel);
        instanceInfo.getStyleClass().add("app-instance-info-group");
        VBox fields = new VBox(16,
                instanceInfo,
                ViewSupport.buildField(Language.tr("home.username"), usernameField, null)
        );
        fields.getStyleClass().add("app-fields");

        CamlButton chooseInstanceButton = CamlButton.outlined(Language.tr("home.browseInstances"));
        chooseInstanceButton.setMinWidth(0);
        chooseInstanceButton.setMaxWidth(Double.MAX_VALUE);
        chooseInstanceButton.setOnAction(event -> onChooseInstance.run());

        CamlButton manageInstancesButton = CamlButton.outlined(Language.tr("home.manageInstances"));
        manageInstancesButton.setMinWidth(0);
        manageInstancesButton.setMaxWidth(Double.MAX_VALUE);
        manageInstancesButton.setOnAction(event -> onManageInstances.run());

        GridPane instanceActions = new GridPane();
        instanceActions.setHgap(8);
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

        VBox actions = new VBox(12, instanceActions, statusLabel, launchBtn);
        actions.getStyleClass().add("app-actions");

        ScrollPane fieldScroll = new ScrollPane(fields);
        fieldScroll.setFitToWidth(true);
        fieldScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fieldScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fieldScroll.getStyleClass().add("app-field-scroll");

        setCenter(fieldScroll);
        setBottom(actions);
        BorderPane.setMargin(actions, new Insets(20, 0, 0, 0));
    }

    private void wireController() {
        controller.setOnStatusChanged(this::showStatus);

        controller.setOnLaunchFinished(() -> {
            launchTask = null;
            launchBtn.setDisable(false);
            launchBtn.setText(Language.tr("home.launch"));
        });

        controller.setOnGameDirectoryInspected(this::applyGameDirectoryInfo);

        launchSettings.selectedVersionProperty().addListener((observable, oldValue, newValue) ->
                updateInstanceInfo());
        launchSettings.fullVersionIsolationProperty().addListener((observable, oldValue, newValue) ->
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
        controller.inspectGameDirectory(gameDirectory, this::applyGameDirectoryInfo,
                message -> showStatus(Language.tr("home.error.inspectDirectory", message), true));
    }

    private void launchGame() {
        if (launchTask != null && !launchTask.isDone()) {
            return;
        }
        Path gameDirectory = launchSettings.gameDirectory();
        String version = launchSettings.selectedVersion();
        String username = usernameField.getText().trim();

        if (gameDirectory == null) {
            showStatus(Language.tr("home.error.noGameDirectory"), true);
            return;
        }
        if (lastGameDirectoryInfo != null && !lastGameDirectoryInfo.directoryExists()) {
            showStatus(Language.tr("home.error.missingGameDirectory"), true);
            return;
        }
        if (version == null || version.isBlank()) {
            showStatus(Language.tr("home.error.noInstance"), true);
            return;
        }
        if (username.isEmpty()) {
            showStatus(Language.tr("home.error.noUsername"), true);
            return;
        }

        launchBtn.setDisable(true);
        launchBtn.setText(Language.tr("home.launching"));
        onLaunchStarted.run();

        launchTask = controller.startGame(version, username, launchSettings.maxMemoryMb(),
                gameDirectory, launchSettings.fullVersionIsolation());
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
                ? Language.tr("home.instance.current.none")
                : Language.tr("home.instance.current", version));
        Path gameDirectory = launchSettings.gameDirectory();
        if (gameDirectory == null) {
            gameLocationLabel.setText(Language.tr("home.gameDirectory.none"));
            styleDetail(gameLocationLabel, true);
            saveLocationLabel.setText(Language.tr("home.saveLocation.none"));
            styleDetail(saveLocationLabel, true);
            return;
        }

        boolean missing = lastGameDirectoryInfo != null && !lastGameDirectoryInfo.directoryExists();
        gameLocationLabel.setText(missing
                ? Language.tr("home.gameDirectory.missing", gameDirectory)
                : Language.tr("home.gameDirectory.value", gameDirectory));
        styleDetail(gameLocationLabel, missing);

        if (missing) {
            saveLocationLabel.setText(Language.tr("home.saveLocation.fixDirectory"));
            styleDetail(saveLocationLabel, true);
            return;
        }
        if (version == null || version.isBlank()) {
            saveLocationLabel.setText(Language.tr("home.saveLocation.selectInstance"));
            styleDetail(saveLocationLabel, false);
            return;
        }

        boolean isolated = launchSettings.fullVersionIsolation();
        Path savePath = isolated
                ? gameDirectory.resolve("versions").resolve(version).resolve("saves")
                : gameDirectory.resolve("saves");
        String suffix = isolated ? Language.tr("home.saveLocation.isolated") : "";
        saveLocationLabel.setText(Language.tr("home.saveLocation.value", savePath) + suffix);
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
