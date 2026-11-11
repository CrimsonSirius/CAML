package org.chs.camlauncher.language.contract;

import java.util.List;

/** 前端显示层使用的只读语言能力；实现必须由后端提供。 */
public interface LanguageTextProvider {
    String text(String key, Object... args);

    String activeLocale();

    String requestedLocale();

    String defaultLocale();

    List<LanguageDescriptor> availableLanguages();

    List<String> availableLocales();

    boolean supportsLocale(String locale);

    String normalizeLocale(String locale);

    String displayName(String locale);
}
