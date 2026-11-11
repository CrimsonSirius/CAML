package org.chs.camlauncher.backend.language;

import java.io.IOException;
import java.io.InputStream;

/** 读取 CAMLLanguage 打包的内置语言资源。 */
final class BundledLanguageResources {
    private static final String RESOURCE_ROOT = "/lang/";

    private BundledLanguageResources() {
    }

    static byte[] read(String locale) throws IOException {
        try (InputStream input = BundledLanguageResources.class.getResourceAsStream(
                RESOURCE_ROOT + locale + ".json")) {
            if (input == null) {
                throw new IOException("缺少内置语言资源: " + RESOURCE_ROOT + locale + ".json");
            }
            return input.readAllBytes();
        }
    }
}
