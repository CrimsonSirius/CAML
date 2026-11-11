package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlRadioButton;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 设置页中的“外观”和“语言”。 */
final class LauncherSettingsView extends VBox {
    private final CamlComboBox<ThemeInfo> themeBox;
    private final CamlRadioButton automaticLanguage;
    private final CamlRadioButton manualLanguage;
    private final CamlComboBox<String> languageBox;
    private String manualLocale;
    private boolean updatingLanguage;

    LauncherSettingsView(LanguageSelectionMode initialMode, String initialLocale,
                         BiConsumer<LanguageSelectionMode, String> onLanguageChanged) {
        themeBox = new CamlComboBox<>();
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setAccessibleText(UiText.tr("settings.appearance.theme.accessible"));

        automaticLanguage = new CamlRadioButton(UiText.tr("settings.appearance.language.auto"));
        manualLanguage = new CamlRadioButton(UiText.tr("settings.appearance.language.manual"));
        languageBox = new CamlComboBox<>();
        languageBox.setMaxWidth(Double.MAX_VALUE);
        languageBox.setAccessibleText(UiText.tr("settings.appearance.language.accessible"));

        Label heading = new Label(UiText.tr("settings.appearance.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("settings.appearance.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label themeHint = new Label(UiText.tr("settings.appearance.themeHint"));
        themeHint.getStyleClass().addAll("caml-body-small", "field-hint");
        themeHint.setWrapText(true);

        VBox appearanceGroup = new VBox(16, heading, description,
                ViewSupport.buildField(UiText.tr("settings.appearance.theme"), themeBox, themeHint));
        appearanceGroup.getStyleClass().add("app-settings-group");
        appearanceGroup.setMaxWidth(Double.MAX_VALUE);

        Label languageHeading = new Label(UiText.tr("settings.appearance.language.title"));
        languageHeading.getStyleClass().add("app-settings-group-title");

        Label languageDescription = new Label(
                UiText.tr("settings.appearance.language.description"));
        languageDescription.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        languageDescription.setWrapText(true);

        ToggleGroup languageGroup = new ToggleGroup();
        automaticLanguage.setToggleGroup(languageGroup);
        manualLanguage.setToggleGroup(languageGroup);
        HBox languageModes = new HBox(18, automaticLanguage, manualLanguage);
        languageModes.setAlignment(Pos.CENTER_LEFT);

        languageBox.getItems().setAll(UiText.supportedLocales());
        languageBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String locale) {
                return locale == null ? "" : UiText.localeDisplayName(locale);
            }

            @Override
            public String fromString(String value) {
                return UiText.supportedLocales().stream()
                        .filter(locale -> toString(locale).equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        manualLocale = UiText.normalizeLocale(initialLocale);
        if (!UiText.isSupportedLocale(manualLocale)) {
            manualLocale = UiText.locale();
        }
        if (!UiText.isSupportedLocale(manualLocale)) {
            manualLocale = UiText.DEFAULT_LOCALE;
        }
        boolean manualMode = initialMode == LanguageSelectionMode.MANUAL;
        languageBox.setValue(manualMode ? manualLocale : effectiveLocale());
        manualLanguage.setSelected(manualMode);
        automaticLanguage.setSelected(!manualMode);
        languageBox.setDisable(!manualMode);

        Label languageHint = new Label(UiText.tr("settings.appearance.language.hint"));
        languageHint.getStyleClass().addAll("caml-body-small", "field-hint");
        languageHint.setWrapText(true);

        VBox languageGroupBox = new VBox(16,
                languageHeading,
                languageDescription,
                languageModes,
                ViewSupport.buildField(UiText.tr("settings.appearance.language.label"),
                        languageBox, languageHint)
        );
        languageGroupBox.getStyleClass().add("app-settings-group");
        languageGroupBox.setMaxWidth(Double.MAX_VALUE);

        languageGroup.selectedToggleProperty().addListener((observable, oldValue, selected) -> {
            if (selected == null) {
                return;
            }
            boolean manual = selected == manualLanguage;
            updatingLanguage = true;
            languageBox.setValue(manual ? manualLocale : effectiveLocale());
            languageBox.setDisable(!manual);
            updatingLanguage = false;
            onLanguageChanged.accept(
                    manual ? LanguageSelectionMode.MANUAL : LanguageSelectionMode.AUTO,
                    manualLocale
            );
        });
        languageBox.valueProperty().addListener((observable, oldValue, value) -> {
            if (!updatingLanguage && value != null && manualLanguage.isSelected()) {
                manualLocale = value;
                onLanguageChanged.accept(LanguageSelectionMode.MANUAL, value);
            }
        });

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(languageGroupBox, appearanceGroup);
    }

    private static String effectiveLocale() {
        String current = UiText.normalizeLocale(UiText.locale());
        return UiText.isSupportedLocale(current) ? current : UiText.DEFAULT_LOCALE;
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
