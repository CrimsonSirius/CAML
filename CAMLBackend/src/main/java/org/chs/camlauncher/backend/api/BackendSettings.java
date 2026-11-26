package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.language.LanguageSnapshot;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.backend.settings.WindowState;
import org.chs.camlauncher.backend.system.DeviceInfo;
import org.chs.camlauncher.backend.system.ProjectDependency;
import org.chs.camlauncher.backend.system.SystemInfo;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.backend.system.SystemMemoryService;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.backend.theme.ThemeService;
import org.chs.camlauncher.language.contract.LanguageEventListener;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 主题、语言、内存、游戏位置和常规持久化设置入口。 */
public final class BackendSettings {
    private static final Logger LOGGER = LogManager.getLogger(BackendSettings.class);

    private final ThemeService themeService;
    private final SettingsService settingsService;
    private final LanguageService languageService;
    private final SystemMemoryService systemMemoryService;
    private final SystemInfo systemInfo;
    private final DeviceInfo deviceInfo;
    private final List<ProjectDependency> projectDependencies;
    private volatile boolean autoGameLanguage = true;

    BackendSettings(
            ThemeService themeService,
            SettingsService settingsService,
            LanguageService languageService,
            SystemMemoryService systemMemoryService,
            SystemInfo systemInfo,
            DeviceInfo deviceInfo,
            List<ProjectDependency> projectDependencies
    ) {
        this.themeService = Objects.requireNonNull(themeService, "themeService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.languageService = Objects.requireNonNull(languageService, "languageService");
        this.systemMemoryService = Objects.requireNonNull(systemMemoryService, "systemMemoryService");
        this.systemInfo = Objects.requireNonNull(systemInfo, "systemInfo");
        this.deviceInfo = Objects.requireNonNull(deviceInfo, "deviceInfo");
        this.projectDependencies = List.copyOf(
                Objects.requireNonNull(projectDependencies, "projectDependencies"));
    }

    void ensureThemes() throws IOException {
        themeService.ensureBundledThemes();
    }

    void initializeLanguage() throws IOException {
        languageService.initialize();
    }

    void initializeAutoGameLanguage() {
        try {
            autoGameLanguage = settingsService.autoGameLanguage();
        } catch (IOException e) {
            LOGGER.warn("无法读取自动游戏语言设置，使用默认值 true", e);
            autoGameLanguage = true;
        }
    }

    Path themesDirectory() {
        return themeService.themesDirectory();
    }

    public List<ThemeInfo> listThemes() throws IOException {
        return themeService.listThemes();
    }

    public ThemeInfo activeTheme() throws IOException {
        return themeService.activeTheme();
    }

    public void selectTheme(String id) throws IOException {
        themeService.selectTheme(id);
    }

    public int maxMemoryMb() throws IOException {
        return settingsService.maxMemoryMb();
    }

    public void setMaxMemoryMb(int value) throws IOException {
        settingsService.setMaxMemoryMb(value);
    }

    public String jvmArguments() throws IOException {
        return settingsService.jvmArguments();
    }

    public void setJvmArguments(String arguments) throws IOException {
        settingsService.setJvmArguments(arguments);
    }

    public List<String> parseJvmArguments(String arguments) {
        return org.chs.camlauncher.backend.launch.command.JvmArgumentsParser.parse(arguments);
    }

    public SystemMemoryInfo systemMemory() {
        return systemMemoryService.read();
    }

    public SystemInfo systemInfo() {
        return systemInfo;
    }

    public DeviceInfo deviceInfo() {
        return deviceInfo;
    }

    public List<ProjectDependency> projectDependencies() {
        return projectDependencies;
    }

    public Optional<Path> savedGameDirectory() throws IOException {
        return settingsService.gameDirectory();
    }

    public void setGameDirectory(Path directory) throws IOException {
        settingsService.setGameDirectory(directory);
    }

    public boolean directoryExists(Path directory) {
        return directory != null && Files.isDirectory(directory);
    }

    public Optional<WindowState> savedWindowState() throws IOException {
        return settingsService.windowState();
    }

    public void saveWindowState(WindowState state) throws IOException {
        settingsService.setWindowState(state);
    }

    public VersionIsolationMode versionIsolationMode() throws IOException {
        return settingsService.versionIsolationMode();
    }

    public void setVersionIsolationMode(VersionIsolationMode mode) throws IOException {
        settingsService.setVersionIsolationMode(mode);
    }

    public boolean autoGameLanguage() throws IOException {
        autoGameLanguage = settingsService.autoGameLanguage();
        return autoGameLanguage;
    }

    boolean autoGameLanguageValue() {
        return autoGameLanguage;
    }

    public void setAutoGameLanguage(boolean enabled) throws IOException {
        settingsService.setAutoGameLanguage(enabled);
        autoGameLanguage = enabled;
    }

    public Optional<String> lastLaunchedInstanceId() throws IOException {
        return settingsService.lastLaunchedInstanceId();
    }

    public void setLastLaunchedInstanceId(String instanceId) throws IOException {
        settingsService.setLastLaunchedInstanceId(instanceId);
    }

    public LanguageSelectionMode languageSelectionMode() throws IOException {
        return languageService.selectionMode();
    }

    public Optional<String> language() throws IOException {
        String locale = languageService.storedLocale();
        return locale == null || locale.isBlank() ? Optional.empty() : Optional.of(locale);
    }

    public LanguageSnapshot setLanguage(LanguageSelectionMode mode, String locale) throws IOException {
        return languageService.switchLanguage(mode, locale);
    }

    public LanguageFallbackMode languageFallbackMode() {
        return languageService.fallbackMode();
    }

    public String languageFallbackLocale() {
        return languageService.fallbackLocale();
    }

    public LanguageSnapshot setLanguageFallback(LanguageFallbackMode mode, String locale)
            throws IOException {
        return languageService.setLanguageFallback(mode, locale);
    }

    public LanguageService languageService() {
        return languageService;
    }

    public LanguageSnapshot languageSnapshot() {
        return languageService.snapshot();
    }

    public void addLanguageEventListener(LanguageEventListener listener) {
        languageService.addEventListener(listener);
    }

    public void removeLanguageEventListener(LanguageEventListener listener) {
        languageService.removeEventListener(listener);
    }
}
