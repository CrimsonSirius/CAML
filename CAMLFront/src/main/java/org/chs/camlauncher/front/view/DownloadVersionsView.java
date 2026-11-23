package org.chs.camlauncher.front.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.task.AsyncTaskHandle;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlIcon;

import java.util.List;
import java.util.function.Consumer;

/** 下载页中的版本列表。 */
final class DownloadVersionsView extends VBox implements PageLifecycle {
    private final VersionCategory category;
    private final Label statusLabel;
    private final ListView<VersionManifest.Version> versionList;
    private AsyncTaskHandle<List<VersionManifest.Version>> loadTask;
    private boolean disposed;

    DownloadVersionsView(DownloadsController controller,
                         VersionCategory category,
                         Consumer<VersionManifest.Version> onVersionSelected) {
        this.category = category;
        statusLabel = new Label(UiText.tr("download.versions.loading",
                controller.downloadSourceLabel(), category.label()));
        getStyleClass().addAll("app-download-content", "app-download-list-page");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);

        versionList = createVersionList(onVersionSelected);
        versionList.getStyleClass().add("app-download-version-list");
        VBox.setVgrow(versionList, Priority.ALWAYS);

        getChildren().addAll(statusLabel, versionList);
        Consumer<List<VersionManifest.Version>> onLoaded =
                this::showVersions;
        loadTask = switch (category) {
            case RELEASE -> controller.fetchReleaseVersions(onLoaded, this::showError);
            case SNAPSHOT -> controller.fetchSnapshotVersions(onLoaded, this::showError);
            case OLD -> controller.fetchOldVersions(onLoaded, this::showError);
        };
    }

    private void showVersions(List<VersionManifest.Version> versions) {
        if (disposed) {
            return;
        }
        statusLabel.getStyleClass().remove("field-hint-error");
        if (versions.isEmpty()) {
            versionList.getItems().clear();
            statusLabel.setText(UiText.tr("download.versions.empty", category.label()));
            if (!statusLabel.getStyleClass().contains("app-empty-state")) {
                statusLabel.getStyleClass().add("app-empty-state");
            }
            return;
        }
        statusLabel.getStyleClass().remove("app-empty-state");
        versionList.getItems().setAll(versions);
        versionList.scrollTo(0);
        statusLabel.setText(UiText.tr("download.versions.count",
                versions.size(), category.label()));
    }

    private void showError(String message) {
        if (disposed) {
            return;
        }
        versionList.getItems().clear();
        statusLabel.setText(UiText.tr("download.versions.error", category.label(), message));
        statusLabel.getStyleClass().removeAll("field-hint", "app-empty-state");
        statusLabel.getStyleClass().add("field-hint-error");
    }

    @Override
    public void dispose() {
        disposed = true;
        if (loadTask != null) {
            loadTask.cancel();
            loadTask = null;
        }
        versionList.getItems().clear();
    }

    static ListView<VersionManifest.Version> createVersionList(
            Consumer<VersionManifest.Version> onVersionSelected) {
        ListView<VersionManifest.Version> list = new ListView<>();
        list.setAccessibleRole(AccessibleRole.LIST_VIEW);
        list.setCellFactory(view -> new VersionCell(onVersionSelected));
        list.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                VersionManifest.Version selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    onVersionSelected.accept(selected);
                    event.consume();
                }
            }
        });
        list.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused && list.getSelectionModel().getSelectedIndex() < 0
                    && !list.getItems().isEmpty()) {
                list.getSelectionModel().selectFirst();
            }
        });
        return list;
    }

    private static final class VersionCell extends ListCell<VersionManifest.Version> {
        private final Consumer<VersionManifest.Version> onVersionSelected;
        private final HBox row;
        private final Label idLabel = new Label();
        private final Label dateLabel = new Label();

        private VersionCell(Consumer<VersionManifest.Version> onVersionSelected) {
            this.onVersionSelected = onVersionSelected;
            getStyleClass().add("app-download-version-cell");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setAccessibleRole(AccessibleRole.LIST_ITEM);

            idLabel.getStyleClass().add("app-download-version-id");
            dateLabel.getStyleClass().add("app-download-version-date");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row = new HBox(new CamlIcon(CamlIcon.Glyph.DOWNLOAD, 18), idLabel, spacer, dateLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().addAll("app-instance-item", "app-download-version-item");
            row.setMaxWidth(Double.MAX_VALUE);
            row.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(0, getWidth() - getPadding().getLeft() - getPadding().getRight()),
                    widthProperty(), paddingProperty()));
            setGraphic(row);

            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && !isEmpty()) {
                    getListView().getSelectionModel().select(getIndex());
                    onVersionSelected.accept(getItem());
                    event.consume();
                }
            });
        }

        @Override
        protected void updateItem(VersionManifest.Version version, boolean empty) {
            super.updateItem(version, empty);
            if (empty || version == null) {
                setGraphic(null);
                setAccessibleText(null);
                return;
            }

            String date = ViewSupport.releaseDate(version.releaseTime());
            idLabel.setText(version.id());
            dateLabel.setText(date);
            setGraphic(row);
            setAccessibleText(date.isBlank()
                    ? version.id()
                    : UiText.tr("download.versions.accessible", version.id(), date));
        }
    }

}
