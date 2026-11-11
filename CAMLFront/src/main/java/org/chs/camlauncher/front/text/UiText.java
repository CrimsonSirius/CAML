package org.chs.camlauncher.front.text;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageTextProvider;

import java.util.List;
import java.util.Objects;

/** 前端显示适配器；所有语言逻辑由后端 LanguageTextProvider 实现。 */
public final class UiText {
    private static final Logger LOGGER = LogManager.getLogger(UiText.class);
    public static final String DEFAULT_LOCALE = LanguageConstants.DEFAULT_LOCALE;

    private static volatile LanguageTextProvider provider;

    private UiText() {
    }

    public static void initialize(LanguageTextProvider languageProvider) {
        if (languageProvider == null) {
            LOGGER.error("初始化 UiText 失败：LanguageTextProvider 为 null");
        }
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
            LOGGER.error("UiText 尚未初始化，无法获取后端语言文本");
            throw new IllegalStateException("UiText is not initialized");
        }
        return current;
    }
}
