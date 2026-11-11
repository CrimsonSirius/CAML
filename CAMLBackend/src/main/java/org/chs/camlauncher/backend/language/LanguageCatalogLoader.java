package org.chs.camlauncher.backend.language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
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

    Catalog load(String requestedLocale, List<String> fallbackLocales) {
        Map<String, String> merged = new LinkedHashMap<>();
        String activeFallback = null;
        for (String fallbackLocale : fallbackLocales) {
            Map<String, String> fallback = loadLocaleMap(fallbackLocale);
            if (!fallback.isEmpty()) {
                merged.putAll(fallback);
                activeFallback = fallbackLocale;
            }
        }

        Map<String, String> requested = loadLocaleMap(requestedLocale);
        merged.putAll(requested);
        String activeLocale = !requested.isEmpty()
                ? requestedLocale
                : activeFallback == null ? requestedLocale : activeFallback;
        String reason = requested.isEmpty()
                ? fallbackLocales.isEmpty() ? "bare-key-mode" : "requested-locale-unavailable"
                : null;
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
