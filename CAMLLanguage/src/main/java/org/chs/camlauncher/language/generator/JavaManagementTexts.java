package org.chs.camlauncher.language.generator;

/** Java 管理页的共享英文默认值。 */
final class JavaManagementTexts {
    private JavaManagementTexts() {
    }

    static void populateDefaults(TranslationTable t) {
        put(t, "settings.section.java", "Java management");
        put(t, "settings.javaManagement.count", "%d Java runtimes");
        put(t, "settings.javaManagement.empty", "No Java runtimes found");
        put(t, "settings.javaManagement.manual", "Manually added");
        put(t, "settings.javaManagement.unavailable", "Unavailable");
        put(t, "settings.javaManagement.meta", "%s · %d-bit%s");
        put(t, "settings.javaManagement.openFolder", "Open containing folder");
        put(t, "settings.javaManagement.openFolder.opening", "Opening folder...");
        put(t, "settings.javaManagement.openFolder.failed", "Failed to open Java folder: %s");
        put(t, "operation.openJavaFolder", "Open Java folder");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
