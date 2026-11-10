package org.chs.camlauncher.front.translation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/**
 * 前端本地化文本入口。
 *
 * <p>参考 Minecraft 的 {@code lang/*.json}：界面只引用翻译键，运行时从
 * {@code /lang/<locale>.json} 读取文案。翻译键缺失时回退为键名，方便发现遗漏。</p>
 *
 * <p>语言选择顺序：系统属性 {@code caml.language}、系统语言（zh → {@code zh_cn}，
 * en → {@code en_us}），其余回退到 {@code zh_cn}。</p>
 */
public final class Language {
    private static final String DEFAULT_LOCALE = "zh_cn";

    private static volatile String activeLocale;
    private static volatile Map<String, String> translations = Map.of();

    private Language() {
    }

    /** 当前生效的语言标识，例如 {@code zh_cn}。 */
    public static String locale() {
        ensureLoaded();
        return activeLocale;
    }

    /** 切换语言并重新加载；供后续语言设置使用。 */
    public static synchronized void setLocale(String locale) {
        activeLocale = normalize(locale);
        translations = load(activeLocale);
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
        try (InputStream stream = Language.class.getResourceAsStream("/lang/" + locale + ".json")) {
            if (stream == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, String> map = new HashMap<>();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        map.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                return Map.copyOf(map);
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            return Map.of();
        }
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

