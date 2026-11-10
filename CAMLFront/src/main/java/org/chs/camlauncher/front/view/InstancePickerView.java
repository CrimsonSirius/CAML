package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.language.Language;
import org.chs.camlauncher.ui.control.CamlButton;

import java.nio.file.Path;
import java.util.function.Consumer;

/** 主页的二级菜单：列出当前游戏位置扫描出的所有实例。 */
final class InstancePickerView extends VBox implements PageLifecycle {
    private final Label statusLabel = new Label(Language.tr("instances.picker.scanning"));
    private final ScrollPane scroll = new ScrollPane();
    private FrontTask<?> scanTask;
    private boolean disposed;

    InstancePickerView(HomeController controller, Path gameDirectory,
                       Consumer<String> onSelected, Runnable onBack) {
        getStyleClass().add("app-instance-picker");
        setPadding(new Insets(32, 40, 32, 40));
        setSpacing(20);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton backButton = ViewSupport.backButton(onBack);

        Label title = new Label(Language.tr("instances.picker.title"));
        title.getStyleClass().add("caml-headline-small");

        Label pathLabel = new Label(gameDirectory == null
                ? Language.tr("instances.picker.noGameDirectory")
                : Language.tr("instances.picker.gameDirectory", gameDirectory));
        pathLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        pathLabel.setWrapText(true);

        VBox titles = new VBox(4, title, pathLabel);
        HBox header = new HBox(12, backButton, titles);
        header.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        statusLabel.setWrapText(true);

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("app-instance-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, statusLabel, scroll);

        if (gameDirectory == null) {
            statusLabel.setText(Language.tr("instances.picker.noGameDirectoryHint"));
            return;
        }

        scanTask = controller.inspectGameDirectory(gameDirectory, info -> {
            if (disposed) {
                return;
            }
            if (!info.directoryExists()) {
                statusLabel.setText(Language.tr("instances.picker.missing", info.directory()));
                return;
            }
            if (!info.hasInstalledVersions()) {
                statusLabel.setText(Language.tr("instances.picker.empty"));
                return;
            }

            VBox list = new VBox(8);
            list.setPadding(new Insets(4, 0, 4, 0));
            for (String version : info.installedVersions()) {
                CamlButton item = CamlButton.outlined(version);
                item.getStyleClass().add("app-instance-item");
                item.setMaxWidth(Double.MAX_VALUE);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setOnAction(event -> onSelected.accept(version));
                list.getChildren().add(item);
            }
            scroll.setContent(list);
            statusLabel.setText(Language.tr("instances.picker.count",
                    info.installedVersions().size()));
        }, message -> {
            if (!disposed) {
                statusLabel.setText(Language.tr("instances.picker.scanFailed", message));
            }
        });
    }

    @Override
    public void dispose() {
        disposed = true;
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }
}
