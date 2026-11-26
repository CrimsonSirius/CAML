package org.chs.camlauncher.front.view;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.instance.InstanceDetails;
import org.chs.camlauncher.backend.instance.InstanceMod;
import org.chs.camlauncher.backend.instance.InstanceModUpdate;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.front.task.AsyncTaskHandle;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.theme.CamlMetrics;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** 实例 Mod 管理页：显示实例 mods 列表并打开实例数据目录。 */
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
    private final List<AsyncTaskHandle<byte[]>> iconTasks = new ArrayList<>();
    private final Map<String, ModRow> modRows = new LinkedHashMap<>();
    private AsyncTaskHandle<?> inspectTask;
    private AsyncTaskHandle<?> updateTask;
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

        Label title = new Label(UiText.tr("instances.manage.mods.pageTitle"));
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
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
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
        loadUpdates();
    }

    private void populateMods(InstanceDetails details) {
        cancelIconTasks();
        modRows.clear();
        modsList.getChildren().clear();
        lastModCount = details.modCount();
        if (details.mods().isEmpty()) {
            Label empty = new Label(UiText.tr("instances.manage.mods.empty"));
            empty.getStyleClass().addAll(
                    "caml-body-medium", "caml-on-surface-muted", "app-empty-state");
            modsList.getChildren().add(empty);
            setStatus(UiText.tr("instances.manage.mods.empty"), false);
            return;
        }

        for (InstanceMod mod : details.mods()) {
            StackPane iconFrame = new StackPane();
            iconFrame.getStyleClass().add("app-mod-icon-frame");
            bindSquare(iconFrame, 36);

            Label initial = new Label(initialOf(mod.fileName()));
            initial.getStyleClass().add("app-mod-icon-text");
            initial.setStyle("-fx-font-size: 1.05em;");
            StackPane placeholder = new StackPane(initial);
            placeholder.getStyleClass().add("app-mod-icon-placeholder");
            bindSquare(placeholder, 36);
            iconFrame.getChildren().add(placeholder);

            AsyncTaskHandle<byte[]> iconTask = controller.loadInstanceModIcon(
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

            Label version = new Label(displayVersion(mod.version()));
            version.getStyleClass().add("app-instance-mod-version");
            version.setMinWidth(Region.USE_PREF_SIZE);

            HBox title = new HBox(name, version);
            title.getStyleClass().add("app-instance-mod-title-row");
            title.setAlignment(Pos.CENTER_LEFT);
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);

            Label size = new Label(formatSize(mod.sizeBytes()));
            size.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

            CamlButton update = CamlButton.filled(UiText.tr("instances.manage.mods.update"));
            update.getStyleClass().add("app-instance-mod-update");
            update.setDisable(true);

            boolean[] enabledState = {mod.enabled()};
            ToggleButton state = new ToggleButton();
            state.getStyleClass().add("app-instance-mod-toggle");
            state.setSelected(!enabledState[0]);
            updateModToggleText(state, enabledState[0]);
            state.setOnAction(event -> toggleMod(state, mod, enabledState));

            ModRow modRow = new ModRow(mod, version, update);
            modRows.put(mod.fileName(), modRow);

            HBox row = new HBox(iconFrame, title, size, update, state);
            row.getStyleClass().add("app-instance-mod-row");
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(title, Priority.ALWAYS);
            modsList.getChildren().add(row);
        }
        setStatus(UiText.tr("instances.manage.mods.count", lastModCount), false);
    }

    private void loadUpdates() {
        if (modRows.isEmpty()) {
            return;
        }
        updateTask = controller.checkInstanceModUpdates(gameDirectory, instanceId,
                this::applyUpdates,
                message -> {
                    if (!disposed) {
                        setStatus(UiText.tr("instances.manage.mods.updateFailed", message), true);
                    }
                });
    }

    private void applyUpdates(List<InstanceModUpdate> updates) {
        if (disposed) {
            return;
        }
        Map<String, InstanceModUpdate> byFile = new LinkedHashMap<>();
        for (InstanceModUpdate update : updates) {
            byFile.put(update.fileName(), update);
        }
        for (ModRow row : modRows.values()) {
            InstanceModUpdate update = byFile.get(row.mod().fileName());
            if (update == null) {
                continue;
            }
            row.versionLabel().setText(versionText(update));
            row.versionLabel().getStyleClass().remove("app-instance-mod-version-update");
            if (update.updateAvailable()) {
                row.versionLabel().getStyleClass().add("app-instance-mod-version-update");
            }
            row.updateButton().setDisable(!update.updateAvailable());
            if (update.updateAvailable() && update.latestFile() != null) {
                row.updateButton().setOnAction(event -> updateMod(row, update));
            }
        }
    }

    private void updateMod(ModRow row, InstanceModUpdate update) {
        row.updateButton().setDisable(true);
        setStatus(UiText.tr("instances.manage.mods.updating", row.mod().fileName()), false);
        controller.updateInstanceMod(gameDirectory, instanceId, row.mod().fileName(),
                update.latestFile(), DownloadListener.NONE,
                result -> {
                    if (!disposed) {
                        loadInstance();
                    }
                },
                message -> {
                    if (!disposed) {
                        row.updateButton().setDisable(false);
                        setStatus(UiText.tr("instances.manage.mods.updateFailed", message), true);
                    }
                });
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

    private static String displayVersion(String version) {
        return version == null || version.isBlank() ? "?" : version;
    }

    private static String versionText(InstanceModUpdate update) {
        String current = displayVersion(update.currentVersion());
        String latest = displayVersion(update.latestVersion());
        return update.updateAvailable() && !current.equals(latest)
                ? current + " → " + latest
                : current;
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
        double decodeSize = CamlMetrics.size(36);
        Image image = new Image(new ByteArrayInputStream(bytes), decodeSize, decodeSize, true, true);
        if (image.isError() || image.getWidth() <= 0) {
            return;
        }
        ImageView view = new ImageView(image);
        DoubleBinding size = CamlMetrics.scaleProperty().multiply(36);
        view.fitWidthProperty().bind(size);
        view.fitHeightProperty().bind(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        frame.getChildren().setAll(view);
    }

    private static void bindSquare(Region region, double designPx) {
        DoubleBinding size = CamlMetrics.scaleProperty().multiply(designPx);
        region.minWidthProperty().bind(size);
        region.minHeightProperty().bind(size);
        region.prefWidthProperty().bind(size);
        region.prefHeightProperty().bind(size);
        region.maxWidthProperty().bind(size);
        region.maxHeightProperty().bind(size);
    }

    private void cancelIconTasks() {
        for (AsyncTaskHandle<byte[]> task : iconTasks) {
            task.cancel();
        }
        iconTasks.clear();
    }

    private record ModRow(InstanceMod mod, Label versionLabel, CamlButton updateButton) {
    }

    @Override
    public void dispose() {
        disposed = true;
        cancelIconTasks();
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (inspectTask != null) {
            inspectTask.cancel();
            inspectTask = null;
        }
        modRows.clear();
    }
}
