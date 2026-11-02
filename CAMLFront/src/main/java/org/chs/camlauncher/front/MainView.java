package org.chs.camlauncher.front;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.BackendStatus;

public final class MainView extends BorderPane {
    public MainView(BackendStatus status) {
        getStyleClass().add("app-root");
        setPadding(new Insets(32));

        Label title = new Label("CAML");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Minecraft Launcher");
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(4, title, subtitle);
        header.getStyleClass().add("header");
        setTop(header);

        Label statusLabel = new Label(status.ready() ? "Backend ready" : "Backend not ready");
        statusLabel.getStyleClass().add(status.ready() ? "status-ready" : "status-error");

        GridPane info = new GridPane();
        info.getStyleClass().add("info-grid");
        info.setHgap(20);
        info.setVgap(12);
        info.addRow(0, label("Version"), value(status.version()));
        info.addRow(1, label("Data directory"), value(status.dataDirectory().toString()));
        info.addRow(2, label("Game directory"), value(status.gameDirectory().toString()));

        VBox content = new VBox(20, statusLabel, info);
        content.getStyleClass().add("content");
        setCenter(content);

        Label footer = new Label("CAMLBackend / CAMLFront");
        footer.getStyleClass().add("footer");
        setBottom(footer);
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static Label value(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-value");
        label.setWrapText(true);
        return label;
    }
}
