package org.chs.camlauncher.backend.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        assertTrue(content.contains("-caml-background: #141517"));
        assertTrue(content.contains("-caml-on-surface: #d7d9dd"));
    }

    @Test
    void nightSurfaceHierarchyKeepsInputsLightest() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureBundledThemes();
        Path night = dataDirectory.resolve("themes").resolve("night.css");

        double background = luminance(token(night, "-caml-background"));
        double group = luminance(token(night, "-caml-surface-container"));
        double input = luminance(token(night, "-caml-surface-lowest"));

        assertTrue(background < group, "页面背景必须比分组更暗");
        assertTrue(group < input, "分组必须比输入框/选择框更暗");
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
    void bundledThemesDefineTheSameColorTokens() throws Exception {
        ThemeService service = new ThemeService(dataDirectory);
        service.ensureBundledThemes();
        Path themes = dataDirectory.resolve("themes");

        assertEquals(
                colorTokens(themes.resolve("mango.css")),
                colorTokens(themes.resolve("night.css"))
        );
    }

    @Test
    void rejectsThemeIdsThatEscapeTheThemeDirectory() {
        ThemeService service = new ThemeService(dataDirectory);

        assertThrows(IllegalArgumentException.class, () -> service.selectTheme("../outside"));
        assertThrows(IllegalArgumentException.class, () -> service.selectTheme(".."));
        assertThrows(IllegalArgumentException.class, () -> service.selectTheme(""));
    }

    private static Set<String> colorTokens(Path theme) throws Exception {
        return Files.readAllLines(theme).stream()
                .map(String::trim)
                .filter(line -> line.startsWith("-caml-") && line.contains(":"))
                .map(line -> line.substring(0, line.indexOf(':')))
                .collect(Collectors.toSet());
    }

    private static String token(Path theme, String name) throws Exception {
        String prefix = name + ":";
        return Files.readAllLines(theme).stream()
                .map(String::trim)
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .map(value -> value.endsWith(";")
                        ? value.substring(0, value.length() - 1).trim()
                        : value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少颜色令牌 " + name));
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return 0.2126 * channel((rgb >> 16) & 0xff)
                + 0.7152 * channel((rgb >> 8) & 0xff)
                + 0.0722 * channel(rgb & 0xff);
    }

    private static double channel(int value) {
        double channel = value / 255.0;
        return channel <= 0.04045
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
