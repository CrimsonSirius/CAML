package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.language.contract.LanguageConstants;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 语言标识规范化、系统语言检测和安全路径解析。 */
final class LanguageLocales {
    static final Pattern LOCALE_PATTERN = Pattern.compile("[a-z]{2,3}(?:_[a-z0-9]{2,8})*");
    private static final String CHINESE_LANGUAGE = "zh";
    private static final String TRADITIONAL_SCRIPT = "Hant";
    private static final Set<String> TRADITIONAL_REGIONS = Set.of("TW", "HK", "MO");
    /** 参与系统语言检测的语言代码与内置 locale 的对应关系；拉丁文与藏文不参与检测。 */
    private static final Map<String, String> DETECTED_LANGUAGE_LOCALES = Map.of(
            "ja", LanguageConstants.JAPANESE_LOCALE,
            "ko", LanguageConstants.KOREAN_LOCALE,
            "fr", LanguageConstants.FRENCH_LOCALE,
            "de", LanguageConstants.GERMAN_LOCALE,
            "ru", LanguageConstants.RUSSIAN_LOCALE
    );
    private static final Map<String, String> NATIVE_NAMES = Map.ofEntries(
            Map.entry("zh_cn", "简体中文"),
            Map.entry("zh_tw", "繁體中文"),
            Map.entry("en_us", "English"),
            Map.entry("fn_cn", "地道中国话"),
            Map.entry("ja_jp", "日本語"),
            Map.entry("ko_kr", "한국어"),
            Map.entry("fr_fr", "Français"),
            Map.entry("de_de", "Deutsch"),
            Map.entry("ru_ru", "Русский"),
            Map.entry("la_la", "Latina"),
            Map.entry("bo_cn", "བོད་ཡིག")
    );

    private LanguageLocales() {
    }

    static String normalize(String locale) {
        if (locale == null || locale.isBlank()) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    static boolean isValid(String locale) {
        return locale != null && LOCALE_PATTERN.matcher(locale).matches();
    }

    static Path runtimeFile(Path languageDirectory, String locale) {
        if (!isValid(locale)) {
            return null;
        }
        Path root = languageDirectory.toAbsolutePath().normalize();
        Path resolved = root.resolve(locale + ".json").normalize();
        return resolved.getParent().equals(root) ? resolved : null;
    }

    static String displayName(String locale) {
        String normalized = normalize(locale);
        return NATIVE_NAMES.getOrDefault(normalized, normalized);
    }

    static String detectLocale() {
        String override = System.getProperty(LanguageConstants.LANGUAGE_OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return normalize(override);
        }
        return detectSystemLocale(Locale.getDefault());
    }

    /**
     * 检测系统语言：简体中文系统使用 {@code zh_cn}，繁体中文系统（脚本 {@code Hant} 或地区
     * {@code TW}/{@code HK}/{@code MO}）使用 {@code zh_tw}，日语、韩语、法语、德语、俄语系统分别使用
     * 对应内置 locale；其余语言（含英语，以及不参与检测的拉丁文与藏文）一律使用默认语言。
     */
    static String detectSystemLocale(Locale systemLocale) {
        if (systemLocale == null) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        String language = systemLocale.getLanguage();
        if (language == null || language.isBlank()) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        if (CHINESE_LANGUAGE.equalsIgnoreCase(language)) {
            return isTraditionalChinese(systemLocale)
                    ? LanguageConstants.TRADITIONAL_CHINESE_LOCALE
                    : LanguageConstants.SIMPLIFIED_CHINESE_LOCALE;
        }
        return DETECTED_LANGUAGE_LOCALES.getOrDefault(
                language.toLowerCase(Locale.ROOT), LanguageConstants.DEFAULT_LOCALE);
    }

    private static boolean isTraditionalChinese(Locale systemLocale) {
        if (TRADITIONAL_SCRIPT.equalsIgnoreCase(systemLocale.getScript())) {
            return true;
        }
        String country = systemLocale.getCountry().toUpperCase(Locale.ROOT);
        return TRADITIONAL_REGIONS.contains(country);
    }
}