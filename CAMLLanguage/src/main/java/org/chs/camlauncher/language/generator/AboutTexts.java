package org.chs.camlauncher.language.generator;

/** “关于”设置页的共享英文默认值。 */
final class AboutTexts {
    private AboutTexts() {
    }

    static void populateDefaults(TranslationTable t) {
        put(t, "settings.section.about", "About");
        put(t, "settings.about.application.title", "Launcher");
        put(t, "settings.about.version", "Version");
        put(t, "settings.about.backend", "Backend");
        put(t, "settings.about.license", "License");
        put(t, "settings.about.system.title", "System");
        put(t, "settings.about.os", "Operating system");
        put(t, "settings.about.osVersion", "System version");
        put(t, "settings.about.kernelVersion", "Kernel version");
        put(t, "settings.about.architecture", "Architecture");
        put(t, "settings.about.runtime.title", "Launcher runtime");
        put(t, "settings.about.javaVersion", "Java version");
        put(t, "settings.about.javaVendor", "Java vendor");
        put(t, "settings.about.device.title", "Device information");
        put(t, "settings.about.cpu", "CPU");
        put(t, "settings.about.gpu", "GPU");
        put(t, "settings.about.npu", "NPU");
        put(t, "settings.about.memory", "System memory");
        put(t, "settings.about.notDetected", "Not detected");
        put(t, "settings.about.directories.title", "Directories");
        put(t, "settings.about.dataDirectory", "Data directory");
        put(t, "settings.about.gameDirectory", "Game directory");
        put(t, "settings.about.project.title", "Project");
        put(t, "settings.about.projectName", "Name");
        put(t, "settings.about.copyright", "Copyright");
        put(t, "settings.about.dependencies.title", "Project dependencies");
        put(t, "settings.about.dependency.author", "Author: %s");
        put(t, "settings.about.dependency.license", "License: %s");
        put(t, "settings.about.unknown", "Unknown");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
