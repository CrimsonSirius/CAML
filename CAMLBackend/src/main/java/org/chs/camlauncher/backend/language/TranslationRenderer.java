package org.chs.camlauncher.backend.language;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.language.contract.LanguageEvent;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 从已解析快照中取文案并完成占位符格式化。 */
final class TranslationRenderer {
    private static final Logger LOGGER = LogManager.getLogger(TranslationRenderer.class);
    private final LanguageEventPublisher events;
    private final Set<String> missingKeyDiagnostics = ConcurrentHashMap.newKeySet();

    TranslationRenderer(LanguageEventPublisher events) {
        this.events = events;
    }

    String render(LanguageSnapshot snapshot, String key, Object... args) {
        String template = snapshot.messages().get(key);
        if (template == null) {
            String diagnosticKey = snapshot.activeLocale() + "\u0000" + key;
            if (missingKeyDiagnostics.add(diagnosticKey)) {
                LOGGER.warn("缺少翻译键 locale={} key={}", snapshot.activeLocale(), key);
                events.publish(new LanguageEvent.KeyMissing(
                        snapshot.version(), snapshot.activeLocale(), key));
            }
            return key;
        }
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (RuntimeException e) {
            LOGGER.warn("翻译格式化失败 locale={} key={}", snapshot.activeLocale(), key, e);
            events.publish(new LanguageEvent.FormatFailed(
                    snapshot.version(), snapshot.activeLocale(), key, e.getMessage()));
            return template;
        }
    }
}
