package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.instance.InstanceDetails;
import org.chs.camlauncher.front.task.AsyncTaskHandle;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;

import java.nio.file.Path;

/** 管理实例入口页：进入 Mod 或资源包管理，或打开实例目录。 */
final class InstanceManagementView extends VBox implements PageLifecycle {
    private final HomeController controller;
    private final Path gameDirectory;
    private final String instanceId;
    private final Label instanceLabel = new Label();
    private final Label pathLabel = new Label();
    private final Label statusLabel = ViewSupport.hintLabel();
    private final CamlButton openFolderButton =
            CamlButton.outlined(UiText.tr("instances.manage.openFolder"));
    private final CamlButton manageModsButton =
            CamlButton.filled(UiText.tr("instances.manage.manageMods"));
    private final CamlButton manageResourcePacksButton =
            CamlButton.tonal(UiText.tr("instances.manage.manageResourcePacks"));
    private AsyncTaskHandle<?> inspectTask;
    private InstanceDetails details;
    private boolean disposed;

    InstanceManagementView(HomeController controller, Path gameDirectory, String instanceId,
                            Runnable onBack, Runnable onManageMods,
                            Runnable onManageResourcePacks) {
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

        openFolderButton.setDisable(true);
        openFolderButton.setOnAction(event -> openFolder());
        manageModsButton.setMaxWidth(Double.MAX_VALUE);
        manageResourcePacksButton.setMaxWidth(Double.MAX_VALUE);
        manageModsButton.setDisable(true);
        manageResourcePacksButton.setDisable(true);
        manageModsButton.setOnAction(event -> onManageMods.run());
        manageResourcePacksButton.setOnAction(event -> onManageResourcePacks.run());
        HBox actions = new HBox(manageModsButton, manageResourcePacksButton);
        actions.getStyleClass().add("app-instance-manage-actions");
        actions.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(manageModsButton, Priority.ALWAYS);
        HBox.setHgrow(manageResourcePacksButton, Priority.ALWAYS);

        HBox toolbar = new HBox(openFolderButton);
        toolbar.getStyleClass().add("app-instance-manage-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, statusLabel, toolbar, actions);
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
        setStatus(UiText.tr("instances.manage.scanning"), false);
        inspectTask = controller.inspectInstance(gameDirectory, instanceId,
                this::applyDetails,
                message -> {
                    if (!disposed) {
                        setStatus(UiText.tr("instances.manage.scanFailed", message), true);
                    }
                });
    }

    private void applyDetails(InstanceDetails details) {
        if (disposed) {
            return;
        }
        this.details = details;
        instanceLabel.setText(UiText.tr("instances.manage.instance", details.instanceId()));
        pathLabel.setText(UiText.tr("instances.manage.folder", details.gameDataDirectory()));
        openFolderButton.setDisable(false);
        manageModsButton.setDisable(false);
        manageResourcePacksButton.setDisable(false);
        setStatus(UiText.tr("instances.manage.summary",
                details.modCount(), details.resourcePackCount()), false);
    }

    private void openFolder() {
        openFolderButton.setDisable(true);
        setStatus(UiText.tr("instances.manage.openFolder.opening"), false);
        controller.openInstanceFolder(gameDirectory, instanceId,
                () -> {
                    if (!disposed) {
                        openFolderButton.setDisable(false);
                        setStatus(UiText.tr("instances.manage.summary",
                                details.modCount(), details.resourcePackCount()), false);
                    }
                },
                message -> {
                    if (!disposed) {
                        openFolderButton.setDisable(false);
                        setStatus(UiText.tr("instances.manage.openFolder.failed", message), true);
                    }
                });
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().remove("field-hint-error");
        if (error) {
            statusLabel.getStyleClass().add("field-hint-error");
        }
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
