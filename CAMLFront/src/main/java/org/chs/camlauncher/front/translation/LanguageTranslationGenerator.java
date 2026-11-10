package org.chs.camlauncher.front.translation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 前端翻译文件生成器基类。
 *
 * <p>每种语言实现一个子类（例如 {@code ZHCNGenerator}），在 {@link #populate(TranslationTable)}
 * 中以 {@code t.put(key, value)} 登记翻译键和文案。{@link #generate(Path)} 统一校验、排序并写出
 * 同名的 {@code <locale>.json}，确保翻译文件始终由代码生成而不是手工维护。</p>
 */
public abstract class LanguageTranslationGenerator {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** 语言标识，同时作为输出文件名，例如 {@code zh_cn}。 */
    protected abstract String locale();

    /** 登记当前语言的全部翻译键；重复键会抛出异常。 */
    protected abstract void populate(TranslationTable translations);

    /** 生成翻译文件，返回写入路径。 */
    public final Path generate(Path outputDirectory) throws IOException {
        TranslationTable translations = new TranslationTable();
        populate(translations);
        Files.createDirectories(outputDirectory);
        Path file = outputDirectory.resolve(locale() + ".json");
        String json = GSON.toJson(translations) + System.lineSeparator();
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }
}