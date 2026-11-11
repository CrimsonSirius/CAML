package org.chs.camlauncher.language;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 内置语言资源安装与读取。 */
final class LanguageResources {
    static final List<String> BUNDLED_LOCALES = List.of("zh_cn", "en_us");
    private static final String RESOURCE_ROOT = "/lang/";

    private LanguageResources() {
    }

    /** 把尚未存在的内置语言文件复制到数据目录，返回运行时语言目录。 */
    static Path install(Path dataDirectory) throws IOException {
        Path directory = dataDirectory.resolve("lang");
        Files.createDirectories(directory);
        for (String locale : BUNDLED_LOCALES) {
            Path target = directory.resolve(locale + ".json");
            if (Files.exists(target)) {
                continue;
            }
            try (InputStream input = openBundled(locale)) {
                if (input == null) {
                    throw new IOException("缺少内置语言文件: " + RESOURCE_ROOT + locale + ".json");
                }
                try {
                    Files.copy(input, target);
                } catch (FileAlreadyExistsException ignored) {
                    // 另一个启动实例已经完成首次复制。
                }
            }
        }
        return directory;
    }

    static InputStream openBundled(String locale) {
        return LanguageResources.class.getResourceAsStream(RESOURCE_ROOT + locale + ".json");
    }
}
