package org.chs.camlauncher.front.view;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.runtime.JavaCandidate;
import org.chs.camlauncher.backend.runtime.JavaDownloadOption;
import org.chs.camlauncher.backend.runtime.JavaDistributionOption;
import org.chs.camlauncher.backend.runtime.JavaPackageType;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.front.task.AsyncTaskHandle;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlIcon;
import org.chs.camlauncher.ui.theme.CamlMotion;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** 设置页中的 Java 管理：扫描并添加启动器可用的 Java。 */
final class JavaManagementView extends VBox implements PageLifecycle {
    private final SettingsController controller;
    private final LaunchSettings launchSettings;
    private final Consumer<List<JavaCandidate>> onCandidatesChanged;
    private final Consumer<Node> showOverlay;
    private final Runnable hideOverlay;
    private final Label statusLabel = new Label();
    private final VBox list = new VBox();
    private final CamlButton refreshButton = CamlButton.outlined(
            UiText.tr("settings.game.java.rescan"));
    private final CamlButton addButton = CamlButton.filled(
            UiText.tr("settings.game.java.add"));
    private final List<JavaCandidate> candidates = new ArrayList<>();
    private AsyncTaskHandle<List<JavaCandidate>> scanTask;
    private AsyncTaskHandle<JavaCandidate> addTask;
    private AsyncTaskHandle<List<JavaDownloadOption>> versionTask;
    private AsyncTaskHandle<JavaCandidate> installTask;
    private boolean scanning;
    private boolean adding;
    private boolean loaded;
    private boolean disposed;

    JavaManagementView(SettingsController controller, LaunchSettings launchSettings,
                       Consumer<List<JavaCandidate>> onCandidatesChanged,
                       boolean linux, Consumer<Node> showOverlay, Runnable hideOverlay) {
        this.controller = controller;
        this.launchSettings = launchSettings;
        this.onCandidatesChanged = onCandidatesChanged;
        this.showOverlay = showOverlay;
        this.hideOverlay = hideOverlay;

        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().addAll("app-settings-content", "app-java-management");

        refreshButton.setOnAction(event -> scan());
        addButton.setOnAction(event -> chooseJavaFile());
        CamlButton downloadButton = CamlButton.outlined(
                UiText.tr("settings.javaManagement.download"));
        downloadButton.setOnAction(event -> showJavaDownloadDialog());
        refreshButton.getStyleClass().add("app-java-management-toolbar-button");
        addButton.getStyleClass().add("app-java-management-toolbar-button");
        downloadButton.getStyleClass().add("app-java-management-toolbar-button");
        HBox toolbar = new HBox(refreshButton, addButton, downloadButton);
        toolbar.getStyleClass().add("app-java-management-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);
        list.getStyleClass().add("app-java-management-list");
        list.setFillWidth(true);

        getChildren().add(toolbar);
        if (linux) {
            Label linuxHint = new Label(
                    UiText.tr("settings.javaManagement.linuxHint"));
            linuxHint.getStyleClass().addAll(
                    "caml-body-small", "app-java-management-linux-hint");
            linuxHint.setWrapText(true);
            getChildren().add(linuxHint);
        }
        getChildren().addAll(statusLabel, list);
    }

    private void showJavaDownloadDialog() {
        if (getScene() == null || disposed) {
            return;
        }
        Label title = new Label(UiText.tr("settings.javaManagement.download"));
        title.getStyleClass().add("app-settings-group-title");

        CamlComboBox<JavaDistributionOption> distributionBox = new CamlComboBox<>();
        distributionBox.getItems().setAll(controller.javaDownloadDistributions());
        distributionBox.setMaxWidth(Double.MAX_VALUE);
        distributionBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(JavaDistributionOption value) {
                return value == null ? "" : value.displayName();
            }

            @Override
            public JavaDistributionOption fromString(String value) {
                return distributionBox.getItems().stream()
                        .filter(item -> item.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });

        CamlComboBox<JavaPackageType> packageTypeBox = new CamlComboBox<>();
        packageTypeBox.getItems().setAll(JavaPackageType.values());
        packageTypeBox.setMaxWidth(Double.MAX_VALUE);
        packageTypeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(JavaPackageType value) {
                return value == null ? "" : value.displayName();
            }

            @Override
            public JavaPackageType fromString(String value) {
                return JavaPackageType.fromApi(value);
            }
        });

        CamlComboBox<JavaDownloadOption> versionBox = new CamlComboBox<>();
        versionBox.setMaxWidth(Double.MAX_VALUE);
        versionBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(JavaDownloadOption value) {
                return value == null ? "" : value.displayName();
            }

            @Override
            public JavaDownloadOption fromString(String value) {
                return versionBox.getItems().stream()
                        .filter(item -> item.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });

        Label downloadStatus = new Label();
        downloadStatus.getStyleClass().addAll("caml-body-small", "field-hint");
        downloadStatus.setWrapText(true);

        CamlButton download = CamlButton.filled(
                UiText.tr("settings.javaManagement.downloadAndInstall"));
        download.setDisable(true);
        CamlButton close = CamlButton.outlined(UiText.tr("common.close"));
        HBox actions = new HBox(download, close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("app-java-download-actions");

        GridPane form = new GridPane();
        form.getStyleClass().add("app-java-download-form");
        form.addRow(0, formLabel("settings.javaManagement.distribution"), distributionBox);
        form.addRow(1, formLabel("settings.javaManagement.packageType"), packageTypeBox);
        form.addRow(2, formLabel("settings.javaManagement.version"), versionBox);
        ColumnConstraints labelColumn = new ColumnConstraints();
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().setAll(labelColumn, fieldColumn);

        Runnable loadVersions = () -> loadJavaVersions(
                distributionBox, packageTypeBox, versionBox, downloadStatus, download);
        distributionBox.valueProperty().addListener((observable, oldValue, value) ->
                loadVersions.run());
        packageTypeBox.valueProperty().addListener((observable, oldValue, value) ->
                loadVersions.run());
        versionBox.valueProperty().addListener((observable, oldValue, value) ->
                download.setDisable(value == null));
        distributionBox.getSelectionModel().selectFirst();
        packageTypeBox.getSelectionModel().selectFirst();
        loadVersions.run();

        close.setOnAction(event -> {
            if (versionTask != null) {
                versionTask.cancel();
                versionTask = null;
            }
            hideOverlay.run();
        });
        download.setOnAction(event -> startJavaDownload(
                versionBox.getValue(), nameFor(versionBox.getValue()),
                distributionBox, packageTypeBox, versionBox, downloadStatus,
                download, close));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox dialog = new VBox(title, form, downloadStatus, spacer, actions);
        dialog.getStyleClass().add("app-java-download-dialog");
        dialog.setMaxWidth(Double.MAX_VALUE);
        showOverlay.accept(dialog);
        Platform.runLater(close::requestFocus);
    }

    private void loadJavaVersions(CamlComboBox<JavaDistributionOption> distributionBox,
                                  CamlComboBox<JavaPackageType> packageTypeBox,
                                  CamlComboBox<JavaDownloadOption> versionBox,
                                  Label status, CamlButton download) {
        if (versionTask != null) {
            versionTask.cancel();
            versionTask = null;
        }
        JavaDistributionOption distribution = distributionBox.getValue();
        JavaPackageType packageType = packageTypeBox.getValue();
        versionBox.getItems().clear();
        versionBox.setValue(null);
        download.setDisable(true);
        if (distribution == null || packageType == null) {
            status.setText("");
            return;
        }
        status.setText(UiText.tr("settings.javaManagement.loadingVersions"));
        status.getStyleClass().removeAll("field-hint-error", "app-empty-state");
        ensureHintStyle(status);
        versionTask = controller.listJavaDownloadOptions(distribution.id(), packageType,
                options -> {
                    if (disposed) {
                        return;
                    }
                    versionTask = null;
                    versionBox.getItems().setAll(options);
                    if (options.isEmpty()) {
                        status.setText(UiText.tr("settings.javaManagement.emptyVersions"));
                    } else {
                        versionBox.getSelectionModel().selectFirst();
                        status.setText(UiText.tr("settings.javaManagement.versionCount",
                                options.size()));
                    }
                },
                message -> {
                    if (disposed) {
                        return;
                    }
                    versionTask = null;
                    status.setText(UiText.tr(
                            "settings.javaManagement.loadVersionsFailed", message));
                    status.getStyleClass().remove("field-hint");
                    status.getStyleClass().add("field-hint-error");
                });
    }

    private void startJavaDownload(JavaDownloadOption option, String name,
                                   CamlComboBox<JavaDistributionOption> distributionBox,
                                   CamlComboBox<JavaPackageType> packageTypeBox,
                                   CamlComboBox<JavaDownloadOption> versionBox,
                                   Label status, CamlButton download, CamlButton close) {
        if (option == null || disposed) {
            return;
        }
        distributionBox.setDisable(true);
        packageTypeBox.setDisable(true);
        versionBox.setDisable(true);
        download.setDisable(true);
        close.setDisable(true);
        status.getStyleClass().removeAll("field-hint-error", "app-empty-state");
        ensureHintStyle(status);
        status.setText(UiText.tr("settings.javaManagement.downloading"));
        DownloadListener listener = progress -> Platform.runLater(() -> {
            if (disposed) {
                return;
            }
            if (progress.total() > 0) {
                int percent = (int) Math.min(100,
                        Math.round(progress.downloaded() * 100.0 / progress.total()));
                status.setText(UiText.tr(
                        "settings.javaManagement.downloadingProgress", percent));
            }
        });
        installTask = controller.downloadJava(option, name, listener, candidate -> {
            if (disposed) {
                return;
            }
            installTask = null;
            launchSettings.setJavaSelectionMode(JavaSelectionMode.MANUAL);
            launchSettings.setSelectedJavaPath(candidate.executable());
            controller.saveJavaSelectionAsync(
                    JavaSelectionMode.MANUAL, candidate.executable());
            hideOverlay.run();
            statusLabel.setText(UiText.tr(
                    "settings.javaManagement.installed", candidate.executable()));
            scan();
        }, message -> {
            if (disposed) {
                return;
            }
            installTask = null;
            distributionBox.setDisable(false);
            packageTypeBox.setDisable(false);
            versionBox.setDisable(false);
            download.setDisable(versionBox.getValue() == null);
            close.setDisable(false);
            status.setText(UiText.tr(
                    "settings.javaManagement.installFailed", message));
            status.getStyleClass().remove("field-hint");
            status.getStyleClass().add("field-hint-error");
        });
    }

    private static Label formLabel(String key) {
        Label label = new Label(UiText.tr(key));
        label.getStyleClass().add("caml-label-medium");
        return label;
    }

    private static String nameFor(JavaDownloadOption option) {
        if (option == null) {
            return "java";
        }
        return option.distributionId() + "-" + option.majorVersion()
                + "-" + option.packageType().apiValue();
    }

    private static void ensureHintStyle(Label label) {
        if (!label.getStyleClass().contains("field-hint")) {
            label.getStyleClass().add("field-hint");
        }
    }

    @Override
    public void onShow() {
        if (!loaded && !scanning && !adding) {
            scan();
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        if (addTask != null) {
            addTask.cancel();
            addTask = null;
        }
        if (versionTask != null) {
            versionTask.cancel();
            versionTask = null;
        }
        if (installTask != null) {
            installTask.cancel();
            installTask = null;
        }
    }

    private void scan() {
        if (scanning || adding || disposed) {
            return;
        }
        scanning = true;
        statusLabel.setText(UiText.tr("settings.game.java.scanningAll"));
        statusLabel.getStyleClass().removeAll("field-hint-error", "app-empty-state");
        ensureHintStyle();
        scanTask = controller.scanJavaCandidates(this::applyCandidates, message -> {
            if (disposed) {
                return;
            }
            scanning = false;
            scanTask = null;
            statusLabel.setText(UiText.tr("settings.game.java.scanFailed", message));
            statusLabel.getStyleClass().removeAll("field-hint", "app-empty-state");
            statusLabel.getStyleClass().add("field-hint-error");
        });
    }

    private void applyCandidates(List<JavaCandidate> loadedCandidates) {
        if (disposed) {
            return;
        }
        scanning = false;
        loaded = true;
        scanTask = null;
        candidates.clear();
        candidates.addAll(loadedCandidates);
        sortCandidates(candidates);
        rebuildList();
        onCandidatesChanged.accept(List.copyOf(candidates));
    }

    private void rebuildList() {
        list.getChildren().clear();
        statusLabel.getStyleClass().removeAll("field-hint-error", "app-empty-state");
        ensureHintStyle();
        if (candidates.isEmpty()) {
            statusLabel.setText(UiText.tr("settings.javaManagement.empty"));
            statusLabel.getStyleClass().add("app-empty-state");
            return;
        }
        statusLabel.setText(UiText.tr("settings.javaManagement.count", candidates.size()));
        for (JavaCandidate candidate : candidates) {
            list.getChildren().add(javaRow(candidate));
        }
    }

    private Region javaRow(JavaCandidate candidate) {
        Label badge = new Label(candidate.majorVersion() > 0
                ? Integer.toString(candidate.majorVersion())
                : "?");
        badge.getStyleClass().add("app-java-version-badge");

        Label nameLabel = new Label(nameText(candidate));
        nameLabel.getStyleClass().add("app-java-management-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        HBox nameRow = new HBox(nameLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.getStyleClass().add("app-java-management-name-row");
        if (candidate.manual()) {
            nameRow.getChildren().add(badge("settings.javaManagement.manual"));
        }
        if (!candidate.available()) {
            nameRow.getChildren().add(badge("settings.javaManagement.unavailable"));
        }

        Label metaLabel = new Label(UiText.tr("settings.javaManagement.meta",
                candidate.architecture(),
                candidate.bits(),
                UiText.tr(candidate.jdk() ? "settings.game.java.jdk" : "settings.game.java.jre")));
        metaLabel.getStyleClass().add("app-java-management-meta");
        metaLabel.setWrapText(true);

        Label pathLabel = new Label(candidate.executable().toString());
        pathLabel.getStyleClass().add("app-java-management-path");
        pathLabel.setWrapText(true);

        VBox info = new VBox(nameRow, metaLabel, pathLabel);
        info.getStyleClass().add("app-java-management-info");
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);

        CamlButton openFolder = CamlButton.outlined("")
                .withGraphic(new CamlIcon(CamlIcon.Glyph.FOLDER, 18));
        openFolder.getStyleClass().add("app-java-management-icon-button");
        openFolder.setAccessibleText(UiText.tr("settings.javaManagement.openFolder"));
        openFolder.setTooltip(new Tooltip(UiText.tr("settings.javaManagement.openFolder")));
        installPressAnimation(openFolder);
        openFolder.setOnAction(event -> openFolder(candidate, openFolder));

        HBox header = new HBox(badge, info, openFolder);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-java-management-row");
        header.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(header);
        card.getStyleClass().addAll("app-settings-group", "app-java-management-card");
        if (!candidate.available()) {
            card.getStyleClass().add("app-java-management-card-unavailable");
        }
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void openFolder(JavaCandidate candidate, CamlButton button) {
        button.setDisable(true);
        statusLabel.setText(UiText.tr("settings.javaManagement.openFolder.opening"));
        statusLabel.getStyleClass().remove("field-hint-error");
        ensureHintStyle();
        controller.openJavaFolder(candidate.executable(), () -> {
            if (disposed) {
                return;
            }
            button.setDisable(false);
            statusLabel.setText(UiText.tr("settings.javaManagement.count", candidates.size()));
            statusLabel.getStyleClass().remove("field-hint-error");
            ensureHintStyle();
        }, message -> {
            if (disposed) {
                return;
            }
            button.setDisable(false);
            statusLabel.setText(UiText.tr(
                    "settings.javaManagement.openFolder.failed", message));
            statusLabel.getStyleClass().remove("field-hint");
            statusLabel.getStyleClass().add("field-hint-error");
        });
    }

    private static Label badge(String textKey) {
        Label badge = new Label(UiText.tr(textKey));
        badge.getStyleClass().add("app-java-management-badge");
        return badge;
    }

    private static String nameText(JavaCandidate candidate) {
        String vendor = candidate.vendor().isBlank() ? "Java" : candidate.vendor();
        return candidate.version().isBlank() ? vendor : vendor + " " + candidate.version();
    }

    private void chooseJavaFile() {
        if (getScene() == null || disposed || adding || scanning) {
            return;
        }
        adding = true;
        Path current = launchSettings.selectedJavaPath();
        Path parent = current == null ? null : current.getParent();
        if (parent == null) {
            showJavaChooser(null);
            return;
        }

        controller.checkDirectory(parent,
                exists -> showJavaChooser(exists ? parent : null),
                message -> showJavaChooser(null));
    }

    private void showJavaChooser(Path initialDirectory) {
        if (getScene() == null || disposed) {
            adding = false;
            return;
        }
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle(UiText.tr("settings.game.java.chooserTitle"));
        if (initialDirectory != null) {
            chooser.setInitialDirectory(initialDirectory.toFile());
        }
        File selected = chooser.showOpenDialog(getScene().getWindow());
        if (selected == null) {
            adding = false;
            return;
        }

        statusLabel.setText(UiText.tr("settings.game.java.checking"));
        statusLabel.getStyleClass().remove("field-hint-error");
        ensureHintStyle();
        addTask = controller.addManualJava(selected.toPath(), candidate -> {
            if (disposed) {
                return;
            }
            addTask = null;
            adding = false;
            candidates.removeIf(item -> samePath(item.executable(), candidate.executable()));
            candidates.add(candidate);
            sortCandidates(candidates);
            loaded = true;
            rebuildList();
            onCandidatesChanged.accept(List.copyOf(candidates));
        }, message -> {
            if (disposed) {
                return;
            }
            addTask = null;
            adding = false;
            statusLabel.setText(UiText.tr("settings.game.java.addFailed", message));
            statusLabel.getStyleClass().remove("field-hint");
            statusLabel.getStyleClass().add("field-hint-error");
        });
    }

    private static void installPressAnimation(CamlButton button) {
        ScaleTransition[] animation = new ScaleTransition[1];
        button.addEventHandler(MouseEvent.MOUSE_PRESSED,
                event -> animateScale(button, animation, 0.94, 70));
        button.addEventHandler(MouseEvent.MOUSE_RELEASED,
                event -> animateScale(button, animation, 1, 160));
        button.addEventHandler(MouseEvent.MOUSE_EXITED,
                event -> animateScale(button, animation, 1, 160));
    }

    private static void animateScale(CamlButton button, ScaleTransition[] animation,
                                     double target, long millis) {
        if (animation[0] != null) {
            animation[0].stop();
        }
        if (CamlMotion.reduceMotion()) {
            button.setScaleX(1);
            button.setScaleY(1);
            return;
        }
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), button);
        transition.setToX(target);
        transition.setToY(target);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.play();
        animation[0] = transition;
    }

    private static boolean samePath(Path left, Path right) {
        return left != null && right != null
                && left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private static void sortCandidates(List<JavaCandidate> candidates) {
        candidates.sort(Comparator
                .comparing(JavaCandidate::available).reversed()
                .thenComparing(Comparator.comparingInt(JavaCandidate::majorVersion).reversed())
                .thenComparing(candidate -> candidate.executable().toString()));
    }

    private void ensureHintStyle() {
        if (!statusLabel.getStyleClass().contains("field-hint")) {
            statusLabel.getStyleClass().add("field-hint");
        }
    }
}
