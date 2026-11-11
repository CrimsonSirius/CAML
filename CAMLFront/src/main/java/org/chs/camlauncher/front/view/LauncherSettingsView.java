package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
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
    private final CamlRadioButton fallbackSystem;
    private final CamlRadioButton fallbackDefault;
    private final CamlRadioButton fallbackKey;
    private final CamlRadioButton fallbackCustom;
    private final CamlComboBox<String> fallbackLanguageBox;
    private String manualLocale;
    private String fallbackLocale;
    private boolean updatingLanguage;
    private boolean updatingFallback;

    LauncherSettingsView(LanguageSelectionMode initialMode, String initialLocale,
                         LanguageFallbackMode initialFallbackMode, String initialFallbackLocale,
                         BiConsumer<LanguageSelectionMode, String> onLanguageChanged,
                         BiConsumer<LanguageFallbackMode, String> onLanguageFallbackChanged) {
        themeBox = new CamlComboBox<>();
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setAccessibleText(UiText.tr("settings.appearance.theme.accessible"));

        automaticLanguage = new CamlRadioButton(UiText.tr("settings.appearance.language.auto"));
        manualLanguage = new CamlRadioButton(UiText.tr("settings.appearance.language.manual"));
        languageBox = new CamlComboBox<>();
        languageBox.setMaxWidth(Double.MAX_VALUE);
        languageBox.setAccessibleText(UiText.tr("settings.appearance.language.accessible"));

        fallbackSystem = new CamlRadioButton(
                UiText.tr("settings.appearance.languageFallback.system"));
        fallbackDefault = new CamlRadioButton(
                UiText.tr("settings.appearance.languageFallback.default"));
        fallbackKey = new CamlRadioButton(
                UiText.tr("settings.appearance.languageFallback.key"));
        fallbackCustom = new CamlRadioButton(
                UiText.tr("settings.appearance.languageFallback.custom"));
        fallbackLanguageBox = new CamlComboBox<>();
        fallbackLanguageBox.setMaxWidth(Double.MAX_VALUE);
        fallbackLanguageBox.setAccessibleText(
                UiText.tr("settings.appearance.languageFallback.accessible"));

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

        Label fallbackHeading = new Label(
                UiText.tr("settings.appearance.languageFallback.title"));
        fallbackHeading.getStyleClass().add("app-settings-group-title");

        Label fallbackDescription = new Label(
                UiText.tr("settings.appearance.languageFallback.description"));
        fallbackDescription.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        fallbackDescription.setWrapText(true);

        ToggleGroup fallbackGroup = new ToggleGroup();
        fallbackSystem.setToggleGroup(fallbackGroup);
        fallbackDefault.setToggleGroup(fallbackGroup);
        fallbackKey.setToggleGroup(fallbackGroup);
        fallbackCustom.setToggleGroup(fallbackGroup);
        VBox fallbackModes = new VBox(10, fallbackSystem, fallbackDefault, fallbackKey, fallbackCustom);
        fallbackModes.setAlignment(Pos.CENTER_LEFT);

        fallbackLanguageBox.getItems().setAll(UiText.supportedLocales());
        fallbackLanguageBox.setConverter(new StringConverter<>() {
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
        fallbackLocale = UiText.normalizeLocale(initialFallbackLocale);
        if (!UiText.isSupportedLocale(fallbackLocale)) {
            fallbackLocale = effectiveLocale();
        }
        fallbackLanguageBox.setValue(fallbackLocale);

        LanguageFallbackMode fallbackMode = initialFallbackMode == null
                ? LanguageFallbackMode.KEY
                : initialFallbackMode;
        fallbackSystem.setSelected(fallbackMode == LanguageFallbackMode.SYSTEM);
        fallbackDefault.setSelected(fallbackMode == LanguageFallbackMode.DEFAULT);
        fallbackKey.setSelected(fallbackMode == LanguageFallbackMode.KEY);
        fallbackCustom.setSelected(fallbackMode == LanguageFallbackMode.CUSTOM);
        fallbackLanguageBox.setDisable(fallbackMode != LanguageFallbackMode.CUSTOM);

        Label fallbackHint = new Label(
                UiText.tr("settings.appearance.languageFallback.hint"));
        fallbackHint.getStyleClass().addAll("caml-body-small", "field-hint");
        fallbackHint.setWrapText(true);

        VBox fallbackGroupBox = new VBox(16,
                fallbackHeading,
                fallbackDescription,
                fallbackModes,
                fallbackLanguageBox,
                fallbackHint
        );
        fallbackGroupBox.getStyleClass().add("app-settings-group");
        fallbackGroupBox.setMaxWidth(Double.MAX_VALUE);

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

        fallbackGroup.selectedToggleProperty().addListener((observable, oldValue, selected) -> {
            if (selected == null) {
                return;
            }
            LanguageFallbackMode mode = selected == fallbackDefault
                    ? LanguageFallbackMode.DEFAULT
                    : selected == fallbackKey
                    ? LanguageFallbackMode.KEY
                    : selected == fallbackCustom
                    ? LanguageFallbackMode.CUSTOM
                    : LanguageFallbackMode.SYSTEM;
            updatingFallback = true;
            fallbackLanguageBox.setDisable(mode != LanguageFallbackMode.CUSTOM);
            fallbackLanguageBox.setValue(fallbackLocale);
            updatingFallback = false;
            onLanguageFallbackChanged.accept(mode, fallbackLocale);
        });
        fallbackLanguageBox.valueProperty().addListener((observable, oldValue, value) -> {
            if (!updatingFallback && value != null && fallbackCustom.isSelected()) {
                fallbackLocale = value;
                onLanguageFallbackChanged.accept(LanguageFallbackMode.CUSTOM, value);
            }
        });

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(languageGroupBox, fallbackGroupBox, appearanceGroup);
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
