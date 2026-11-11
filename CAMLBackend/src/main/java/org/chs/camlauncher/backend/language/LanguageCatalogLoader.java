package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageErrorCode;
import org.chs.camlauncher.language.contract.LanguageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 加载单个语言并按默认语言补齐缺失键。 */
final class LanguageCatalogLoader {
    private final Path languageDirectory;
    private final Consumer<InvalidFile> diagnostics;

    LanguageCatalogLoader(Path languageDirectory, Consumer<InvalidFile> diagnostics) {
        this.languageDirectory = languageDirectory;
        this.diagnostics = diagnostics;
    }

    Catalog load(String requestedLocale) {
        Map<String, String> requested = loadLocaleMap(requestedLocale);
        Map<String, String> defaults = requestedLocale.equals(LanguageConstants.DEFAULT_LOCALE)
                ? requested
                : loadLocaleMap(LanguageConstants.DEFAULT_LOCALE);
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        merged.putAll(requested);
        if (merged.isEmpty()) {
            throw new LanguageException(
                    LanguageErrorCode.BOOTSTRAP_FAILED, requestedLocale, null, null);
        }

        boolean fellBack = requested.isEmpty()
                && !requestedLocale.equals(LanguageConstants.DEFAULT_LOCALE);
        String activeLocale = fellBack ? LanguageConstants.DEFAULT_LOCALE : requestedLocale;
        String reason = fellBack ? "requested-locale-unavailable" : null;
        return new Catalog(activeLocale, reason, merged);
    }

    private Map<String, String> loadLocaleMap(String locale) {
        if (!LanguageLocales.isValid(locale)) {
            diagnostics.accept(new InvalidFile(locale, null, "invalid-locale", null));
            return Map.of();
        }
        Path runtime = LanguageLocales.runtimeFile(languageDirectory, locale);
        if (runtime != null && Files.isRegularFile(runtime)) {
            try {
                Map<String, String> parsed = LanguageCatalogParser.parse(
                        Files.readAllBytes(runtime), runtime.getFileName().toString());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
                diagnostics.accept(new InvalidFile(
                        locale, runtime.getFileName().toString(), "empty-catalog", null));
            } catch (IOException | RuntimeException e) {
                diagnostics.accept(new InvalidFile(
                        locale, runtime.getFileName().toString(), "invalid-json", e));
            }
        }
        try {
            return LanguageCatalogParser.parse(
                    BundledLanguageResources.read(locale), locale + ".json");
        } catch (IOException | RuntimeException e) {
            diagnostics.accept(new InvalidFile(
                    locale, locale + ".json", "bundled-resource-unreadable", e));
            return Map.of();
        }
    }

    record Catalog(String activeLocale, String fallbackReason, Map<String, String> messages) {
    }

    record InvalidFile(String locale, String fileName, String reason, Throwable cause) {
    }
}
