package org.chs.camlauncher.language.generator;

/** 主页工作区相关文案的共享英文默认值。 */
final class WorkspaceTexts {
    private WorkspaceTexts() {
    }

    static void populateDefaults(TranslationTable t) {
        put(t, "log.expand", "Show");
        put(t, "log.collapse", "Hide");
        put(t, "log.empty", "No activity yet");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
