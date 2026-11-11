package org.chs.camlauncher.front.text;

import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageTextProvider;

import java.util.List;
import java.util.Objects;

/** 前端显示适配器；所有语言逻辑由后端 LanguageTextProvider 实现。 */
public final class UiText {
    public static final String DEFAULT_LOCALE = LanguageConstants.DEFAULT_LOCALE;

    private static volatile LanguageTextProvider provider;

    private UiText() {
    }

    public static void initialize(LanguageTextProvider languageProvider) {
        provider = Objects.requireNonNull(languageProvider, "languageProvider");
    }

    public static String tr(String key, Object... args) {
        return requireProvider().text(key, args);
    }

    public static String locale() {
        return requireProvider().activeLocale();
    }

    public static List<String> supportedLocales() {
        return requireProvider().availableLocales();
    }

    public static boolean isSupportedLocale(String locale) {
        return requireProvider().supportsLocale(locale);
    }

    public static String normalizeLocale(String locale) {
        return requireProvider().normalizeLocale(locale);
    }

    public static String localeDisplayName(String locale) {
        return requireProvider().displayName(locale);
    }

    private static LanguageTextProvider requireProvider() {
        LanguageTextProvider current = provider;
        if (current == null) {
            throw new IllegalStateException("UiText is not initialized");
        }
        return current;
    }
}
