package org.chs.camlauncher.backend.launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.language.contract.LanguageConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Pattern;

/** 首次启动实例时把游戏语言设置为启动器当前语言。 */
final class GameLanguageInitializer {
    private static final Logger LOGGER = LogManager.getLogger(GameLanguageInitializer.class);
    private static final Pattern LOCALE_PATTERN =
            Pattern.compile("[a-z]{2,3}(?:_[a-z0-9]{2,8})*");
    private static final String OPTIONS_FILE = "options.txt";
    private static final String LANGUAGE_PREFIX = "lang:";

    private GameLanguageInitializer() {
    }

    static void apply(Path gameDataDirectory, String launcherLocale) {
        Path options = gameDataDirectory.resolve(OPTIONS_FILE).toAbsolutePath().normalize();
        String language = minecraftLanguage(launcherLocale);
        try {
            if (Files.isRegularFile(options)) {
                String content = Files.readString(options, StandardCharsets.UTF_8);
                if (hasLanguage(content)) {
                    return;
                }
                String separator = content.isEmpty()
                        || content.endsWith("\n")
                        || content.endsWith("\r")
                        ? ""
                        : System.lineSeparator();
                writeAtomically(options, content + separator
                        + LANGUAGE_PREFIX + language + System.lineSeparator());
            } else {
                writeAtomically(options, LANGUAGE_PREFIX + language + System.lineSeparator());
            }
            LOGGER.info("首次启动自动设置游戏语言: {} -> {}", launcherLocale, language);
        } catch (IOException e) {
            // 语言初始化失败不能阻止游戏启动。
            LOGGER.warn("无法写入游戏语言设置: {}", options, e);
        }
    }

    static String minecraftLanguage(String launcherLocale) {
        String normalized = launcherLocale == null
                ? ""
                : launcherLocale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("fn_cn".equals(normalized)) {
            return LanguageConstants.SIMPLIFIED_CHINESE_LOCALE;
        }
        if (!LOCALE_PATTERN.matcher(normalized).matches()) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        return normalized;
    }

    private static boolean hasLanguage(String content) {
        return content.lines().anyMatch(line -> line.trim().startsWith(LANGUAGE_PREFIX));
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "caml-options-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
