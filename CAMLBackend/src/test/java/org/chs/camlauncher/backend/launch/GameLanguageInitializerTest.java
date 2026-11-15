package org.chs.camlauncher.backend.launch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLanguageInitializerTest {
    @TempDir
    Path gameDataDirectory;

    @Test
    void mapsCustomChineseLocaleToSimplifiedChinese() {
        assertEquals("zh_cn", GameLanguageInitializer.minecraftLanguage("fn_cn"));
        assertEquals("zh_cn", GameLanguageInitializer.minecraftLanguage("ZH-CN"));
        assertEquals("en_us", GameLanguageInitializer.minecraftLanguage("en_us"));
        assertEquals("en_us", GameLanguageInitializer.minecraftLanguage("invalid locale"));
    }

    @Test
    void createsOptionsForFirstLaunch() throws Exception {
        GameLanguageInitializer.apply(gameDataDirectory, "fn_cn");

        Path options = gameDataDirectory.resolve("options.txt");
        assertEquals("lang:zh_cn", Files.readString(options, StandardCharsets.UTF_8).trim());
    }

    @Test
    void appendsLanguageWithoutOverwritingOtherOptions() throws Exception {
        Path options = gameDataDirectory.resolve("options.txt");
        Files.writeString(options, "fov:0.5\nrenderDistance:12\n", StandardCharsets.UTF_8);

        GameLanguageInitializer.apply(gameDataDirectory, "zh_cn");

        String content = Files.readString(options, StandardCharsets.UTF_8);
        assertTrue(content.contains("fov:0.5"));
        assertTrue(content.contains("renderDistance:12"));
        assertTrue(content.endsWith("lang:zh_cn\n"));
    }

    @Test
    void doesNotOverrideExistingLanguage() throws Exception {
        Path options = gameDataDirectory.resolve("options.txt");
        Files.writeString(options, "lang:en_us\nfov:0.5\n", StandardCharsets.UTF_8);

        GameLanguageInitializer.apply(gameDataDirectory, "fn_cn");

        assertEquals("lang:en_us\nfov:0.5\n",
                Files.readString(options, StandardCharsets.UTF_8));
    }
}
