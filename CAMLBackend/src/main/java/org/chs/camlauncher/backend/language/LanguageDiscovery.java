package org.chs.camlauncher.backend.language;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** 扫描内置和用户新增的可用语言。 */
final class LanguageDiscovery {
    private static final Logger LOGGER = LogManager.getLogger(LanguageDiscovery.class);
    private final Path languageDirectory;

    LanguageDiscovery(Path languageDirectory) {
        this.languageDirectory = languageDirectory;
    }

    List<LanguageDescriptor> scan() {
        List<LanguageDescriptor> result = new ArrayList<>();
        Set<String> acknowledged = new HashSet<>();
        for (String locale : LanguageConstants.BUNDLED_LOCALES) {
            result.add(new LanguageDescriptor(locale, LanguageLocales.displayName(locale), true, false));
            acknowledged.add(locale);
        }

        List<String> custom = new ArrayList<>();
        try (Stream<Path> files = Files.list(languageDirectory)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                if (!Files.isRegularFile(file) || !fileName.endsWith(".json")) {
                    continue;
                }
                String raw = fileName.substring(0, fileName.length() - ".json".length());
                String normalized = LanguageLocales.normalize(raw);
                if (!raw.equals(normalized) || !LanguageLocales.isValid(normalized)) {
                    LOGGER.warn("忽略未规范化的自定义语言文件: {}", file);
                    continue;
                }
                if (acknowledged.add(normalized)) {
                    custom.add(normalized);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("无法扫描语言目录 {}", languageDirectory, e);
        }
        custom.sort(String::compareTo);
        for (String locale : custom) {
            result.add(new LanguageDescriptor(locale, LanguageLocales.displayName(locale), false, true));
        }
        return List.copyOf(result);
    }
}
