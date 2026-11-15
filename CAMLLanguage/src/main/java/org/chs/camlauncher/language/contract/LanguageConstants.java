package org.chs.camlauncher.language.contract;

import java.util.List;

/** 语言模块共享常量，不包含运行时行为。 */
public final class LanguageConstants {
    public static final String DEFAULT_LOCALE = "en_us";
    public static final String SIMPLIFIED_CHINESE_LOCALE = "zh_cn";
    public static final String TRADITIONAL_CHINESE_LOCALE = "zh_tw";
    public static final String JAPANESE_LOCALE = "ja_jp";
    public static final String KOREAN_LOCALE = "ko_kr";
    public static final String FRENCH_LOCALE = "fr_fr";
    public static final String GERMAN_LOCALE = "de_de";
    public static final String RUSSIAN_LOCALE = "ru_ru";
    public static final String LATIN_LOCALE = "la_la";
    public static final String TIBETAN_LOCALE = "bo_cn";
    public static final String LANGUAGE_OVERRIDE_PROPERTY = "caml.language";
    public static final List<String> BUNDLED_LOCALES = List.of(
            "zh_cn", "zh_tw", "en_us", "fn_cn",
            "ja_jp", "ko_kr", "fr_fr", "de_de", "ru_ru", "la_la", "bo_cn");

    private LanguageConstants() {
    }
}