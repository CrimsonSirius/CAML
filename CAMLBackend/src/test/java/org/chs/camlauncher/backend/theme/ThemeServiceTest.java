package org.chs.camlauncher.backend.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeServiceTest {
    @TempDir
    Path dataDirectory;

    @Test
    void copiesBundledThemesOnFirstUse() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);

        service.ensureBundledThemes();

        for (String id : List.of("mango", "night")) {
            Path theme = dataDirectory.resolve("themes").resolve(id + ".css");
            assertTrue(Files.isRegularFile(theme), "缺少内置主题 " + id);
            String content = Files.readString(theme);
            assertTrue(content.contains("-caml-accent:"), id);
            assertTrue(content.contains("-caml-background:"), id);
            assertTrue(content.contains("-caml-on-surface:"), id);
            assertTrue(content.contains("-caml-on-surface-muted:"), id);
        }
    }

    @Test
    void listsCssThemesSortedAndIgnoresOtherFiles() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureBundledThemes();
        Path themes = dataDirectory.resolve("themes");
        Files.writeString(themes.resolve("z.css"), ".caml-root {}");
        Files.writeString(themes.resolve("a.css"), ".caml-root {}");
        Files.writeString(themes.resolve("notes.txt"), "ignored");

        List<String> ids = service.listThemes().stream().map(ThemeInfo::id).toList();

        assertEquals(List.of("a", "mango", "night", "z"), ids);
    }

    @Test
    void activeThemeFallsBackToDefaultWhenNotConfigured() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);

        assertEquals(ThemeService.DEFAULT_THEME_ID, service.activeTheme().id());
    }

    @Test
    void selectedThemeIsPersistedForNextStartup() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureBundledThemes();
        Files.writeString(dataDirectory.resolve("themes").resolve("custom.css"), ".caml-root {}");

        service.selectTheme("custom");

        assertEquals("custom", new ThemeService(dataDirectory).activeTheme().id());
    }

    @Test
    void nightThemeCanBeSelectedAndContainsDarkPalette() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureBundledThemes();

        service.selectTheme("night");

        ThemeInfo active = new ThemeService(dataDirectory).activeTheme();
        assertEquals("night", active.id());
        String content = Files.readString(active.file());
        assertTrue(content.contains("-caml-background: #1e1f22"));
        assertTrue(content.contains("-caml-on-surface: #dfe1e5"));
    }

    @Test
    void existingBundledThemesAreNotOverwritten() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureBundledThemes();
        Path themes = dataDirectory.resolve("themes");
        Path mango = themes.resolve("mango.css");
        Path night = themes.resolve("night.css");
        Files.writeString(mango, "my custom mango");
        Files.writeString(night, "my custom night");

        service.ensureBundledThemes();

        assertEquals("my custom mango", Files.readString(mango));
        assertEquals("my custom night", Files.readString(night));
    }

    @Test
    void addsMissingBundledThemeForExistingInstallations() throws Exception {
        Path themes = dataDirectory.resolve("themes");
        Files.createDirectories(themes);
        Path mango = themes.resolve("mango.css");
        Files.writeString(mango, "my custom mango");

        new ThemeService(dataDirectory).ensureBundledThemes();

        assertEquals("my custom mango", Files.readString(mango));
        assertTrue(Files.isRegularFile(themes.resolve("night.css")));
    }

    @Test
    void rejectsThemeIdsThatEscapeTheThemeDirectory() {
        ThemeService service = new ThemeService(dataDirectory);

        assertThrows(IllegalArgumentException.class, () -> service.selectTheme("../outside"));
        assertThrows(IllegalArgumentException.class, () -> service.selectTheme(".."));
        assertThrows(IllegalArgumentException.class, () -> service.selectTheme(""));
    }
}
