package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlButton;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

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

    static CamlButton backButton(Runnable onBack) {
        CamlButton button = CamlButton.outlined("返回");
        button.getStyleClass().addAll("app-pill-button", "app-instance-item");
        button.setMinWidth(80);
        button.setOnAction(event -> onBack.run());
        return button;
    }

    static String releaseDate(String releaseTime) {
        if (releaseTime == null || releaseTime.isBlank()) {
            return "";
        }
        try {
            return OffsetDateTime.parse(releaseTime).toLocalDate().toString();
        } catch (DateTimeParseException ignored) {
            return releaseTime.length() >= 10 ? releaseTime.substring(0, 10) : releaseTime;
        }
    }
}
