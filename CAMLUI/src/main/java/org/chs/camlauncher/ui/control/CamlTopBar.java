package org.chs.camlauncher.ui.control;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** CAML 顶部栏。 */
public final class CamlTopBar extends HBox {
    private final HBox actions = new HBox(8);
    private final HBox navigation = new HBox(4);

    public CamlTopBar(String title) {
        this(title, null);
    }

    public CamlTopBar(String title, String subtitle) {
        getStyleClass().add("caml-top-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("caml-title-large");

        VBox titles = new VBox(2, titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
            titles.getChildren().add(subtitleLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_RIGHT);
        navigation.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(titles, navigation, spacer, actions);
    }

    public void addAction(Node action) {
        actions.getChildren().add(action);
    }

    public void addNavigation(Node navigationNode) {
        navigation.getChildren().add(navigationNode);
    }
}
