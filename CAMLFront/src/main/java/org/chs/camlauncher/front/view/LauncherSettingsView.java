package org.chs.camlauncher.front.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.ui.control.CamlComboBox;

import java.util.List;
import java.util.function.Consumer;

/** 设置页中的“启动器设置”。 */
final class LauncherSettingsView extends VBox {
    private final CamlComboBox<ThemeInfo> themeBox;

    LauncherSettingsView() {
        themeBox = new CamlComboBox<>();
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setAccessibleText("主题");

        Label heading = new Label("启动器设置");
        heading.getStyleClass().add("caml-title-large");

        Label description = new Label("调整启动器自身的外观。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label themeHint = new Label("主题来自 ~/.caml/themes，切换后立即生效");
        themeHint.getStyleClass().addAll("caml-body-small", "field-hint");
        themeHint.setWrapText(true);

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(
                new VBox(6, heading, description),
                ViewSupport.buildField("主题", themeBox, themeHint)
        );
    }

    void configureThemes(List<ThemeInfo> themes, ThemeInfo active, Consumer<ThemeInfo> onSelected) {
        themeBox.getItems().setAll(themes);
        themeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeInfo theme) {
                return theme == null ? "" : theme.id();
            }

            @Override
            public ThemeInfo fromString(String value) {
                return themes.stream()
                        .filter(theme -> theme.id().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        themeBox.setValue(active);
        themeBox.setDisable(themes.isEmpty());
        themeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                onSelected.accept(newValue);
            }
        });
    }
}
