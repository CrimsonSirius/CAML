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
    void copiesBundledDefaultThemeOnFirstUse() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);

        service.ensureDefaultTheme();

        Path theme = dataDirectory.resolve("themes").resolve("default.css");
        assertTrue(Files.isRegularFile(theme));
        assertTrue(Files.readString(theme).contains("-caml-background: #ffffff"));
    }

    @Test
    void listsCssThemesSortedAndIgnoresOtherFiles() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureDefaultTheme();
        Path themes = dataDirectory.resolve("themes");
        Files.writeString(themes.resolve("z.css"), ".caml-root {}");
        Files.writeString(themes.resolve("a.css"), ".caml-root {}");
        Files.writeString(themes.resolve("notes.txt"), "ignored");

        List<String> ids = service.listThemes().stream().map(ThemeInfo::id).toList();

        assertEquals(List.of("a", "default", "z"), ids);
    }

    @Test
    void activeThemeFallsBackToDefaultWhenNotConfigured() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);

        assertEquals(ThemeService.DEFAULT_THEME_ID, service.activeTheme().id());
    }

    @Test
    void selectedThemeIsPersistedForNextStartup() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureDefaultTheme();
        Files.writeString(dataDirectory.resolve("themes").resolve("custom.css"), ".caml-root {}");

        service.selectTheme("custom");

        assertEquals("custom", new ThemeService(dataDirectory).activeTheme().id());
    }

    @Test
    void existingDefaultThemeIsNotOverwritten() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureDefaultTheme();
        Path theme = dataDirectory.resolve("themes").resolve("default.css");
        Files.writeString(theme, "my custom default");

        service.ensureDefaultTheme();

        assertEquals("my custom default", Files.readString(theme));
    }

    @Test
    void rejectsThemeIdsThatEscapeTheThemeDirectory() {
        ThemeService service = new ThemeService(dataDirectory);

        assertThrows(IllegalArgumentException.class, () -> service.selectTheme("../outside"));
        assertThrows(IllegalArgumentException.class, () -> service.selectTheme(".."));
        assertThrows(IllegalArgumentException.class, () -> service.selectTheme(""));
    }
}
