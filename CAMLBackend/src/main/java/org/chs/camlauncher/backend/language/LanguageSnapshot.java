package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.language.contract.LanguageDescriptor;

import java.util.List;
import java.util.Map;

/** 一次语言切换后的不可变状态快照。 */
public record LanguageSnapshot(
        int version,
        String requestedLocale,
        String activeLocale,
        String fallbackReason,
        List<LanguageDescriptor> availableLanguages,
        Map<String, String> messages
) {
}
