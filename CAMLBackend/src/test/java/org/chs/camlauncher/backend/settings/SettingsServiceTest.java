package org.chs.camlauncher.backend.settings;

import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void rejectsAutomaticVersionIsolationPersistence() {
        SettingsService service = new SettingsService(dataDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setVersionIsolationMode(VersionIsolationMode.AUTO)
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
}
