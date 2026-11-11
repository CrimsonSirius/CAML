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
    private static final Map<String, String> NATIVE_NAMES = Map.of(
            "zh_cn", "简体中文",
            "en_us", "English",
            "fn_cn", "地道中国话"
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

    static String detectSystemLocale(Locale systemLocale) {
        if (systemLocale == null || !CHINESE_LANGUAGE.equalsIgnoreCase(systemLocale.getLanguage())) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        if (TRADITIONAL_SCRIPT.equalsIgnoreCase(systemLocale.getScript())) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        String country = systemLocale.getCountry().toUpperCase(Locale.ROOT);
        return TRADITIONAL_REGIONS.contains(country)
                ? LanguageConstants.DEFAULT_LOCALE
                : LanguageConstants.SIMPLIFIED_CHINESE_LOCALE;
    }
}
