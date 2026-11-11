package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.language.contract.LanguageEvent;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageServiceTest {
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
        assertTrue(Files.isRegularFile(dataDirectory.resolve("lang/en_us.json")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("lang/zh_cn.json")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("lang/fn_cn.json")));
        assertEquals(List.of("zh_cn", "en_us", "fn_cn"), service.availableLocales());
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

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "xx_yy");

        assertEquals("xx_yy", snapshot.activeLocale());
        assertEquals("Custom launch", service.text("home.launch"));
        assertFalse(service.text("nav.home").equals("nav.home"));
    }

    @Test
    void reportsFallbackWhenRequestedLocaleIsUnavailable() throws Exception {
        LanguageService service = service();
        service.initialize();
        List<LanguageEvent> events = new ArrayList<>();
        service.addEventListener(events::add);

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "fr_fr");

        assertEquals("fr_fr", snapshot.requestedLocale());
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
    void keepsOnlyNormalizedCustomLocaleNames() throws Exception {
        LanguageService service = service();
        service.initialize();
        Files.createDirectories(dataDirectory.resolve("lang"));
        Files.writeString(dataDirectory.resolve("lang/PT-BR.json"), "{\"home.launch\":\"Custom\"}");

        LanguageSnapshot snapshot = service.switchLanguage(LanguageSelectionMode.MANUAL, "pt_br");

        assertFalse(snapshot.availableLanguages().stream()
                .anyMatch(descriptor -> descriptor.locale().equals("PT-BR")));
        assertEquals("en_us", snapshot.activeLocale());
    }

    @Test
    void detectsOnlySimplifiedChineseSystemLocale() {
        assertEquals("zh_cn", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-CN")));
        assertEquals("zh_cn", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-SG")));
        assertEquals("zh_cn", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-Hans")));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.US));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.TRADITIONAL_CHINESE));
        assertEquals("en_us", LanguageService.detectSystemLocale(Locale.forLanguageTag("zh-Hant")));
        assertEquals("en_us", LanguageService.detectSystemLocale(null));
    }

    private LanguageService service() {
        return new LanguageService(dataDirectory, new SettingsService(dataDirectory));
    }
}
