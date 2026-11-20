package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.InstanceDetails;
import org.chs.camlauncher.backend.api.InstanceResourcePack;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;

import java.nio.file.Path;
import java.util.Locale;

/** 管理实例资源包：显示版本号并切换启用状态。 */
final class InstanceResourcePackManagementView extends VBox implements PageLifecycle {
    private final HomeController controller;
    private final Path gameDirectory;
    private final String instanceId;
    private final Label instanceLabel = new Label();
    private final Label pathLabel = new Label();
    private final Label statusLabel = ViewSupport.hintLabel();
    private final VBox resourcePackList = new VBox();
    private final ScrollPane scroll = new ScrollPane(resourcePackList);
    private final CamlButton openFolderButton =
            CamlButton.outlined(UiText.tr("instances.manage.openFolder"));
    private FrontTask<?> inspectTask;
    private int lastCount;
    private boolean disposed;

    InstanceResourcePackManagementView(HomeController controller, Path gameDirectory,
                                       String instanceId, Runnable onBack) {
        this.controller = controller;
        this.gameDirectory = gameDirectory;
        this.instanceId = instanceId;

        getStyleClass().add("app-instance-picker");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton backButton = ViewSupport.backButton(onBack);

        Label title = new Label(UiText.tr("instances.manage.resourcePacks.pageTitle"));
        title.getStyleClass().add("caml-headline-small");

        instanceLabel.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        pathLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        pathLabel.setWrapText(true);

        VBox titles = new VBox(title, instanceLabel, pathLabel);
        titles.getStyleClass().add("app-instance-picker-titles");
        HBox header = new HBox(backButton, titles);
        header.getStyleClass().add("app-instance-picker-header");
        header.setAlignment(Pos.CENTER_LEFT);

        openFolderButton.setDisable(true);
        openFolderButton.setOnAction(event -> openFolder());
        HBox toolbar = new HBox(openFolderButton);
        toolbar.getStyleClass().add("app-instance-manage-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        resourcePackList.getStyleClass().add("app-instance-pack-list");
        resourcePackList.setMaxWidth(Double.MAX_VALUE);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("app-instance-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, statusLabel, toolbar, scroll);
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
        setStatus(UiText.tr("instances.manage.resourcePacks.scanning"), false);
        inspectTask = controller.inspectInstance(gameDirectory, instanceId,
                this::applyDetails,
                message -> {
                    if (!disposed) {
                        setStatus(UiText.tr(
                                "instances.manage.resourcePacks.scanFailed", message), true);
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
        populateResourcePacks(details);
    }

    private void populateResourcePacks(InstanceDetails details) {
        resourcePackList.getChildren().clear();
        lastCount = details.resourcePackCount();
        if (details.resourcePacks().isEmpty()) {
            Label empty = new Label(UiText.tr("instances.manage.resourcePacks.empty"));
            empty.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
            resourcePackList.getChildren().add(empty);
            setStatus(UiText.tr("instances.manage.resourcePacks.empty"), false);
            return;
        }

        for (InstanceResourcePack pack : details.resourcePacks()) {
            Label name = new Label(pack.fileName());
            name.getStyleClass().add("caml-body-medium");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);

            Label version = new Label(UiText.tr(
                    "instances.manage.resourcePacks.version", displayVersion(pack.version())));
            version.getStyleClass().add("app-instance-pack-version");
            version.setMinWidth(Region.USE_PREF_SIZE);

            Label size = new Label(formatSize(pack.sizeBytes()));
            size.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

            boolean[] enabledState = {pack.enabled()};
            ToggleButton state = new ToggleButton();
            state.getStyleClass().add("app-instance-mod-toggle");
            state.setSelected(!enabledState[0]);
            updateToggleText(state, enabledState[0]);
            state.setOnAction(event ->
                    toggleResourcePack(state, pack, enabledState));

            HBox row = new HBox(name, version, size, state);
            row.getStyleClass().add("app-instance-pack-row");
            row.setAlignment(Pos.CENTER_LEFT);
            resourcePackList.getChildren().add(row);
        }
        setStatus(UiText.tr("instances.manage.resourcePacks.count", lastCount), false);
    }

    private void toggleResourcePack(ToggleButton button, InstanceResourcePack pack,
                                    boolean[] enabledState) {
        boolean targetDisabled = button.isSelected();
        button.setDisable(true);
        controller.setResourcePackDisabled(gameDirectory, instanceId,
                pack.fileName(), targetDisabled,
                () -> {
                    if (disposed) {
                        return;
                    }
                    enabledState[0] = !targetDisabled;
                    button.setSelected(!enabledState[0]);
                    updateToggleText(button, enabledState[0]);
                    button.setDisable(false);
                },
                message -> {
                    if (disposed) {
                        return;
                    }
                    button.setSelected(!enabledState[0]);
                    updateToggleText(button, enabledState[0]);
                    button.setDisable(false);
                    setStatus(UiText.tr(
                            "instances.manage.resourcePacks.toggleFailed", message), true);
                });
    }

    private void openFolder() {
        openFolderButton.setDisable(true);
        setStatus(UiText.tr("instances.manage.openFolder.opening"), false);
        controller.openInstanceFolder(gameDirectory, instanceId,
                () -> {
                    if (!disposed) {
                        openFolderButton.setDisable(false);
                        setStatus(lastCount > 0
                                ? UiText.tr("instances.manage.resourcePacks.count", lastCount)
                                : UiText.tr("instances.manage.resourcePacks.empty"), false);
                    }
                },
                message -> {
                    if (!disposed) {
                        openFolderButton.setDisable(false);
                        setStatus(UiText.tr("instances.manage.openFolder.failed", message), true);
                    }
                });
    }

    private static void updateToggleText(ToggleButton button, boolean enabled) {
        button.setText(UiText.tr(enabled
                ? "instances.manage.resourcePacks.enabled"
                : "instances.manage.resourcePacks.disabled"));
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().remove("field-hint-error");
        if (error) {
            statusLabel.getStyleClass().add("field-hint-error");
        }
    }

    private static String displayVersion(String version) {
        return version == null || version.isBlank() ? "?" : version;
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

    @Override
    public void dispose() {
        disposed = true;
        if (inspectTask != null) {
            inspectTask.cancel();
            inspectTask = null;
        }
    }
}
