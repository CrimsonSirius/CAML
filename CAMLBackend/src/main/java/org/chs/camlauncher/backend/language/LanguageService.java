package org.chs.camlauncher.backend.language;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageDescriptor;
import org.chs.camlauncher.language.contract.LanguageErrorCode;
import org.chs.camlauncher.language.contract.LanguageEvent;
import org.chs.camlauncher.language.contract.LanguageEventListener;
import org.chs.camlauncher.language.contract.LanguageException;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageTextProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** 语言运行时编排器；具体文件、目录、发现、事件和渲染逻辑由协作类负责。 */
public final class LanguageService implements LanguageTextProvider {
    private static final Logger LOGGER = LogManager.getLogger(LanguageService.class);

    private final Path dataDirectory;
    private final Path languageDirectory;
    private final SettingsService settingsService;
    private final BuiltinLanguageInstaller installer;
    private final LanguageCatalogLoader catalogLoader;
    private final LanguageDiscovery discovery;
    private final LanguageEventPublisher events = new LanguageEventPublisher();
    private final TranslationRenderer renderer = new TranslationRenderer(events);
    private final AtomicInteger version = new AtomicInteger();

    private volatile LanguageSnapshot snapshot;
    private volatile LanguageSelectionMode selectionMode = LanguageSelectionMode.AUTO;
    private volatile String storedLocale = "";
    private volatile LanguageFallbackMode fallbackMode = LanguageFallbackMode.KEY;
    private volatile String fallbackLocale = LanguageConstants.DEFAULT_LOCALE;

    public LanguageService(Path dataDirectory, SettingsService settingsService) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.languageDirectory = dataDirectory.resolve("lang");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.installer = new BuiltinLanguageInstaller(languageDirectory);
        this.catalogLoader = new LanguageCatalogLoader(languageDirectory, this::reportInvalidFile);
        this.discovery = new LanguageDiscovery(languageDirectory);
    }

    public synchronized LanguageSnapshot initialize() throws IOException {
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(languageDirectory);
            for (BuiltinLanguageInstaller.Repair repair : installer.install()) {
                reportRepair(repair);
            }

            selectionMode = readSelectionMode();
            storedLocale = readStoredLocale();
            fallbackMode = readFallbackMode();
            fallbackLocale = readFallbackLocale();
            String requestedLocale = selectionMode == LanguageSelectionMode.MANUAL
                    ? normalizeLocale(storedLocale)
                    : detectLocale();
            LanguageSnapshot next = createSnapshot(
                    requestedLocale,
                    loadCatalog(requestedLocale),
                    discovery.scan()
            );
            snapshot = next;
            publishState(next);
            return next;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("语言系统初始化失败 dataDirectory={}", dataDirectory, e);
            throw e;
        }
    }

    public synchronized LanguageSnapshot switchLanguage(LanguageSelectionMode mode, String locale)
            throws IOException {
        try {
            Objects.requireNonNull(mode, "mode");
            settingsService.setLanguage(mode, locale);
            selectionMode = mode;
            storedLocale = locale == null ? "" : locale;
            String requestedLocale = mode == LanguageSelectionMode.MANUAL
                    ? normalizeLocale(locale)
                    : detectLocale();
            LanguageSnapshot next = createSnapshot(
                    requestedLocale,
                    loadCatalog(requestedLocale),
                    discovery.scan()
            );
            snapshot = next;
            publishState(next);
            return next;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("切换语言失败 mode={} locale={}", mode, locale, e);
            throw e;
        }
    }

    public synchronized LanguageSnapshot setLanguageFallback(
            LanguageFallbackMode mode, String locale) throws IOException {
        try {
            Objects.requireNonNull(mode, "mode");
            settingsService.setLanguageFallback(mode, locale);
            fallbackMode = mode;
            fallbackLocale = locale == null || locale.isBlank()
                    ? LanguageConstants.DEFAULT_LOCALE
                    : normalizeLocale(locale);
            String requestedLocale = snapshot().requestedLocale();
            LanguageSnapshot next = createSnapshot(
                    requestedLocale,
                    loadCatalog(requestedLocale),
                    discovery.scan()
            );
            snapshot = next;
            publishState(next);
            return next;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("设置语言回退失败 mode={} locale={}", mode, locale, e);
            throw e;
        }
    }

    public LanguageSnapshot snapshot() {
        LanguageSnapshot current = snapshot;
        if (current == null) {
            LOGGER.error("语言快照尚未初始化，拒绝提供语言状态");
            throw new LanguageException(
                    LanguageErrorCode.BOOTSTRAP_FAILED, null, null, null);
        }
        return current;
    }

    public LanguageSelectionMode selectionMode() {
        return selectionMode;
    }

    public String storedLocale() {
        return storedLocale;
    }

    public LanguageFallbackMode fallbackMode() {
        return fallbackMode;
    }

    public String fallbackLocale() {
        return fallbackLocale;
    }

    public void addEventListener(LanguageEventListener listener) {
        events.add(listener);
    }

    public void removeEventListener(LanguageEventListener listener) {
        events.remove(listener);
    }

    @Override
    public String text(String key, Object... args) {
        return renderer.render(snapshot(), key, args);
    }

    @Override
    public String activeLocale() {
        return snapshot().activeLocale();
    }

    @Override
    public String requestedLocale() {
        return snapshot().requestedLocale();
    }

    @Override
    public String defaultLocale() {
        return LanguageConstants.DEFAULT_LOCALE;
    }

    @Override
    public List<LanguageDescriptor> availableLanguages() {
        return snapshot().availableLanguages();
    }

    @Override
    public List<String> availableLocales() {
        return snapshot().availableLanguages().stream().map(LanguageDescriptor::locale).toList();
    }

    @Override
    public boolean supportsLocale(String locale) {
        return availableLocales().contains(normalizeLocale(locale));
    }

    @Override
    public String normalizeLocale(String locale) {
        return LanguageLocales.normalize(locale);
    }

    @Override
    public String displayName(String locale) {
        return LanguageLocales.displayName(locale);
    }

    public String detectLocale() {
        return LanguageLocales.detectLocale();
    }

    static String detectSystemLocale(Locale systemLocale) {
        return LanguageLocales.detectSystemLocale(systemLocale);
    }

    private LanguageSnapshot createSnapshot(String requestedLocale,
                                            LanguageCatalogLoader.Catalog catalog,
                                            List<LanguageDescriptor> descriptors) {
        return new LanguageSnapshot(
                version.incrementAndGet(),
                requestedLocale,
                catalog.activeLocale(),
                catalog.fallbackReason(),
                fallbackMode,
                fallbackLocale,
                List.copyOf(descriptors),
                catalog.messages()
        );
    }

    private LanguageCatalogLoader.Catalog loadCatalog(String requestedLocale) {
        LanguageCatalogLoader.Catalog catalog = catalogLoader.load(
                requestedLocale, fallbackTargets(requestedLocale));
        if (catalog.messages().isEmpty() && fallbackMode == LanguageFallbackMode.DEFAULT) {
            throw new LanguageException(
                    LanguageErrorCode.BOOTSTRAP_FAILED, requestedLocale, null, null);
        }
        return catalog;
    }

    private List<String> fallbackTargets(String requestedLocale) {
        Set<String> targets = new LinkedHashSet<>();
        switch (fallbackMode) {
            case KEY -> {
                return List.of();
            }
            case DEFAULT -> targets.add(LanguageConstants.DEFAULT_LOCALE);
            case SYSTEM -> targets.add(detectLocale());
            case CUSTOM -> {
                if (fallbackLocale != null && !fallbackLocale.isBlank()) {
                    targets.add(normalizeLocale(fallbackLocale));
                }
            }
        }
        targets.remove(requestedLocale);
        return List.copyOf(targets);
    }

    private void publishState(LanguageSnapshot state) {
        if (state.fallbackReason() != null) {
            LOGGER.warn("语言回退 requested={} active={} reason={}",
                    state.requestedLocale(), state.activeLocale(), state.fallbackReason());
            events.publish(new LanguageEvent.Fallback(
                    state.version(), state.requestedLocale(), state.activeLocale(),
                    state.fallbackReason()));
        }
        events.publish(new LanguageEvent.StateChanged(
                state.version(), state.requestedLocale(), state.activeLocale(),
                state.availableLanguages()));
    }

    private void reportRepair(BuiltinLanguageInstaller.Repair repair) {
        if (repair.cause() == null) {
            LOGGER.info("同步内置语言文件 locale={} file={} reason={}",
                    repair.locale(), repair.fileName(), repair.reason());
        } else {
            LOGGER.warn("内置语言文件损坏，按内置资源重写 locale={} file={} reason={}",
                    repair.locale(), repair.fileName(), repair.reason(), repair.cause());
            events.publish(new LanguageEvent.FileInvalid(
                    version.get(), repair.locale(), repair.fileName(),
                    repair.reason(), repair.cause()));
        }
        events.publish(new LanguageEvent.FileRepaired(
                version.get(), repair.locale(), repair.fileName(), repair.reason()));
    }

    private void reportInvalidFile(LanguageCatalogLoader.InvalidFile invalid) {
        LOGGER.warn("语言文件无效 locale={} file={} reason={}",
                invalid.locale(), invalid.fileName(), invalid.reason(), invalid.cause());
        events.publish(new LanguageEvent.FileInvalid(
                version.get(), invalid.locale(), invalid.fileName(),
                invalid.reason(), invalid.cause()));
    }

    private LanguageSelectionMode readSelectionMode() {
        try {
            return settingsService.languageSelectionMode();
        } catch (IOException e) {
            LOGGER.warn("无法读取语言选择模式，使用自动选择", e);
            return LanguageSelectionMode.AUTO;
        }
    }

    private String readStoredLocale() {
        try {
            return settingsService.language().orElse("");
        } catch (IOException e) {
            LOGGER.warn("无法读取语言设置，使用自动选择", e);
            return "";
        }
    }

    private LanguageFallbackMode readFallbackMode() {
        try {
            return settingsService.languageFallbackMode();
        } catch (IOException e) {
            LOGGER.warn("无法读取语言回退模式，使用自动回退", e);
            return LanguageFallbackMode.KEY;
        }
    }

    private String readFallbackLocale() {
        try {
            return settingsService.languageFallbackLocale()
                    .map(this::normalizeLocale)
                    .orElse(LanguageConstants.DEFAULT_LOCALE);
        } catch (IOException e) {
            LOGGER.warn("无法读取自定义回退语言，使用默认语言", e);
            return LanguageConstants.DEFAULT_LOCALE;
        }
    }
}
