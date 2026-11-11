package org.chs.camlauncher.language.contract;

/** 一个可用界面语言的描述。 */
public record LanguageDescriptor(
        String locale,
        String displayName,
        boolean bundled,
        boolean custom
) {
}
