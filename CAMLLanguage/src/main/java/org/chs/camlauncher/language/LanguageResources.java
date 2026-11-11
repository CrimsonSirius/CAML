package org.chs.camlauncher.language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** 内置语言资源安装与读取。 */
final class LanguageResources {
    static final List<String> BUNDLED_LOCALES = List.of("zh_cn", "en_us", "fn_cn");
    private static final String RESOURCE_ROOT = "/lang/";

    private LanguageResources() {
    }

    /**
     * 把内置语言文件同步到数据目录，返回运行时语言目录。
     *
     * <p>只处理内置语言：文件缺失或内容与内置资源不一致（改过文案、缺键、JSON 损坏）时
     * 按内置资源重写，保证每次启动都能拿到最新的内置翻译与新增键。
     * 数据目录中用户自行新增的语言文件不受影响，始终原样保留。</p>
     */
    static Path install(Path dataDirectory) throws IOException {
        Path directory = dataDirectory.resolve("lang");
        Files.createDirectories(directory);
        for (String locale : BUNDLED_LOCALES) {
            Path target = directory.resolve(locale + ".json");
            if (matchesBundled(locale, target)) {
                continue;
            }
            try (InputStream input = openBundled(locale)) {
                if (input == null) {
                    throw new IOException("缺少内置语言文件: " + RESOURCE_ROOT + locale + ".json");
                }
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return directory;
    }

    /**
     * 已安装文件是否与内置资源完全一致。
     *
     * <p>按翻译键值比较而不是按字节比较，键序或空白不同不算差异；<br>
     * 文件不存在、JSON 无法解析或键值有差异都返回 {@code false}，交由调用方按内置资源重写。</p>
     */
    private static boolean matchesBundled(String locale, Path installed) {
        if (!Files.isRegularFile(installed)) {
            return false;
        }
        try (InputStream input = openBundled(locale)) {
            if (input == null) {
                return false;
            }
            try (Reader bundled = new InputStreamReader(input, StandardCharsets.UTF_8);
                 Reader existing = Files.newBufferedReader(installed, StandardCharsets.UTF_8)) {
                return Language.parse(bundled).equals(Language.parse(existing));
            }
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    static InputStream openBundled(String locale) {
        return LanguageResources.class.getResourceAsStream(RESOURCE_ROOT + locale + ".json");
    }
}