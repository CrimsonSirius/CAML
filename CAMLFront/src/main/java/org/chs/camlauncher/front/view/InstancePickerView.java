package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.ui.control.CamlButton;

import java.nio.file.Path;
import java.util.function.Consumer;

/** 主页的二级菜单：列出当前游戏位置扫描出的所有实例。 */
final class InstancePickerView extends VBox {
    private final Label statusLabel = new Label("正在扫描游戏实例...");
    private final ScrollPane scroll = new ScrollPane();

    InstancePickerView(HomeController controller, Path gameDirectory,
                       Consumer<String> onSelected, Runnable onBack) {
        getStyleClass().add("app-instance-picker");
        setPadding(new Insets(32, 40, 32, 40));
        setSpacing(20);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton backButton = ViewSupport.backButton(onBack);

        Label title = new Label("选择游戏实例");
        title.getStyleClass().add("caml-headline-small");

        Label pathLabel = new Label(gameDirectory == null
                ? "尚未设置游戏位置"
                : "游戏位置：" + gameDirectory);
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
            statusLabel.setText("尚未设置游戏位置，请先到设置中保存游戏位置");
            return;
        }

        controller.inspectGameDirectory(gameDirectory, info -> {
            if (!info.directoryExists()) {
                statusLabel.setText("游戏位置不存在：" + info.directory());
                return;
            }
            if (!info.hasInstalledVersions()) {
                statusLabel.setText("当前游戏位置没有已安装实例");
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
            statusLabel.setText("共 " + info.installedVersions().size() + " 个游戏实例");
        });
    }
}
