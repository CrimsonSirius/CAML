package org.chs.camlauncher.backend.language;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.language.contract.LanguageConstants;
import org.chs.camlauncher.language.contract.LanguageDescriptor;
import org.chs.camlauncher.language.contract.LanguageErrorCode;
import org.chs.camlauncher.language.contract.LanguageEvent;
import org.chs.camlauncher.language.contract.LanguageEventListener;
import org.chs.camlauncher.language.contract.LanguageException;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageTextProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** 后端语言运行时：全部语言文件 IO、检测、回退、格式化和诊断都由这里负责。 */
public final class LanguageService implements LanguageTextProvider {
    private static final Logger LOGGER = LogManager.getLogger(LanguageService.class);
    private static final String RESOURCE_ROOT = "/lang/";
    private static final Pattern LOCALE_PATTERN = Pattern.compile("[a-z]{2,3}(?:_[a-z0-9]{2,8})*");
    private static final String CHINESE_LANGUAGE = "zh";
    private static final String TRADITIONAL_SCRIPT = "Hant";
    private static final Set<String> TRADITIONAL_REGIONS = Set.of("TW", "HK", "MO");
    private static final Map<String, String> NATIVE_NAMES = Map.of(
            "zh_cn", "简体中文",
            "en_us", "English",
            "fn_cn", "地道中国话"
    );

    private final Path dataDirectory;
    private final Path languageDirectory;
    private final SettingsService settingsService;
    private final List<LanguageEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> missingKeyDiagnostics = ConcurrentHashMap.newKeySet();
    private final AtomicInteger version = new AtomicInteger();

    private volatile LanguageSnapshot snapshot;
    private volatile LanguageSelectionMode selectionMode = LanguageSelectionMode.AUTO;
    private volatile String storedLocale = "";

    public LanguageService(Path dataDirectory, SettingsService settingsService) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.languageDirectory = dataDirectory.resolve("lang");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
    }

    public synchronized LanguageSnapshot initialize() throws IOException {
        Files.createDirectories(dataDirectory);
        Files.createDirectories(languageDirectory);
        installBuiltins();

        selectionMode = readSelectionMode();
        storedLocale = readStoredLocale();
        String requestedLocale = selectionMode == LanguageSelectionMode.MANUAL
                ? normalizeLocale(storedLocale)
                : detectLocale();
        LoadedCatalog catalog = loadCatalog(requestedLocale);
        List<LanguageDescriptor> descriptors = scanDescriptors();
        LanguageSnapshot next = createSnapshot(requestedLocale, catalog, descriptors);
        snapshot = next;
        emitFallback(next);
        emit(new LanguageEvent.StateChanged(
                next.version(), next.requestedLocale(), next.activeLocale(), next.availableLanguages()));
        return next;
    }

    public synchronized LanguageSnapshot switchLanguage(LanguageSelectionMode mode, String locale)
            throws IOException {
        Objects.requireNonNull(mode, "mode");
        settingsService.setLanguage(mode, locale);
        selectionMode = mode;
        storedLocale = locale == null ? "" : locale;
        String requestedLocale = mode == LanguageSelectionMode.MANUAL
                ? normalizeLocale(locale)
                : detectLocale();
        LoadedCatalog catalog = loadCatalog(requestedLocale);
        List<LanguageDescriptor> descriptors = scanDescriptors();
        LanguageSnapshot next = createSnapshot(requestedLocale, catalog, descriptors);
        snapshot = next;
        emitFallback(next);
        emit(new LanguageEvent.StateChanged(
                next.version(), next.requestedLocale(), next.activeLocale(), next.availableLanguages()));
        return next;
    }

    public LanguageSnapshot snapshot() {
        LanguageSnapshot current = snapshot;
        if (current == null) {
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

    public void addEventListener(LanguageEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeEventListener(LanguageEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String text(String key, Object... args) {
        LanguageSnapshot current = snapshot();
        String template = current.messages().get(key);
        if (template == null) {
            String diagnosticKey = current.activeLocale() + "\u0000" + key;
            if (missingKeyDiagnostics.add(diagnosticKey)) {
                LOGGER.warn("缺少翻译键 locale={} key={}", current.activeLocale(), key);
                emit(new LanguageEvent.KeyMissing(current.version(), current.activeLocale(), key));
            }
            return key;
        }
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (RuntimeException e) {
            LOGGER.warn("翻译格式化失败 locale={} key={}", current.activeLocale(), key, e);
            emit(new LanguageEvent.FormatFailed(
                    current.version(), current.activeLocale(), key, e.getMessage()));
            return template;
        }
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
        String normalized = normalizeLocale(locale);
        return availableLocales().contains(normalized);
    }

    @Override
    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    @Override
    public String displayName(String locale) {
        String normalized = normalizeLocale(locale);
        return NATIVE_NAMES.getOrDefault(normalized, normalized);
    }

    public String detectLocale() {
        String override = System.getProperty(LanguageConstants.LANGUAGE_OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return normalizeLocale(override);
        }
        return detectSystemLocale(Locale.getDefault());
    }

    static String detectSystemLocale(Locale systemLocale) {
        if (systemLocale == null || !CHINESE_LANGUAGE.equalsIgnoreCase(systemLocale.getLanguage())) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        if (TRADITIONAL_SCRIPT.equalsIgnoreCase(systemLocale.getScript())) {
            return LanguageConstants.DEFAULT_LOCALE;
        }
        String country = systemLocale.getCountry().toUpperCase(Locale.ROOT);
        return TRADITIONAL_REGIONS.contains(country)
                ? LanguageConstants.DEFAULT_LOCALE
                : LanguageConstants.SIMPLIFIED_CHINESE_LOCALE;
    }

    private LanguageSnapshot createSnapshot(String requestedLocale, LoadedCatalog catalog,
                                            List<LanguageDescriptor> descriptors) {
        int nextVersion = version.incrementAndGet();
        return new LanguageSnapshot(
                nextVersion,
                requestedLocale,
                catalog.activeLocale(),
                catalog.fallbackReason(),
                List.copyOf(descriptors),
                Map.copyOf(catalog.messages())
        );
    }

    private void emitFallback(LanguageSnapshot state) {
        if (state.fallbackReason() == null) {
            return;
        }
        LOGGER.warn("语言回退 requested={} active={} reason={}",
                state.requestedLocale(), state.activeLocale(), state.fallbackReason());
        emit(new LanguageEvent.Fallback(
                state.version(), state.requestedLocale(), state.activeLocale(), state.fallbackReason()));
    }

    private void emit(LanguageEvent event) {
        for (LanguageEventListener listener : listeners) {
            try {
                listener.onLanguageEvent(event);
            } catch (RuntimeException e) {
                LOGGER.warn("语言事件监听器失败: {}", event, e);
            }
        }
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

    private void installBuiltins() throws IOException {
        for (String locale : LanguageConstants.BUNDLED_LOCALES) {
            byte[] bundled = readBundled(locale);
            Map<String, String> bundledCatalog = parseCatalog(bundled, locale + ".json");
            Path installed = languageDirectory.resolve(locale + ".json");
            String reason = repairReason(installed, bundledCatalog);
            if (reason == null) {
                continue;
            }
            writeAtomically(installed, bundled);
            LOGGER.info("同步内置语言文件 locale={} file={} reason={}",
                    locale, installed, reason);
            emit(new LanguageEvent.FileRepaired(
                    version.get(), locale, installed.getFileName().toString(), reason));
        }
    }

    private String repairReason(Path installed, Map<String, String> bundledCatalog) {
        if (!Files.isRegularFile(installed)) {
            return "missing";
        }
        try {
            Map<String, String> installedCatalog = parseCatalog(Files.readAllBytes(installed),
                    installed.getFileName().toString());
            return installedCatalog.equals(bundledCatalog) ? null : "content-differs";
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("内置语言文件损坏，将按内置资源重写 file={}", installed, e);
            emit(new LanguageEvent.FileInvalid(
                    version.get(),
                    installed.getFileName().toString().replace(".json", ""),
                    installed.getFileName().toString(),
                    "invalid-json",
                    e));
            return "invalid-json";
        }
    }

    private LoadedCatalog loadCatalog(String requestedLocale) {
        Map<String, String> requested = loadLocaleMap(requestedLocale);
        Map<String, String> defaults = requestedLocale.equals(LanguageConstants.DEFAULT_LOCALE)
                ? requested
                : loadLocaleMap(LanguageConstants.DEFAULT_LOCALE);
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        merged.putAll(requested);
        if (merged.isEmpty()) {
            throw new LanguageException(
                    LanguageErrorCode.BOOTSTRAP_FAILED, requestedLocale, null, null);
        }

        boolean fellBack = requested.isEmpty()
                && !requestedLocale.equals(LanguageConstants.DEFAULT_LOCALE);
        String activeLocale = fellBack ? LanguageConstants.DEFAULT_LOCALE : requestedLocale;
        String reason = fellBack ? "requested-locale-unavailable" : null;
        return new LoadedCatalog(activeLocale, reason, merged);
    }

    private Map<String, String> loadLocaleMap(String locale) {
        Path runtime = safeLocaleFile(locale);
        if (runtime != null && Files.isRegularFile(runtime)) {
            try {
                Map<String, String> parsed = parseCatalog(
                        Files.readAllBytes(runtime), runtime.getFileName().toString());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
                emitInvalid(locale, runtime, "empty-catalog", null);
            } catch (IOException | RuntimeException e) {
                emitInvalid(locale, runtime, "invalid-json", e);
            }
        }
        try {
            byte[] bundled = readBundled(locale);
            return parseCatalog(bundled, locale + ".json");
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("无法读取内置语言资源 locale={}", locale, e);
            return Map.of();
        }
    }

    private void emitInvalid(String locale, Path file, String reason, Throwable cause) {
        LOGGER.warn("语言文件无效 locale={} file={} reason={}", locale, file, reason, cause);
        emit(new LanguageEvent.FileInvalid(
                version.get(), locale, file.getFileName().toString(), reason, cause));
    }

    private Path safeLocaleFile(String locale) {
        if (locale == null || !LOCALE_PATTERN.matcher(locale).matches()) {
            LOGGER.warn("忽略非法语言标识: {}", locale);
            return null;
        }
        Path resolved = languageDirectory.resolve(locale + ".json").normalize();
        return resolved.getParent().equals(languageDirectory.normalize()) ? resolved : null;
    }

    private List<LanguageDescriptor> scanDescriptors() {
        List<LanguageDescriptor> result = new ArrayList<>();
        Set<String> acknowledged = ConcurrentHashMap.newKeySet();
        for (String locale : LanguageConstants.BUNDLED_LOCALES) {
            result.add(new LanguageDescriptor(locale, displayName(locale), true, false));
            acknowledged.add(locale);
        }

        List<String> custom = new ArrayList<>();
        try (Stream<Path> files = Files.list(languageDirectory)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                if (!Files.isRegularFile(file) || !fileName.endsWith(".json")) {
                    continue;
                }
                String raw = fileName.substring(0, fileName.length() - ".json".length());
                String normalized = normalizeLocale(raw);
                if (!raw.equals(normalized) || !LOCALE_PATTERN.matcher(normalized).matches()) {
                    LOGGER.warn("忽略未规范化的自定义语言文件: {}", file);
                    continue;
                }
                if (acknowledged.add(normalized)) {
                    custom.add(normalized);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("无法扫描语言目录 {}", languageDirectory, e);
        }
        custom.sort(String::compareTo);
        for (String locale : custom) {
            result.add(new LanguageDescriptor(locale, displayName(locale), false, true));
        }
        return List.copyOf(result);
    }

    private byte[] readBundled(String locale) throws IOException {
        try (InputStream input = LanguageService.class.getResourceAsStream(
                RESOURCE_ROOT + locale + ".json")) {
            if (input == null) {
                throw new IOException("缺少内置语言资源: " + RESOURCE_ROOT + locale + ".json");
            }
            return input.readAllBytes();
        }
    }

    private static Map<String, String> parseCatalog(byte[] content, String source) throws IOException {
        try {
            JsonObject object = JsonParser.parseString(new String(content, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    result.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Map.copyOf(result);
        } catch (RuntimeException e) {
            throw new IOException("无法解析语言文件: " + source, e);
        }
    }

    private void writeAtomically(Path target, byte[] content) throws IOException {
        Path temporary = Files.createTempFile(languageDirectory, "caml-lang-", ".tmp");
        try {
            Files.write(temporary, content);
            parseCatalog(Files.readAllBytes(temporary), temporary.getFileName().toString());
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record LoadedCatalog(String activeLocale, String fallbackReason,
                                 Map<String, String> messages) {
    }
}
