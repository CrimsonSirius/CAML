package org.chs.camlauncher.front.translation;

import org.chs.camlauncher.front.translation.generator.ENUSGenerator;
import org.chs.camlauncher.front.translation.generator.ZHCNGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 翻译文件生成入口。
 *
 * <p>由 Gradle {@code generateTranslation} 任务调用，依次执行所有
 * {@link LanguageTranslationGenerator}，把结果写入 {@code CAMLFront/src/main/resources/lang}。</p>
 */
public final class TranslationGeneratorRunner {
    private TranslationGeneratorRunner() {
    }

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length > 0 ? args[0] : "CAMLFront/src/main/resources/lang");
        Files.createDirectories(output);
        List<LanguageTranslationGenerator> generators = List.of(
                new ZHCNGenerator(),
                new ENUSGenerator()
        );
        for (LanguageTranslationGenerator generator : generators) {
            Path file = generator.generate(output);
            System.out.println("Generated " + file.toAbsolutePath());
        }
    }
}

