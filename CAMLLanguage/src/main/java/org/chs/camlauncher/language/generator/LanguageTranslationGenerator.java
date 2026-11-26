package org.chs.camlauncher.language.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CAML 翻译文件生成器基类。
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

    /**
     * 可选：先由子类登记本地化文案，再由此方法补齐尚未登记的键。
     *
     * <p>账号相关文案数量多、更新频繁，缺少翻译时统一回退到英文，
     * 保证各语言键集合一致；子类只要在 {@link #populate(TranslationTable)} 中
     * 登记同名键即可覆盖这里的默认值。</p>
     */
    protected void populateSharedDefaults(TranslationTable translations) {
        WorkspaceTexts.populateDefaults(translations);
        AccountTexts.populateDefaults(translations);
        AboutTexts.populateDefaults(translations);
        JavaManagementTexts.populateDefaults(translations);
        ModpackTexts.populateDefaults(translations);
        translations.putIfAbsent("launch.warning.java.title",
                "Java version may be incompatible");
        translations.putIfAbsent("launch.warning.java.message",
                "This game requires Java %s, but Java %s is selected. Force launching may fail.");
        translations.putIfAbsent("launch.warning.java.stop", "Stop");
        translations.putIfAbsent("launch.warning.java.force", "Force launch");
        translations.putIfAbsent("common.reset", "Reset");
        translations.putIfAbsent("operation.saveArchiveExtractionLimits",
                "Save archive extraction limits");
        translations.putIfAbsent("download.settings.archive.title", "Archive extraction limits");
        translations.putIfAbsent("download.settings.archive.description",
                "Limits applied when extracting modpacks and worlds.");
        translations.putIfAbsent("download.settings.archive.entries", "Maximum entries");
        translations.putIfAbsent("download.settings.archive.entrySize",
                "Maximum entry size (MiB)");
        translations.putIfAbsent("download.settings.archive.totalSize",
                "Maximum total extracted size (MiB)");
        translations.putIfAbsent("download.settings.archive.saved",
                "Archive extraction limits saved.");
        translations.putIfAbsent("download.settings.archive.invalid",
                "Enter valid limits. Total size must be at least the entry limit.");
        translations.putIfAbsent("tasks.title", "Tasks");
        translations.putIfAbsent("tasks.subtitle", "Downloads and installations");
        translations.putIfAbsent("tasks.empty", "No tasks yet");
        translations.putIfAbsent("tasks.back", "Back to tasks");
        translations.putIfAbsent("tasks.open", "Open task %s · %s");
        translations.putIfAbsent("tasks.status.running", "Running");
        translations.putIfAbsent("tasks.status.completed", "Completed");
        translations.putIfAbsent("tasks.status.failed", "Failed");
        translations.putIfAbsent("tasks.status.cancelled", "Cancelled");
    }

    /** 生成翻译文件，返回写入路径。 */
    public final Path generate(Path outputDirectory) throws IOException {
        TranslationTable translations = new TranslationTable();
        populate(translations);
        populateSharedDefaults(translations);
        Files.createDirectories(outputDirectory);
        Path file = outputDirectory.resolve(locale() + ".json");
        // 固定使用 LF，保证不同系统上生成的 JSON 逐字节一致。
        String json = GSON.toJson(translations) + "\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }
}