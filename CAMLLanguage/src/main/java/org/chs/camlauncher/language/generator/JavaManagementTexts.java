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
        put(t, "settings.javaManagement.download", "Download Java");
        put(t, "settings.javaManagement.distribution", "Distribution");
        put(t, "settings.javaManagement.packageType", "Package type");
        put(t, "settings.javaManagement.version", "Version");
        put(t, "settings.javaManagement.downloadAndInstall", "Download and install");
        put(t, "settings.javaManagement.loadingVersions", "Loading Java versions...");
        put(t, "settings.javaManagement.emptyVersions", "No downloadable Java versions found");
        put(t, "settings.javaManagement.versionCount", "%d versions");
        put(t, "settings.javaManagement.loadVersionsFailed", "Failed to load Java versions: %s");
        put(t, "settings.javaManagement.downloading", "Downloading Java...");
        put(t, "settings.javaManagement.downloadingProgress", "Downloading Java... %d%%");
        put(t, "settings.javaManagement.installed", "Java installed: %s");
        put(t, "settings.javaManagement.installFailed", "Failed to install Java: %s");
        put(t, "settings.javaManagement.linuxHint",
                "On Linux, install the JDK or JRE with your distribution package manager "
                        + "(such as apt, dnf, or pacman). "
                        + "CAML-managed Java is not recommended.");
        put(t, "operation.openJavaFolder", "Open Java folder");
        put(t, "operation.fetchJavaDownloadList", "Fetch Java download list: %s");
        put(t, "operation.downloadJava", "Download and install Java %s");
        put(t, "common.close", "Close");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
