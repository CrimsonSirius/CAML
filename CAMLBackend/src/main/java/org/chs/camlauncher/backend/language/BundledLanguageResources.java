package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.language.contract.LanguageConstants;

import java.io.IOException;
import java.io.InputStream;

/** 读取 CAMLLanguage 打包的内置语言资源。 */
final class BundledLanguageResources {
    private static final String RESOURCE_ROOT = "/lang/";

    private BundledLanguageResources() {
    }

    /**
     * 读取内置语言文件。
     *
     * <p>lang/*.json 属于 CAMLLanguage，所以用 LanguageConstants 作为定位锚点。
     * jpackage 产物把 app 目录下的全部 jar 放进 --module-path，它们会以命名（自动）模块加载；
     * 命名模块里的 {@code Class.getResourceAsStream} 只能看到本模块的资源，
     * 用 CAMLBackend 的类去读 CAMLLanguage 的 lang/*.json 会返回 null（安装版启动即失败的根因）。
     */
    static byte[] read(String locale) throws IOException {
        String name = RESOURCE_ROOT + locale + ".json";
        try (InputStream input = open(name)) {
            if (input == null) {
                throw new IOException("缺少内置语言资源: " + name);
            }
            return input.readAllBytes();
        }
    }

    private static InputStream open(String name) {
        // 模块内读取：无论在模块路径还是类路径上都能命中 CAMLLanguage 里的 lang/*.json。
        InputStream input = LanguageConstants.class.getResourceAsStream(name);
        if (input != null) {
            return input;
        }
        // 兜底：以类路径方式启动（Gradle run、IDE、单元测试）时的常规查找。
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = BundledLanguageResources.class.getClassLoader();
        }
        return loader.getResourceAsStream(name.substring(1));
    }
}