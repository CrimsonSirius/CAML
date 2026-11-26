package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageEvent;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageServiceTest {
    /**
     * 一个既不在内置语言清单中、也没有对应内置资源的 locale。
     *
     * <p>回退相关用例必须使用它：内置语言一定会被 {@code LanguageCatalogLoader} 从 classpath 读到，
     * 无法构造“请求的语言不可用”场景，因此不能拿任何内置 locale 充当缺失语言。</p>
     */
    private static final String UNBUNDLED_LOCALE = "sv_se";

    @TempDir
    Path dataDirectory;

    @BeforeEach
    void forceDefaultLocale() {
        System.setProperty("caml.language", "en_us");
    }

    @AfterEach
    void clearLocaleOverride() {
        System.clearProperty("caml.language");
    }

    @Test
    void installsBuiltinsAndDefaultsToEnglish() throws Exception {
        LanguageService service = service();
        LanguageSnapshot snapshot = service.initialize();

        assertEquals("en_us", snapshot.activeLocale());
        assertEquals("Launch game", service.text("home.launch"));
        for (String locale : List.of("zh_cn", "zh_tw", "en_us", "fn_cn",
                "ja_jp", "ko_kr", "fr_fr", "de_de", "ru_ru", "la_la", "bo_cn")) {
            assertTrue(Files.isRegularFile(dataDirectory.resolve("lang/" + locale + ".json")),
                    "缺少内置语言文件: " + locale);
        }
        assertEquals(List.of("zh_cn", "zh_tw", "en_us", "fn_cn",
                        "ja_jp", "ko_kr", "fr_fr", "de_de", "ru_ru", "la_la", "bo_cn"),
                service.availableLocales());
        assertEquals("简体中文", service.displayName("zh_cn"));
        assertEquals("繁體中文", service.displayName("zh_tw"));
        assertEquals("日本語", service.displayName("ja_jp"));
        assertEquals("한국어", service.displayName("ko_kr"));
        assertEquals("Français", service.displayName("fr_fr"));
        assertEquals("Deutsch", service.displayName("de_de"));
        assertEquals("Русский", service.displayName("ru_ru"));
        assertEquals("Latina", service.displayName("la_la"));
        assertEquals("བོད་ཡིག", service.displayName("bo_cn"));
    }

    @Test
    void switchesLanguageAndFormatsPlaceholders() throws Exception {
        LanguageService service = service();
        service.initialize();
        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "zh_cn");

        assertEquals("zh_cn", snapshot.activeLocale());
        assertEquals("启动游戏", service.text("home.launch"));
        assertEquals("当前实例：1.21.5", service.text("home.instance.current", "1.21.5"));
    }

    @Test
    void failedLanguagePersistenceKeepsActiveSnapshot() throws Exception {
        Files.createDirectories(dataDirectory.resolve("settings.properties"));
        LanguageService service = service();
        LanguageSnapshot before = service.initialize();

        assertThrows(IOException.class, () -> service.switchLanguage(
                LanguageSelectionMode.MANUAL, "zh_cn"));

        assertEquals(before.activeLocale(), service.activeLocale());
        assertEquals(LanguageSelectionMode.AUTO, service.selectionMode());
    }

    @Test
    void switchesToTraditionalChinese() throws Exception {
        LanguageService service = service();
        service.initialize();
        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "zh_tw");

        assertEquals("zh_tw", snapshot.activeLocale());
        assertEquals("啟動遊戲", service.text("home.launch"));
        assertEquals("目前實例：1.21.5", service.text("home.instance.current", "1.21.5"));
    }

    @Test
    void switchesToJapaneseAndFormatsPlaceholders() throws Exception {
        LanguageService service = service();
        service.initialize();
        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "ja_jp");

        assertEquals("ja_jp", snapshot.activeLocale());
        assertEquals("ゲームを起動", service.text("home.launch"));
        assertEquals("現在のインスタンス: 1.21.5", service.text("home.instance.current", "1.21.5"));
    }

    @Test
    void repairsBundledFileThatDiffersFromResource() throws Exception {
        Path file = dataDirectory.resolve("lang/zh_cn.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"home.launch\":\"broken\"}");

        LanguageService service = service();
        service.initialize();
        service.switchLanguage(LanguageSelectionMode.MANUAL, "zh_cn");

        assertTrue(Files.size(file) > 100);
        assertEquals("启动游戏", service.text("home.launch"));
    }

    @Test
    void fallsBackToBundledResourceWhenRuntimeFileIsCorrupt() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/fn_cn.json"), "{ broken json");

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "fn_cn");

        assertEquals("fn_cn", snapshot.activeLocale());
        assertEquals("发射比赛", service.text("home.launch"));
    }

    @Test
    void mergesMissingCustomKeysFromDefaultLocale() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.DEFAULT, "en_us");

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("xx_yy", snapshot.activeLocale());
        assertEquals("Custom launch", service.text("home.launch"));
        assertFalse(service.text("nav.home").equals("nav.home"));
    }

    @Test
    void reportsFallbackWhenRequestedLocaleIsUnavailable() throws Exception {
        LanguageService service = service();
        service.initialize();
        service.setLanguageFallback(LanguageFallbackMode.DEFAULT, "en_us");
        List<LanguageEvent> events = new ArrayList<>();
        service.addEventListener(events::add);

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, UNBUNDLED_LOCALE);

        assertEquals(UNBUNDLED_LOCALE, snapshot.requestedLocale());
        assertEquals("en_us", snapshot.activeLocale());
        assertNotNull(snapshot.fallbackReason());
        assertTrue(events.stream().anyMatch(LanguageEvent.Fallback.class::isInstance));
    }

    @Test
    void reportsMissingKeysAndReturnsTheKey() throws Exception {
        LanguageService service = service();
        service.initialize();
        List<LanguageEvent> events = new ArrayList<>();
        service.addEventListener(events::add);

        assertEquals("missing.translation.key", service.text("missing.translation.key"));
        assertTrue(events.stream().anyMatch(LanguageEvent.KeyMissing.class::isInstance));
    }

    @Test
    void rawKeyFallbackDoesNotMergeDefaultLocale() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.KEY, "en_us");

        service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("Custom launch", service.text("home.launch"));
        assertEquals("nav.home", service.text("nav.home"));
    }

    @Test
    void defaultFallbackIgnoresDetectedLanguage() throws Exception {
        System.setProperty("caml.language", "zh_cn");
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.DEFAULT, "en_us");

        service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("Home", service.text("nav.home"));
    }

    @Test
    void automaticFallbackUsesDetectedLanguage() throws Exception {
        System.setProperty("caml.language", "zh_cn");
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.SYSTEM, "en_us");

        service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("主页", service.text("nav.home"));
    }

    @Test
    void customFallbackUsesSelectedLanguage() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.CUSTOM, "zh_cn");

        service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("主页", service.text("nav.home"));
    }

    @Test
    void keepsOnlyNormalizedCustomLocaleNames() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.createDirectories(dataDirectory.resolve("lang"));
        Files.writeString(dataDirectory.resolve("lang/PT-BR.json"), "{\"home.launch\":\"Custom\"}");

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "pt_br");

        assertFalse(snapshot.availableLanguages().stream()
                .anyMatch(descriptor -> descriptor.locale().equals("PT-BR")));
        assertEquals("pt_br", snapshot.activeLocale());
    }

    @Test
    void automaticFallbackHasNoEnglishFallback() throws Exception {
        System.setProperty("caml.language", UNBUNDLED_LOCALE);
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.SYSTEM, "en_us");

        service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("nav.home", service.text("nav.home"));
    }

    @Test
    void customFallbackHasNoEnglishFallback() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.writeString(dataDirectory.resolve("lang/xx_yy.json"),
                "{\"home.launch\":\"Custom launch\"}");
        service.setLanguageFallback(LanguageFallbackMode.CUSTOM, UNBUNDLED_LOCALE);

        service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("nav.home", service.text("nav.home"));
    }

    @Test
    void unbundledLocaleIsReallyNotBundled() {
        assertFalse(LanguageConstants.BUNDLED_LOCALES.contains(UNBUNDLED_LOCALE),
                UNBUNDLED_LOCALE + " 已成为内置语言，回退用例必须换用其他未内置的 locale");
        assertFalse(LanguageService.detectSystemLocale(Locale.forLanguageTag("sv-SE")).equals(UNBUNDLED_LOCALE),
                "系统检测不应把 " + UNBUNDLED_LOCALE + " 识别为可用语言");
    }

    @Test
    void detectsChineseSystemLocalesAndDefaultsOtherwise() {
        assertEquals("zh_cn", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-CN")));
        assertEquals("zh_cn", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-SG")));
        assertEquals("zh_cn", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-Hans")));
        assertEquals("zh_tw", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-TW")));
        assertEquals("zh_tw", LanguageService.detectSystemLocale(Locale.TRADITIONAL_CHINESE));
        assertEquals("zh_tw", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-HK")));
        assertEquals("zh_tw", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-MO")));
        assertEquals("zh_tw", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-Hant")));
        assertEquals("ja_jp", LanguageService.detectSystemLocale(Locale.JAPAN));
        assertEquals("ja_jp", LanguageService.detectSystemLocale(Locale.forLanguageTag("ja-JP")));
        assertEquals("ko_kr", LanguageService.detectSystemLocale(Locale.KOREA));
        assertEquals("ko_kr", LanguageService.detectSystemLocale(Locale.forLanguageTag("ko-KR")));
        assertEquals("fr_fr", LanguageService.detectSystemLocale(Locale.FRANCE));
        assertEquals("fr_fr", LanguageService.detectSystemLocale(Locale.forLanguageTag("fr-CA")));
        assertEquals("de_de", LanguageService.detectSystemLocale(Locale.GERMANY));
        assertEquals("de_de", LanguageService.detectSystemLocale(Locale.forLanguageTag("de-AT")));
        assertEquals("ru_ru", LanguageService.detectSystemLocale(Locale.forLanguageTag("ru-RU")));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.US));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.UK));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.forLanguageTag("la")));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.forLanguageTag("bo-CN")));
        assertEquals("en_us", LanguageService.detectSystemLocale(null));
    }

    private LanguageService service() {
        return new LanguageService(dataDirectory, new SettingsService(dataDirectory));
    }
}
