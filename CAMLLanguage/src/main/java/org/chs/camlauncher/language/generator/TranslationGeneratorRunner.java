package org.chs.camlauncher.language.generator;

import org.chs.camlauncher.language.generator.lang.ENUSGenerator;
import org.chs.camlauncher.language.generator.lang.FNCNGenerator;
import org.chs.camlauncher.language.generator.lang.ZHCNGenerator;
import org.chs.camlauncher.language.generator.lang.ZHTWGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 翻译文件生成入口。
 *
 * <p>由 Gradle {@code generateTranslation} 任务调用，依次执行所有
 * {@link LanguageTranslationGenerator}，把结果写入 {@code CAMLLanguage/src/main/resources/lang}。</p>
 */
public final class TranslationGeneratorRunner {
    private TranslationGeneratorRunner() {
    }

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length > 0 ? args[0] : "CAMLLanguage/src/main/resources/lang");
        Files.createDirectories(output);
        List<LanguageTranslationGenerator> generators = List.of(
                new ZHCNGenerator(),
                new ENUSGenerator(),
                new FNCNGenerator(),
                new ZHTWGenerator()
        );
        for (LanguageTranslationGenerator generator : generators) {
            Path file = generator.generate(output);
            System.out.println("Generated " + file.toAbsolutePath());
        }
    }
}

