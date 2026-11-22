package org.chs.camlauncher.language.generator;

/** 繁體中文的主頁工作區文案覆蓋。 */
public final class TraditionalWorkspaceTexts {
    private TraditionalWorkspaceTexts() {
    }

    public static void populate(TranslationTable t) {
        put(t, "log.expand", "展開");
        put(t, "log.collapse", "收起");
        put(t, "log.empty", "尚無活動記錄");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
