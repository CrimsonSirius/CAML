package org.chs.camlauncher.language;

import org.chs.camlauncher.language.generator.ENUSGenerator;
import org.chs.camlauncher.language.generator.FNCNGenerator;
import org.chs.camlauncher.language.generator.ZHCNGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 校验翻译文件与生成器保持一致。 */
class LanguageTest {
    @TempDir
    Path dataDirectory;

    @BeforeEach
    void initializeLanguageDirectory() throws IOException {
        Language.initialize(dataDirectory);
    }

    @AfterEach
    void resetLocale() {
        Language.setLocale("zh_cn");
    }

    @Test
    void loadsChineseTranslations() {
        Language.setLocale("zh_cn");
        assertEquals("zh_cn", Language.locale());
        assertEquals("启动游戏", Language.tr("home.launch"));
    }

    @Test
    void switchesLanguage() {
        Language.setLocale("en_us");
        assertEquals("Launch game", Language.tr("home.launch"));
        assertNotEquals("主页", Language.tr("nav.home"));
    }

    @Test
    void formatsPlaceholders() {
        Language.setLocale("zh_cn");
        assertEquals("当前实例：1.21.5", Language.tr("home.instance.current", "1.21.5"));
    }

    @Test
    void fallsBackToKeyWhenMissing() {
        Language.setLocale("zh_cn");
        assertEquals("missing.translation.key", Language.tr("missing.translation.key"));
    }

    @Test
    void keepsTemplateWhenFormattingFails() {
        Language.setLocale("zh_cn");
        assertEquals("启动游戏", Language.tr("home.launch", "unexpected"));
    }

    @Test
    void exposesSupportedLocalesAndNormalization() {
        assertEquals(List.of("zh_cn", "en_us", "fn_cn"), Language.supportedLocales());
        assertTrue(Language.isSupportedLocale("EN-US"));
        assertEquals("en_us", Language.normalizeLocale("EN-US"));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("lang").resolve("zh_cn.json")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("lang").resolve("en_us.json")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("lang").resolve("fn_cn.json")));
    }

    @Test
    void localeDisplayNamesAreHardcodedAndIgnoreLanguageFiles() throws IOException {
        List<String> expected = List.of("简体中文", "English", "地道中国话");

        Language.setLocale("zh_cn");
        assertEquals(expected, displayNames());
        Language.setLocale("en_us");
        assertEquals(expected, displayNames());
        Language.setLocale("fn_cn");
        assertEquals(expected, displayNames());

        // 翻译文件里写错语言名也不会影响下拉框
        Files.writeString(dataDirectory.resolve("lang").resolve("zh_cn.json"),
                "{\"settings.appearance.language.zh_cn\":\"Wrong name\"}");
        Language.setLocale("zh_cn");
        assertEquals(expected, displayNames());

        assertEquals("简体中文", Language.localeDisplayName("ZH-CN"));
        assertEquals("fr_fr", Language.localeDisplayName("fr_fr"));
    }

    @Test
    void readsEditedLanguageFileFromDataDirectory() throws IOException {
        Path file = dataDirectory.resolve("lang").resolve("en_us.json");
        Files.writeString(file, "{\"home.launch\":\"Edited launch\"}");

        Language.setLocale("en_us");

        assertEquals("Edited launch", Language.tr("home.launch"));
    }

    @Test
    void generatedFilesMatchEachOther(@TempDir Path tempDirectory) throws IOException {
        Map<String, String> zhen = generated(tempDirectory, new ZHCNGenerator());
        Map<String, String> enus = generated(tempDirectory, new ENUSGenerator());
        Map<String, String> fncn = generated(tempDirectory, new FNCNGenerator());

        assertFalse(zhen.isEmpty(), "中文翻译不能为空");
        assertEquals(new TreeMap<>(zhen).keySet(), new TreeMap<>(enus).keySet(),
                "中英文翻译键必须一致");
        assertEquals(new TreeMap<>(zhen).keySet(), new TreeMap<>(fncn).keySet(),
                "各语言翻译键必须一致");
    }

    @Test
    void generatedFilesMatchBundledResources(@TempDir Path tempDirectory) throws IOException {
        Map<String, String> generated = generated(tempDirectory, new ZHCNGenerator());
        Language.setLocale("zh_cn");
        for (String key : generated.keySet()) {
            assertEquals(generated.get(key), Language.tr(key),
                    "lang/zh_cn.json 与 ZHCNGenerator 不一致：" + key);
        }
        assertTrue(generated.containsKey("nav.home"));
    }

    private static Map<String, String> generated(Path directory, LanguageTranslationGenerator generator)
            throws IOException {
        Path file = generator.generate(directory);
        return Map.copyOf(readJson(file));
    }

    private static Map<String, String> readJson(Path file) throws IOException {
        String json = java.nio.file.Files.readString(file);
        return new com.google.gson.Gson().fromJson(json, new com.google.gson.reflect.TypeToken<
                java.util.LinkedHashMap<String, String>>() {
        }.getType());
    }

    private static List<String> displayNames() {
        return Language.supportedLocales().stream().map(Language::localeDisplayName).toList();
    }
}