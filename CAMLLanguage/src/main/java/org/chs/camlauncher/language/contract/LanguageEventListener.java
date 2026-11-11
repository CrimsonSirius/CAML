package org.chs.camlauncher.language.contract;

/** 语言事件的只读监听器。 */
@FunctionalInterface
public interface LanguageEventListener {
    void onLanguageEvent(LanguageEvent event);
}
