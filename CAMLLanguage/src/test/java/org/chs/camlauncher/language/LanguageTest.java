package org.chs.camlauncher.language;

import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;
import org.chs.camlauncher.language.generator.lang.BOCNGenerator;
import org.chs.camlauncher.language.generator.lang.DEDEGenerator;
import org.chs.camlauncher.language.generator.lang.ENUSGenerator;
import org.chs.camlauncher.language.generator.lang.FNCNGenerator;
import org.chs.camlauncher.language.generator.lang.FRFRGenerator;
import org.chs.camlauncher.language.generator.lang.JAJPGenerator;
import org.chs.camlauncher.language.generator.lang.KOKRGenerator;
import org.chs.camlauncher.language.generator.lang.LALAGenerator;
import org.chs.camlauncher.language.generator.lang.RURUGenerator;
import org.chs.camlauncher.language.generator.lang.ZHCNGenerator;
import org.chs.camlauncher.language.generator.lang.ZHTWGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 校验开发期生成器与已提交语言资源的一致性。 */
class LanguageTest {
    private static final List<LanguageTranslationGenerator> GENERATORS = List.of(
            new ZHCNGenerator(),
            new ZHTWGenerator(),
            new ENUSGenerator(),
            new FNCNGenerator(),
            new JAJPGenerator(),
            new KOKRGenerator(),
            new FRFRGenerator(),
            new DEDEGenerator(),
            new RURUGenerator(),
            new LALAGenerator(),
            new BOCNGenerator()
    );

    @Test
    void generatedFilesMatchEachOther(@TempDir Path directory) throws Exception {
        Map<String, Map<String, String>> generated = generated(directory);
        Map<String, String> enus = generated.get("en_us");

        assertFalse(enus.isEmpty(), "英文翻译不能为空");
        for (String locale : generated.keySet()) {
            assertEquals(new TreeMap<>(enus).keySet(),
                    new TreeMap<>(generated.get(locale)).keySet(),
                    locale + " 与英文翻译键必须一致");
        }
    }

    @Test
    void generatedFilesMatchBundledResources(@TempDir Path directory) throws Exception {
        Map<String, Map<String, String>> generated = generated(directory);

        assertEquals(Set.copyOf(LanguageConstants.BUNDLED_LOCALES), generated.keySet(),
                "BUNDLED_LOCALES 必须与生成器一一对应");
        for (String locale : LanguageConstants.BUNDLED_LOCALES) {
            assertEquals(generated.get(locale), readBundled(locale),
                    "lang/" + locale + ".json 与生成器输出不一致");
        }
    }

    @Test
    void generatedPlaceholdersMatchEnglish(@TempDir Path directory) throws Exception {
        Map<String, Map<String, String>> generated = generated(directory);
        Map<String, String> enus = generated.get("en_us");

        for (Map.Entry<String, Map<String, String>> locale : generated.entrySet()) {
            for (Map.Entry<String, String> message : locale.getValue().entrySet()) {
                assertEquals(placeholders(enus.get(message.getKey())),
                        placeholders(message.getValue()),
                        locale.getKey() + " 的占位符必须与英文一致: " + message.getKey());
            }
        }
    }

    @Test
    void translationTableRejectsDuplicateKeys() {
        TranslationTable table = new TranslationTable();
        table.put("duplicate.key", "first");

        assertThrows(IllegalArgumentException.class,
                () -> table.put("duplicate.key", "second"));
    }

    private static Map<String, Map<String, String>> generated(Path directory) throws Exception {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (LanguageTranslationGenerator generator : GENERATORS) {
            Path file = generator.generate(directory);
            String locale = file.getFileName().toString().replace(".json", "");
            result.put(locale, readJson(Files.newInputStream(file)));
        }
        return Map.copyOf(result);
    }

    private static Map<String, String> readBundled(String locale) throws Exception {
        InputStream input = LanguageTest.class.getResourceAsStream("/lang/" + locale + ".json");
        if (input == null) {
            throw new IllegalStateException("Missing bundled language: " + locale);
        }
        return readJson(input);
    }

    private static Map<String, String> readJson(InputStream input) throws Exception {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return Map.copyOf(new com.google.gson.Gson().fromJson(reader,
                    new com.google.gson.reflect.TypeToken<LinkedHashMap<String, String>>() {
                    }.getType()));
        }
    }

    private static List<String> placeholders(String template) {
        List<String> result = new ArrayList<>();
        if (template == null) {
            return result;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        Collections.sort(result);
        return result;
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z%]");
}