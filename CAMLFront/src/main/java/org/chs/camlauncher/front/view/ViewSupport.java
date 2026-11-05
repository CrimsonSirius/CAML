package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** 跨页面复用的轻量布局工具。 */
final class ViewSupport {
    private ViewSupport() {
    }

    static Label hintLabel() {
        Label label = new Label();
        label.getStyleClass().addAll("caml-body-small", "field-hint");
        label.setWrapText(true);
        return label;
    }

    static Label fieldLabel(String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("caml-label-medium");
        label.setLabelFor(control);
        return label;
    }

    static VBox buildField(String labelText, Node control, Label helper) {
        Label label = fieldLabel(labelText, control);
        VBox field = new VBox(8, label, control);
        if (helper != null) {
            field.getChildren().add(helper);
        }
        field.getStyleClass().add("app-field");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return field;
    }

    static VBox emptySidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setMinWidth(180);
        sidebar.setPrefWidth(180);
        sidebar.setMaxWidth(180);
        sidebar.setMaxHeight(Double.MAX_VALUE);
        return sidebar;
    }
}
