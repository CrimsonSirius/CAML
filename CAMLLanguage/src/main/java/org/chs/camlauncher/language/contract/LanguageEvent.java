package org.chs.camlauncher.language.contract;

import java.util.List;

/** 后端语言子系统发布的结构化事件。 */
public sealed interface LanguageEvent permits
        LanguageEvent.StateChanged,
        LanguageEvent.Fallback,
        LanguageEvent.FileInvalid,
        LanguageEvent.FileRepaired,
        LanguageEvent.KeyMissing,
        LanguageEvent.FormatFailed {

    record StateChanged(
            int version,
            String requestedLocale,
            String activeLocale,
            List<LanguageDescriptor> availableLanguages
    ) implements LanguageEvent {
    }

    record Fallback(
            int version,
            String requestedLocale,
            String activeLocale,
            String reason
    ) implements LanguageEvent {
    }

    record FileInvalid(
            int version,
            String locale,
            String fileName,
            String reason,
            Throwable cause
    ) implements LanguageEvent {
    }

    record FileRepaired(
            int version,
            String locale,
            String fileName,
            String reason
    ) implements LanguageEvent {
    }

    record KeyMissing(
            int version,
            String locale,
            String key
    ) implements LanguageEvent {
    }

    record FormatFailed(
            int version,
            String locale,
            String key,
            String reason
    ) implements LanguageEvent {
    }
}
