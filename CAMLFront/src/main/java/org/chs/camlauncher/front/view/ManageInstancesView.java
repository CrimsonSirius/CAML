package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.InstanceDetails;
import org.chs.camlauncher.backend.api.InstanceMod;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 管理实例二级页：显示实例 mods 列表并打开实例数据目录。 */
final class ManageInstancesView extends VBox implements PageLifecycle {
    private final HomeController controller;
    private final Path gameDirectory;
    private final String instanceId;
    private final Label instanceLabel = new Label();
    private final Label pathLabel = new Label();
    private final Label statusLabel = ViewSupport.hintLabel();
    private final VBox modsList = new VBox();
    private final ScrollPane modsScroll = new ScrollPane(modsList);
    private final CamlButton openFolderButton =
            CamlButton.outlined(UiText.tr("instances.manage.openFolder"));
    private final List<FrontTask<byte[]>> iconTasks = new ArrayList<>();
    private FrontTask<?> inspectTask;
    private int lastModCount;
    private boolean disposed;

    ManageInstancesView(HomeController controller, Path gameDirectory, String instanceId,
                        Runnable onBack) {
        this.controller = controller;
        this.gameDirectory = gameDirectory;
        this.instanceId = instanceId;

        getStyleClass().add("app-instance-picker");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton backButton = ViewSupport.backButton(onBack);

        Label title = new Label(UiText.tr("instances.manage.title"));
        title.getStyleClass().add("caml-headline-small");

        instanceLabel.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        pathLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        pathLabel.setWrapText(true);

        VBox titles = new VBox(title, instanceLabel, pathLabel);
        titles.getStyleClass().add("app-instance-picker-titles");
        HBox header = new HBox(backButton, titles);
        header.getStyleClass().add("app-instance-picker-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label modsHeading = new Label(UiText.tr("instances.manage.mods.title"));
        modsHeading.getStyleClass().add("caml-title-small");

        openFolderButton.setDisable(true);
        openFolderButton.setOnAction(event -> openFolder());
        HBox toolbar = new HBox(openFolderButton);
        toolbar.getStyleClass().add("app-instance-manage-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        modsList.getStyleClass().add("app-instance-mod-list");
        modsList.setMaxWidth(Double.MAX_VALUE);
        modsScroll.setFitToWidth(true);
        modsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        modsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        modsScroll.getStyleClass().add("app-instance-scroll");
        VBox.setVgrow(modsScroll, Priority.ALWAYS);

        getChildren().addAll(header, statusLabel, toolbar, modsHeading, modsScroll);
        loadInstance();
    }

    private void loadInstance() {
        if (gameDirectory == null) {
            setStatus(UiText.tr("instances.manage.noGameDirectory"), true);
            return;
        }
        if (instanceId == null || instanceId.isBlank()) {
            setStatus(UiText.tr("instances.manage.noInstance"), true);
            return;
        }

        instanceLabel.setText(UiText.tr("instances.manage.instance", instanceId));
        setStatus(UiText.tr("instances.manage.mods.scanning"), false);
        inspectTask = controller.inspectInstance(gameDirectory, instanceId,
                this::applyDetails,
                message -> {
                    if (!disposed) {
                        setStatus(UiText.tr("instances.manage.mods.scanFailed", message), true);
                    }
                });
    }

    private void applyDetails(InstanceDetails details) {
        if (disposed) {
            return;
        }
        instanceLabel.setText(UiText.tr("instances.manage.instance", details.instanceId()));
        pathLabel.setText(UiText.tr("instances.manage.folder", details.gameDataDirectory()));
        openFolderButton.setDisable(false);
        populateMods(details);
    }

    private void populateMods(InstanceDetails details) {
        cancelIconTasks();
        modsList.getChildren().clear();
        lastModCount = details.modCount();
        if (details.mods().isEmpty()) {
            Label empty = new Label(UiText.tr("instances.manage.mods.empty"));
            empty.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
            modsList.getChildren().add(empty);
            setStatus(UiText.tr("instances.manage.mods.empty"), false);
            return;
        }

        for (InstanceMod mod : details.mods()) {
            StackPane iconFrame = new StackPane();
            iconFrame.getStyleClass().add("app-mod-icon-frame");
            iconFrame.setMinSize(36, 36);
            iconFrame.setPrefSize(36, 36);
            iconFrame.setMaxSize(36, 36);

            Label initial = new Label(initialOf(mod.fileName()));
            initial.getStyleClass().add("app-mod-icon-text");
            initial.setStyle("-fx-font-size: 1.05em;");
            StackPane placeholder = new StackPane(initial);
            placeholder.getStyleClass().add("app-mod-icon-placeholder");
            placeholder.setMinSize(36, 36);
            placeholder.setPrefSize(36, 36);
            placeholder.setMaxSize(36, 36);
            iconFrame.getChildren().add(placeholder);

            FrontTask<byte[]> iconTask = controller.loadInstanceModIcon(
                    gameDirectory, instanceId, mod.fileName(),
                    bytes -> applyModIcon(iconFrame, bytes),
                    ignored -> {
                    });
            if (iconTask != null) {
                iconTasks.add(iconTask);
            }

            Label name = new Label(mod.fileName());
            name.getStyleClass().add("caml-body-medium");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);

            Label size = new Label(formatSize(mod.sizeBytes()));
            size.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

            boolean[] enabledState = {mod.enabled()};
            ToggleButton state = new ToggleButton();
            state.getStyleClass().add("app-instance-mod-toggle");
            state.setSelected(!enabledState[0]);
            updateModToggleText(state, enabledState[0]);
            state.setOnAction(event -> toggleMod(state, mod, enabledState));

            HBox row = new HBox(iconFrame, name, size, state);
            row.getStyleClass().add("app-instance-mod-row");
            row.setAlignment(Pos.CENTER_LEFT);
            modsList.getChildren().add(row);
        }
        setStatus(UiText.tr("instances.manage.mods.count", lastModCount), false);
    }

    private void openFolder() {
        openFolderButton.setDisable(true);
        setStatus(UiText.tr("instances.manage.openFolder.opening"), false);
        controller.openInstanceFolder(gameDirectory, instanceId,
                () -> {
                    if (!disposed) {
                        openFolderButton.setDisable(false);
                        if (lastModCount > 0) {
                            setStatus(UiText.tr("instances.manage.mods.count", lastModCount), false);
                        } else {
                            setStatus(UiText.tr("instances.manage.mods.empty"), false);
                        }
                    }
                },
                message -> {
                    if (!disposed) {
                        openFolderButton.setDisable(false);
                        setStatus(UiText.tr("instances.manage.openFolder.failed", message), true);
                    }
                });
    }

    private void toggleMod(ToggleButton button, InstanceMod mod, boolean[] enabledState) {
        boolean targetDisabled = button.isSelected();
        button.setDisable(true);
        controller.setModDisabled(gameDirectory, instanceId, mod.fileName(), targetDisabled,
                () -> {
                    if (disposed) {
                        return;
                    }
                    enabledState[0] = !targetDisabled;
                    button.setSelected(!enabledState[0]);
                    updateModToggleText(button, enabledState[0]);
                    button.setDisable(false);
                },
                message -> {
                    if (disposed) {
                        return;
                    }
                    button.setSelected(!enabledState[0]);
                    updateModToggleText(button, enabledState[0]);
                    button.setDisable(false);
                    setStatus(UiText.tr("instances.manage.mods.toggleFailed", message), true);
                });
    }

    private static void updateModToggleText(ToggleButton button, boolean enabled) {
        button.setText(UiText.tr(enabled
                ? "instances.manage.mods.enabled"
                : "instances.manage.mods.disabled"));
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().remove("field-hint-error");
        if (error) {
            statusLabel.getStyleClass().add("field-hint-error");
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String initialOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "?";
        }
        return fileName.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static void applyModIcon(StackPane frame, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        Image image = new Image(new ByteArrayInputStream(bytes), 36, 36, true, true);
        if (image.isError() || image.getWidth() <= 0) {
            return;
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(36);
        view.setFitHeight(36);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        frame.getChildren().setAll(view);
    }

    private void cancelIconTasks() {
        for (FrontTask<byte[]> task : iconTasks) {
            task.cancel();
        }
        iconTasks.clear();
    }

    @Override
    public void dispose() {
        disposed = true;
        cancelIconTasks();
        if (inspectTask != null) {
            inspectTask.cancel();
            inspectTask = null;
        }
    }
}
