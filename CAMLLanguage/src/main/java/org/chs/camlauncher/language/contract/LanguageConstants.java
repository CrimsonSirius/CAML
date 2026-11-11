package org.chs.camlauncher.language.contract;

import java.util.List;

/** 语言模块共享常量，不包含运行时行为。 */
public final class LanguageConstants {
    public static final String DEFAULT_LOCALE = "en_us";
    public static final String SIMPLIFIED_CHINESE_LOCALE = "zh_cn";
    public static final String LANGUAGE_OVERRIDE_PROPERTY = "caml.language";
    public static final List<String> BUNDLED_LOCALES = List.of("zh_cn", "en_us", "fn_cn");

    private LanguageConstants() {
    }
}
