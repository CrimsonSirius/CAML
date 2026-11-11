package org.chs.camlauncher.front.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.language.Language;
import org.chs.camlauncher.ui.control.CamlButton;

import java.util.List;
import java.util.function.Consumer;

/** 下载页中的版本列表。 */
final class DownloadVersionsView extends VBox implements PageLifecycle {
    private final VersionCategory category;
    private final Label statusLabel;
    private final ScrollPane scroll = new ScrollPane();
    private FrontTask<List<VersionManifest.Version>> loadTask;
    private boolean disposed;

    DownloadVersionsView(DownloadsController controller,
                         VersionCategory category,
                         Consumer<VersionManifest.Version> onVersionSelected) {
        this.category = category;
        statusLabel = new Label(Language.tr("download.versions.loading",
                controller.downloadSourceLabel(), category.label()));
        getStyleClass().add("app-download-content");
        setPadding(new Insets(32, 40, 32, 40));
        setSpacing(16);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("app-instance-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(statusLabel, scroll);
        Consumer<List<VersionManifest.Version>> onLoaded =
                versions -> showVersions(versions, onVersionSelected);
        loadTask = switch (category) {
            case RELEASE -> controller.fetchReleaseVersions(onLoaded, this::showError);
            case SNAPSHOT -> controller.fetchSnapshotVersions(onLoaded, this::showError);
            case OLD -> controller.fetchOldVersions(onLoaded, this::showError);
        };
    }

    private void showVersions(List<VersionManifest.Version> versions,
                              Consumer<VersionManifest.Version> onVersionSelected) {
        if (disposed) {
            return;
        }
        if (versions.isEmpty()) {
            statusLabel.setText(Language.tr("download.versions.empty", category.label()));
            return;
        }

        VBox list = new VBox(8);
        list.setPadding(new Insets(4, 0, 4, 0));
        for (VersionManifest.Version version : versions) {
            String date = ViewSupport.releaseDate(version.releaseTime());
            CamlButton item = CamlButton.outlined("");
            item.getStyleClass().addAll("app-instance-item", "app-download-version-item");
            item.setMaxWidth(Double.MAX_VALUE);

            Label idLabel = new Label(version.id());
            idLabel.getStyleClass().add("app-download-version-id");
            Label dateLabel = new Label(date);
            dateLabel.getStyleClass().add("app-download-version-date");
            dateLabel.setMinWidth(96);
            dateLabel.setAlignment(Pos.CENTER_RIGHT);

            BorderPane row = new BorderPane();
            row.setLeft(idLabel);
            row.setRight(dateLabel);
            row.prefWidthProperty().bind(Bindings.max(0, item.widthProperty().subtract(48)));
            item.setGraphic(row);
            item.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            item.setAccessibleText(date.isBlank()
                    ? version.id()
                    : Language.tr("download.versions.accessible", version.id(), date));
            item.setOnAction(event -> onVersionSelected.accept(version));
            list.getChildren().add(item);
        }
        scroll.setContent(list);
        statusLabel.setText(Language.tr("download.versions.count",
                versions.size(), category.label()));
    }

    private void showError(String message) {
        if (disposed) {
            return;
        }
        statusLabel.setText(Language.tr("download.versions.error", category.label(), message));
        statusLabel.getStyleClass().remove("field-hint");
        statusLabel.getStyleClass().add("field-hint-error");
    }

    @Override
    public void dispose() {
        disposed = true;
        if (loadTask != null) {
            loadTask.cancel();
            loadTask = null;
        }
    }

}
