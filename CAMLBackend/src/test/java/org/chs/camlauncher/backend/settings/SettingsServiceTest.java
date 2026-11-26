package org.chs.camlauncher.backend.settings;

import org.chs.camlauncher.backend.api.WindowState;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsServiceTest {
    @TempDir
    Path dataDirectory;

    @Test
    void returnsDefaultWhenFileIsMissing() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertEquals(SettingsService.DEFAULT_MAX_MEMORY_MB, service.maxMemoryMb());
    }

    @Test
    void persistsMaxMemoryAcrossInstances() throws Exception {
        new SettingsService(dataDirectory).setMaxMemoryMb(8192);

        assertEquals(8192, new SettingsService(dataDirectory).maxMemoryMb());
    }

    @Test
    void fallsBackToDefaultForInvalidValue() throws Exception {
        Files.writeString(dataDirectory.resolve("settings.properties"), "maxMemoryMb=oops\n");

        assertEquals(SettingsService.DEFAULT_MAX_MEMORY_MB,
                new SettingsService(dataDirectory).maxMemoryMb());
    }

    @Test
    void rejectsMemoryBelowMinimum() {
        SettingsService service = new SettingsService(dataDirectory);

        assertThrows(IllegalArgumentException.class, () -> service.setMaxMemoryMb(511));
    }

    @Test
    void jvmArgumentsDefaultToEmptyAndPersist() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertEquals(SettingsService.DEFAULT_JVM_ARGUMENTS, service.jvmArguments());

        service.setJvmArguments("  -XX:+UseG1GC -Dname=\"hello world\"  ");

        assertEquals(
                "-XX:+UseG1GC -Dname=\"hello world\"",
                new SettingsService(dataDirectory).jvmArguments()
        );
    }

    @Test
    void clearingJvmArgumentsRemovesSetting() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);
        service.setJvmArguments("-XX:+UseG1GC");
        service.setJvmArguments("  ");

        assertEquals(SettingsService.DEFAULT_JVM_ARGUMENTS,
                new SettingsService(dataDirectory).jvmArguments());
    }

    @Test
    void persistsGameDirectoryAcrossInstances() throws Exception {
        Path gameDirectory = dataDirectory.resolve("games").resolve(".minecraft");
        new SettingsService(dataDirectory).setGameDirectory(gameDirectory);

        assertEquals(
                gameDirectory.toAbsolutePath().normalize(),
                new SettingsService(dataDirectory).gameDirectory().orElseThrow()
        );
    }

    @Test
    void missingGameDirectoryReturnsEmpty() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertTrue(service.gameDirectory().isEmpty());
    }

    @Test
    void defaultsToFullVersionIsolation() throws Exception {
        assertEquals(
                VersionIsolationMode.FULL,
                new SettingsService(dataDirectory).versionIsolationMode()
        );
    }

    @Test
    void persistsVersionIsolationAcrossInstances() throws Exception {
        new SettingsService(dataDirectory).setVersionIsolationMode(VersionIsolationMode.NONE);

        assertEquals(
                VersionIsolationMode.NONE,
                new SettingsService(dataDirectory).versionIsolationMode()
        );
    }

    @Test
    void invalidVersionIsolationFallsBackToFull() throws Exception {
        Files.writeString(dataDirectory.resolve("settings.properties"), "versionIsolation=oops\n");

        assertEquals(
                VersionIsolationMode.FULL,
                new SettingsService(dataDirectory).versionIsolationMode()
        );
    }

    @Test
    void defaultsToDefaultDownloadConcurrency() throws Exception {
        assertEquals(
                SettingsService.DEFAULT_DOWNLOAD_CONCURRENCY,
                new SettingsService(dataDirectory).downloadConcurrency()
        );
    }

    @Test
    void persistsDownloadConcurrencyAcrossInstances() throws Exception {
        new SettingsService(dataDirectory).setDownloadConcurrency(64);

        assertEquals(64, new SettingsService(dataDirectory).downloadConcurrency());
    }

    @Test
    void invalidDownloadConcurrencyFallsBackToDefault() throws Exception {
        Files.writeString(dataDirectory.resolve("settings.properties"), "downloadConcurrency=oops\n");

        assertEquals(
                SettingsService.DEFAULT_DOWNLOAD_CONCURRENCY,
                new SettingsService(dataDirectory).downloadConcurrency()
        );
    }

    @Test
    void rejectsDownloadConcurrencyOutsideRange() {
        SettingsService service = new SettingsService(dataDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setDownloadConcurrency(SettingsService.MIN_DOWNLOAD_CONCURRENCY - 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.setDownloadConcurrency(SettingsService.MAX_DOWNLOAD_CONCURRENCY + 1)
        );
    }

    @Test
    void defaultsToMirrorDownloadSource() throws Exception {
        assertEquals(
                DownloadSourceMode.MIRROR,
                new SettingsService(dataDirectory).downloadSourceMode()
        );
        assertEquals("", new SettingsService(dataDirectory).customDownloadApi());
    }

    @Test
    void persistsDownloadSourceAndCustomApiAcrossInstances() throws Exception {
        new SettingsService(dataDirectory).setDownloadSource(
                DownloadSourceMode.CUSTOM, "https://mirror.example/api/");
        new SettingsService(dataDirectory).setDownloadSource(
                DownloadSourceMode.OFFICIAL, "https://mirror.example/api/");

        SettingsService loaded = new SettingsService(dataDirectory);
        assertEquals(DownloadSourceMode.OFFICIAL, loaded.downloadSourceMode());
        assertEquals("https://mirror.example/api/", loaded.customDownloadApi());
    }

    @Test
    void missingLastLaunchedInstanceReturnsEmpty() throws Exception {
        assertTrue(new SettingsService(dataDirectory).lastLaunchedInstanceId().isEmpty());
    }

    @Test
    void persistsLastLaunchedInstanceAcrossInstances() throws Exception {
        new SettingsService(dataDirectory).setLastLaunchedInstanceId("我的 1.21.4");

        assertEquals(
                "我的 1.21.4",
                new SettingsService(dataDirectory).lastLaunchedInstanceId().orElseThrow()
        );
    }

    @Test
    void serializesConcurrentWritesWithoutLosingUpdates() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> memory = executor.submit(() -> {
                start.await();
                for (int index = 0; index < 100; index++) {
                    service.setMaxMemoryMb(8192);
                }
                return null;
            });
            Future<?> lastInstance = executor.submit(() -> {
                start.await();
                for (int index = 0; index < 100; index++) {
                    service.setLastLaunchedInstanceId("并发实例");
                }
                return null;
            });

            start.countDown();
            memory.get(10, TimeUnit.SECONDS);
            lastInstance.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        SettingsService loaded = new SettingsService(dataDirectory);
        assertEquals(8192, loaded.maxMemoryMb());
        assertEquals("并发实例", loaded.lastLaunchedInstanceId().orElseThrow());
    }

    @Test
    void rejectsBlankLastLaunchedInstance() {
        SettingsService service = new SettingsService(dataDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setLastLaunchedInstanceId("  ")
        );
    }

    @Test
    void defaultsToAutomaticJavaSelection() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertEquals(JavaSelectionMode.AUTO, service.javaSelectionMode());
        assertTrue(service.selectedJavaPath().isEmpty());
        assertTrue(service.manualJavaPaths().isEmpty());
    }

    @Test
    void persistsJavaSelectionAndDeduplicatesManualPaths() throws Exception {
        Path first = dataDirectory.resolve("java-a/bin/java").toAbsolutePath().normalize();
        Path second = dataDirectory.resolve("java-b/bin/java").toAbsolutePath().normalize();
        SettingsService service = new SettingsService(dataDirectory);
        service.addManualJavaPath(first);
        service.addManualJavaPath(second);
        service.addManualJavaPath(first);
        service.setJavaSelection(JavaSelectionMode.MANUAL, second);

        SettingsService loaded = new SettingsService(dataDirectory);
        assertEquals(JavaSelectionMode.MANUAL, loaded.javaSelectionMode());
        assertEquals(second, loaded.selectedJavaPath().orElseThrow());
        assertEquals(List.of(first, second), loaded.manualJavaPaths());
    }

    @Test
    void defaultsToAutomaticLanguageSelection() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertEquals(LanguageSelectionMode.AUTO, service.languageSelectionMode());
        assertTrue(service.language().isEmpty());
    }

    @Test
    void persistsLanguageSelectionAndNormalizesLocale() throws Exception {
        new SettingsService(dataDirectory).setLanguage(LanguageSelectionMode.MANUAL, "EN-US");

        SettingsService loaded = new SettingsService(dataDirectory);
        assertEquals(LanguageSelectionMode.MANUAL, loaded.languageSelectionMode());
        assertEquals("en_us", loaded.language().orElseThrow());
    }

    @Test
    void persistsLanguageFallbackSelection() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertEquals(LanguageFallbackMode.KEY, service.languageFallbackMode());
        assertTrue(service.languageFallbackLocale().isEmpty());

        service.setLanguageFallback(LanguageFallbackMode.CUSTOM, "ZH-CN");

        SettingsService loaded = new SettingsService(dataDirectory);
        assertEquals(LanguageFallbackMode.CUSTOM, loaded.languageFallbackMode());
        assertEquals("zh_cn", loaded.languageFallbackLocale().orElseThrow());
    }

    @Test
    void readsLegacyAutoFallbackModeAsSystem() throws Exception {
        Files.writeString(dataDirectory.resolve("settings.properties"),
                "languageFallbackMode=auto\n");

        assertEquals(LanguageFallbackMode.SYSTEM,
                new SettingsService(dataDirectory).languageFallbackMode());
    }

    @Test
    void autoGameLanguageDefaultsTrueAndPersists() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        assertTrue(service.autoGameLanguage());
        service.setAutoGameLanguage(false);
        assertFalse(new SettingsService(dataDirectory).autoGameLanguage());
    }

    @Test
    void persistsWindowState() throws Exception {
        SettingsService service = new SettingsService(dataDirectory);

        service.setWindowState(new WindowState(120, 80, 1280, 720, true));

        WindowState loaded = new SettingsService(dataDirectory).windowState().orElseThrow();
        assertEquals(120, loaded.x());
        assertEquals(80, loaded.y());
        assertEquals(1280, loaded.width());
        assertEquals(720, loaded.height());
        assertTrue(loaded.maximized());
    }

    @Test
    void ignoresInvalidWindowState() throws Exception {
        Files.writeString(dataDirectory.resolve("settings.properties"),
                "windowX=bad\nwindowY=80\nwindowWidth=1280\nwindowHeight=720\n");

        assertTrue(new SettingsService(dataDirectory).windowState().isEmpty());
    }

    @Test
    void persistsDisabledModsPerInstanceAndGameDirectory() throws Exception {
        Path game = dataDirectory.resolve("game");
        SettingsService service = new SettingsService(dataDirectory);
        service.setModDisabled(game, "cuteJKpww", "sodium.jar", true);

        SettingsService loaded = new SettingsService(dataDirectory);
        assertTrue(loaded.disabledMods(game, "cuteJKpww").contains("sodium.jar"));
        assertTrue(loaded.disabledMods(game, "other").isEmpty());
        assertTrue(loaded.disabledMods(dataDirectory.resolve("other-game"), "cuteJKpww").isEmpty());

        loaded.setModDisabled(game, "cuteJKpww", "sodium.jar", false);
        assertTrue(new SettingsService(dataDirectory)
                .disabledMods(game, "cuteJKpww").isEmpty());
    }
}
