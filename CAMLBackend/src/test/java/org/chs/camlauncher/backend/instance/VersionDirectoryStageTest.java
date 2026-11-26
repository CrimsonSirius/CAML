package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionDirectoryStageTest {
    @TempDir
    Path gameDirectory;

    @Test
    void commitsStagedDirectoryToFinalInstancePath() throws Exception {
        InstalledVersion installed;
        try (VersionDirectoryStage stage = VersionDirectoryStage.create(gameDirectory, "demo")) {
            Files.writeString(stage.directory().resolve("demo.json"), "{}");
            installed = stage.commit(installedVersion("demo"));
        }

        Path target = gameDirectory.resolve("versions/demo");
        assertTrue(Files.isRegularFile(target.resolve("demo.json")));
        assertEquals(target.toAbsolutePath().normalize(), installed.versionDirectory());
        assertEquals(target.resolve("demo.json").toAbsolutePath().normalize(),
                installed.versionJson());
    }

    @Test
    void refusesToOverwriteExistingTargetAndCleansStageOnClose() throws Exception {
        Path target = gameDirectory.resolve("versions/demo");
        Files.createDirectories(target);
        Files.writeString(target.resolve("existing.txt"), "keep");

        Path staged;
        try (VersionDirectoryStage stage = VersionDirectoryStage.create(gameDirectory, "demo")) {
            staged = stage.directory();
            assertThrows(IOException.class, () -> stage.commit(installedVersion("demo")));
        }

        assertFalse(Files.exists(staged));
        assertEquals("keep", Files.readString(target.resolve("existing.txt")));
    }

    private InstalledVersion installedVersion(String id) {
        VersionInfo manifest = new VersionInfo(
                id, "release", null, "example.Main", null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        return new InstalledVersion(id, manifest, gameDirectory, null, null,
                null, null, null, null);
    }
}
