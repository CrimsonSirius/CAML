package org.chs.camlauncher.backend.settings;

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
}
