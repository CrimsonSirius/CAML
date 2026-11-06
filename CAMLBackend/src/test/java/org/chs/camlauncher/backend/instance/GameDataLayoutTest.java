package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 版本隔离：游戏数据在 versions/&lt;id&gt; 内时，必须把 --gameDir 指向版本目录，
 * 否则游戏看不到原有存档（PCL、HMCL 的默认行为）。
 */
class GameDataLayoutTest {

    @TempDir
    Path gameDirectory;

    @Test
    void isolatedVersionUsesVersionDirectory() throws Exception {
        Path versionDirectory = gameDirectory.resolve("versions").resolve("1.21.4-DeepSeek");
        Files.createDirectories(versionDirectory.resolve("saves").resolve("新的世界"));
        Files.writeString(versionDirectory.resolve("options.txt"), "fov:0.0");

        assertTrue(GameDataLayout.isIsolated(versionDirectory));
        assertEquals(versionDirectory, GameDataLayout.resolve(gameDirectory, versionDirectory));
    }

    @Test
    void sharedLayoutUsesRootDirectory() throws Exception {
        Path versionDirectory = gameDirectory.resolve("versions").resolve("1.21.4");
        Files.createDirectories(versionDirectory);
        Files.writeString(versionDirectory.resolve("1.21.4.json"), "{}");
        Files.createDirectories(gameDirectory.resolve("saves"));

        assertFalse(GameDataLayout.isIsolated(versionDirectory));
        assertEquals(gameDirectory, GameDataLayout.resolve(gameDirectory, versionDirectory));
    }

    @Test
    void missingVersionDirectoryIsNotIsolated() {
        Path missing = gameDirectory.resolve("versions").resolve("absent");
        assertFalse(GameDataLayout.isIsolated(missing));
        assertEquals(gameDirectory, GameDataLayout.resolve(gameDirectory, missing));
    }

    @Test
    void fullModeAlwaysUsesVersionDirectory() {
        Path versionDirectory = gameDirectory.resolve("versions").resolve("1.21.4");

        assertEquals(
                versionDirectory,
                GameDataLayout.resolve(gameDirectory, versionDirectory, VersionIsolationMode.FULL)
        );
    }

    @Test
    void noneModeAlwaysUsesRootDirectory() throws Exception {
        Path versionDirectory = gameDirectory.resolve("versions").resolve("1.21.4");
        Files.createDirectories(versionDirectory.resolve("saves"));
        Files.writeString(versionDirectory.resolve("options.txt"), "fov:0.0");

        assertEquals(
                gameDirectory,
                GameDataLayout.resolve(gameDirectory, versionDirectory, VersionIsolationMode.NONE)
        );
    }
}

