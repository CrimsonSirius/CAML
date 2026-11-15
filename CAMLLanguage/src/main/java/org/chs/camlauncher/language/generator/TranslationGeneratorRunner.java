package org.chs.camlauncher.language.generator;

import org.chs.camlauncher.language.generator.lang.BOCNGenerator;
import org.chs.camlauncher.language.generator.lang.DEDEGenerator;
import org.chs.camlauncher.language.generator.lang.ENUSGenerator;
import org.chs.camlauncher.language.generator.lang.FNCNGenerator;
import org.chs.camlauncher.language.generator.lang.FRFRGenerator;
import org.chs.camlauncher.language.generator.lang.JAJPGenerator;
import org.chs.camlauncher.language.generator.lang.KOKRGenerator;
import org.chs.camlauncher.language.generator.lang.LALAGenerator;
import org.chs.camlauncher.language.generator.lang.RURUGenerator;
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
                new ZHTWGenerator(),
                new ENUSGenerator(),
                new FNCNGenerator(),
                new JAJPGenerator(),
                new KOKRGenerator(),
                new FRFRGenerator(),
                new DEDEGenerator(),
                new RURUGenerator(),
                new LALAGenerator(),
                new BOCNGenerator()
        );
        for (LanguageTranslationGenerator generator : generators) {
            Path file = generator.generate(output);
            System.out.println("Generated " + file.toAbsolutePath());
        }
    }
}