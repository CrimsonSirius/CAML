package org.chs.camlauncher.language;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 前端本地化文本入口。
 *
 * <p>参考 Minecraft 的 {@code lang/*.json}：界面只引用翻译键，运行时优先从
 * 数据目录中的 {@code lang/<locale>.json} 读取文案。内置语言文件由
 * {@link #initialize(Path)} 在启动时同步：缺失或内容与内置资源不一致时按内置资源重写，
 * 用户自行新增的语言文件保持原样。翻译键缺失时回退为键名，方便发现遗漏。</p>
 *
 * <p>语言选择顺序：系统属性 {@code caml.language}、系统语言（en → {@code en_us}，
 * 其余回退到 {@code zh_cn}）。</p>
 */
public final class Language {
    public static final String DEFAULT_LOCALE = "fn_cn";

    /**
     * 内置语言的自称，硬编码在运行时而不是翻译文件里。
     *
     * <p>语言名必须使用该语言自己的写法，且与界面语言无关。放在代码中可以保证
     * 新增语言或修改翻译时不会写出错误的语言名。新增内置语言时在这里登记自称，
     * 未登记的语言回退显示 locale 标识。</p>
     */
    private static final Map<String, String> NATIVE_NAMES = Map.of(
            "zh_cn", "简体中文",
            "en_us", "English",
            "fn_cn", "地道中国话"
    );

    private static volatile Path languageDirectory;
    private static volatile String activeLocale;
    private static volatile Map<String, String> translations = Map.of();

    private Language() {
    }

    /** 安装内置语言文件并切换到数据目录读取模式。 */
    public static synchronized void initialize(Path dataDirectory) throws IOException {
        languageDirectory = LanguageResources.install(
                Objects.requireNonNull(dataDirectory, "dataDirectory"));
        activeLocale = null;
        translations = Map.of();
    }

    /** 当前生效的语言标识，例如 {@code zh_cn}。 */
    public static String locale() {
        ensureLoaded();
        return activeLocale;
    }

    /** 切换语言并重新加载；供语言设置使用。 */
    public static synchronized void setLocale(String locale) {
        activeLocale = normalize(locale);
        translations = load(activeLocale);
    }

    /** 按系统属性和系统语言重新自动选择语言。 */
    public static void useAutomaticLocale() {
        setLocale(detectLocale());
    }

    /** 当前可用的界面语言：内置语言优先，用户新增语言按名称排序。 */
    public static List<String> supportedLocales() {
        Path directory = languageDirectory;
        if (directory == null || !Files.isDirectory(directory)) {
            return LanguageResources.BUNDLED_LOCALES;
        }
        try (Stream<Path> files = Files.list(directory)) {
            Set<String> available = new HashSet<>();
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (Files.isRegularFile(file) && name.endsWith(".json")) {
                    available.add(name.substring(0, name.length() - ".json".length()));
                }
            }
            List<String> ordered = new ArrayList<>();
            for (String bundled : LanguageResources.BUNDLED_LOCALES) {
                if (available.remove(bundled)) {
                    ordered.add(bundled);
                }
            }
            available.stream().sorted().forEach(ordered::add);
            return List.copyOf(ordered);
        } catch (IOException e) {
            return LanguageResources.BUNDLED_LOCALES;
        }
    }

    public static boolean isSupportedLocale(String locale) {
        return supportedLocales().contains(normalize(locale));
    }

    public static String normalizeLocale(String locale) {
        return normalize(locale);
    }

    /**
     * 返回语言在**自身语言**中的名称，例如 {@code zh_cn} → {@code 简体中文}。
     *
     * <p>名称来自运行时硬编码的 {@link #NATIVE_NAMES}，不读取翻译文件、也不经过当前
     * 界面语言的翻译，因此切换界面语言时语言下拉框中的名称始终统一。
     * 未登记的 locale 回退显示 locale 标识。</p>
     */
    public static String localeDisplayName(String locale) {
        return NATIVE_NAMES.getOrDefault(normalize(locale), locale);
    }

    /** 按翻译键取文案，可选 {@link String#format(Locale, String, Object...)} 占位符。 */
    public static String tr(String key, Object... args) {
        ensureLoaded();
        String template = translations.getOrDefault(key, key);
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (IllegalFormatException e) {
            return template;
        }
    }

    private static synchronized void ensureLoaded() {
        if (activeLocale == null) {
            activeLocale = detectLocale();
            translations = load(activeLocale);
        }
    }

    private static Map<String, String> load(String locale) {
        Map<String, String> loaded = loadResource(locale);
        if (loaded.isEmpty() && !DEFAULT_LOCALE.equals(locale)) {
            loaded = loadResource(DEFAULT_LOCALE);
        }
        return loaded;
    }

    private static Map<String, String> loadResource(String locale) {
        Path directory = languageDirectory;
        if (directory != null) {
            Path file = directory.resolve(locale + ".json");
            if (Files.isRegularFile(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    Map<String, String> parsed = parse(reader);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }
                } catch (IOException | JsonParseException | IllegalStateException e) {
                    // 数据目录文件损坏时退回内置资源，避免整个界面只剩键名。
                }
            }
        }
        return loadBundled(locale);
    }

    private static Map<String, String> loadBundled(String locale) {
        try (InputStream stream = LanguageResources.openBundled(locale)) {
            if (stream == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(reader);
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            return Map.of();
        }
    }

    /** 解析语言 JSON；同步内置资源比对也复用这里的逻辑。 */
    static Map<String, String> parse(Reader reader) {
        JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return Map.copyOf(map);
    }

    private static String detectLocale() {
        String override = System.getProperty("caml.language");
        if (override != null && !override.isBlank()) {
            return normalize(override);
        }
        String language = Locale.getDefault().getLanguage();
        return "en".equalsIgnoreCase(language) ? "en_us" : DEFAULT_LOCALE;
    }

    private static String normalize(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}