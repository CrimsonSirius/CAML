package org.chs.camlauncher.backend.theme;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 管理启动器数据目录中的外部主题。
 *
 * <p>主题是 {@code <data>/themes/*.css} 文件。启动器本体不携带固定配色；
 * 默认主题作为 classpath 资源打包，首次启动时复制到主题目录，之后由用户自由
 * 添加、删除或修改主题文件。</p>
 */
public final class ThemeService {
    public static final String DEFAULT_THEME_ID = "default";

    private static final String THEMES_DIRECTORY = "themes";
    private static final String THEME_EXTENSION = ".css";
    private static final String DEFAULT_THEME_RESOURCE = "/themes/default.css";
    private static final String CONFIG_FILE = "theme.txt";

    private final Path themesDirectory;
    private final Path configFile;

    public ThemeService(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.themesDirectory = dataDirectory.resolve(THEMES_DIRECTORY);
        this.configFile = dataDirectory.resolve(CONFIG_FILE);
    }

    /** 创建主题目录，并在缺少默认主题时从 classpath 复制一份。 */
    public void ensureDefaultTheme() throws IOException {
        Files.createDirectories(themesDirectory);
        Path target = themesDirectory.resolve(DEFAULT_THEME_ID + THEME_EXTENSION);
        if (Files.exists(target)) {
            return;
        }

        try (InputStream input = ThemeService.class.getResourceAsStream(DEFAULT_THEME_RESOURCE)) {
            if (input == null) {
                throw new IOException("缺少内置默认主题: " + DEFAULT_THEME_RESOURCE);
            }
            try {
                Files.copy(input, target);
            } catch (FileAlreadyExistsException ignored) {
                // 另一个启动实例已经完成了首次复制。
            }
        }
    }

    /** @return 按文件名排序的主题列表 */
    public List<ThemeInfo> listThemes() throws IOException {
        ensureDefaultTheme();
        try (Stream<Path> files = Files.list(themesDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(ThemeService::isCssFile)
                    .filter(file -> file.getFileName().toString().length() > THEME_EXTENSION.length())
                    .map(this::toThemeInfo)
                    .sorted(Comparator.comparing(ThemeInfo::id, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    /** @return 配置文件记录的主题；不存在或无效时回退到默认主题 */
    public ThemeInfo activeTheme() throws IOException {
        List<ThemeInfo> themes = listThemes();
        String configured = readConfiguredId();
        Optional<ThemeInfo> selected = themes.stream()
                .filter(theme -> theme.id().equals(configured))
                .findFirst();
        if (selected.isPresent()) {
            return selected.get();
        }

        return themes.stream()
                .filter(theme -> theme.id().equals(DEFAULT_THEME_ID))
                .findFirst()
                .orElseThrow(() -> new IOException("主题目录中没有默认主题"));
    }

    /** 记录当前主题。只允许主题目录下的普通 CSS 文件名。 */
    public void selectTheme(String id) throws IOException {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("非法主题 ID: " + id);
        }

        Path file = themesDirectory.resolve(id + THEME_EXTENSION);
        if (!Files.isRegularFile(file)) {
            throw new IOException("主题不存在: " + id);
        }
        Files.writeString(configFile, id + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path themesDirectory() {
        return themesDirectory;
    }

    private String readConfiguredId() throws IOException {
        if (!Files.isRegularFile(configFile)) {
            return DEFAULT_THEME_ID;
        }
        String id = Files.readString(configFile).trim();
        return id.isEmpty() ? DEFAULT_THEME_ID : id;
    }

    private ThemeInfo toThemeInfo(Path file) {
        String name = file.getFileName().toString();
        String id = name.substring(0, name.length() - THEME_EXTENSION.length());
        return new ThemeInfo(id, file.toAbsolutePath().normalize());
    }

    private static boolean isCssFile(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(THEME_EXTENSION);
    }

    private static boolean isValidId(String id) {
        return id != null
                && !id.isBlank()
                && !id.contains("/")
                && !id.contains("\\")
                && !id.contains(":")
                && !id.equals(".")
                && !id.equals("..")
                && id.chars().noneMatch(Character::isISOControl);
    }
}
