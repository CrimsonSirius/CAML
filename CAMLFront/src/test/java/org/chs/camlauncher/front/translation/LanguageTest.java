package org.chs.camlauncher.front.translation;

import org.chs.camlauncher.front.translation.generator.ENUSGenerator;
import org.chs.camlauncher.front.translation.generator.ZHCNGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 校验翻译文件与生成器保持一致。 */
class LanguageTest {
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
    void generatedFilesMatchEachOther(@TempDir Path tempDirectory) throws IOException {
        Map<String, String> zhen = generated(tempDirectory, new ZHCNGenerator());
        Map<String, String> enus = generated(tempDirectory, new ENUSGenerator());

        assertFalse(zhen.isEmpty(), "中文翻译不能为空");
        assertEquals(new TreeMap<>(zhen).keySet(), new TreeMap<>(enus).keySet(),
                "中英文翻译键必须一致");
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
}
