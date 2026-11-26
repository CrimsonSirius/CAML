package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameDataLayoutTest {

    @TempDir
    Path gameDirectory;

    @Test
    void fullModeAlwaysUsesVersionDirectory() {
        Path versionDirectory = gameDirectory.resolve("versions").resolve("1.21.4");

        assertEquals(
                versionDirectory,
                GameDataLayout.resolve(gameDirectory, versionDirectory, VersionIsolationMode.FULL)
        );
    }

    @Test
    void noneModeAlwaysUsesRootDirectory() {
        Path versionDirectory = gameDirectory.resolve("versions").resolve("1.21.4");

        assertEquals(
                gameDirectory,
                GameDataLayout.resolve(gameDirectory, versionDirectory, VersionIsolationMode.NONE)
        );
    }
}
