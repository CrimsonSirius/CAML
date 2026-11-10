package org.chs.camlauncher.front.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.front.translation.Language;
import org.chs.camlauncher.ui.control.CamlComboBox;

import java.util.List;
import java.util.function.Consumer;

/** 设置页中的“外观”。 */
final class LauncherSettingsView extends VBox {
    private final CamlComboBox<ThemeInfo> themeBox;

    LauncherSettingsView() {
        themeBox = new CamlComboBox<>();
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setAccessibleText(Language.tr("settings.appearance.theme.accessible"));

        Label heading = new Label(Language.tr("settings.appearance.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(Language.tr("settings.appearance.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label themeHint = new Label(Language.tr("settings.appearance.themeHint"));
        themeHint.getStyleClass().addAll("caml-body-small", "field-hint");
        themeHint.setWrapText(true);

        VBox group = new VBox(16, heading, description,
                ViewSupport.buildField(Language.tr("settings.appearance.theme"), themeBox, themeHint));
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().add(group);
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