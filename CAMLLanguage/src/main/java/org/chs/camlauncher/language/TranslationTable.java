package org.chs.camlauncher.language;

import java.util.TreeMap;

/**
 * 一种语言的翻译键与文案集合。
 *
 * <p>按键排序保存；{@link #put(String, String)} 会拒绝重复键，避免后写入的文案
 * 静默覆盖前面的翻译。生成器直接以 {@code t.put(key, value)} 的实例方法登记文案。</p>
 */
public final class TranslationTable extends TreeMap<String, String> {
    @Override
    public String put(String key, String value) {
        if (containsKey(key)) {
            throw new IllegalArgumentException("Duplicate translation key: " + key);
        }
        return super.put(key, value);
    }
}

