package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;

/** 管理实例二级页；当前只保留页面壳，内容待后续实现。 */
final class ManageInstancesView extends VBox {
    ManageInstancesView(Runnable onBack) {
        getStyleClass().add("app-instance-picker");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton backButton = ViewSupport.backButton(onBack);

        Label title = new Label(UiText.tr("instances.manage.title"));
        title.getStyleClass().add("caml-headline-small");

        HBox header = new HBox(backButton, title);
        header.getStyleClass().add("app-instance-picker-header");
        header.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(header);
    }
}