package org.chs.camlauncher.backend.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/** 启动器设置持久化：游戏位置、内存、隔离、下载、Java 和语言选择。 */
public final class SettingsService {
    public static final int DEFAULT_MAX_MEMORY_MB = 4096;
    public static final int MIN_MAX_MEMORY_MB = 512;
    public static final String DEFAULT_JVM_ARGUMENTS = "";
    public static final VersionIsolationMode DEFAULT_VERSION_ISOLATION = VersionIsolationMode.FULL;
    public static final int MIN_DOWNLOAD_CONCURRENCY = DownloadConcurrency.MIN;
    public static final int MAX_DOWNLOAD_CONCURRENCY = DownloadConcurrency.MAX;
    public static final int DEFAULT_DOWNLOAD_CONCURRENCY = DownloadConcurrency.DEFAULT;
    public static final DownloadSourceMode DEFAULT_DOWNLOAD_SOURCE = DownloadSourceMode.MIRROR;

    private static final String MAX_MEMORY_KEY = "maxMemoryMb";
    private static final String JVM_ARGUMENTS_KEY = "jvmArguments";
    private static final String GAME_DIRECTORY_KEY = "gameDirectory";
    private static final String VERSION_ISOLATION_KEY = "versionIsolation";
    private static final String DOWNLOAD_CONCURRENCY_KEY = "downloadConcurrency";
    private static final String DOWNLOAD_SOURCE_KEY = "downloadSource";
    private static final String CUSTOM_DOWNLOAD_API_KEY = "customDownloadApi";
    private static final String LAST_LAUNCHED_INSTANCE_KEY = "lastInstanceId";
    private static final String JAVA_SELECTION_MODE_KEY = "javaSelectionMode";
    private static final String SELECTED_JAVA_PATH_KEY = "selectedJavaPath";
    private static final String MANUAL_JAVA_PATHS_KEY = "manualJavaPaths";
    private static final String LANGUAGE_MODE_KEY = "languageMode";
    private static final String LANGUAGE_KEY = "language";
    private static final String LANGUAGE_FALLBACK_MODE_KEY = "languageFallbackMode";
    private static final String LANGUAGE_FALLBACK_LOCALE_KEY = "languageFallbackLocale";
    private static final String AUTO_GAME_LANGUAGE_KEY = "autoGameLanguage";
    private static final String DISABLED_MODS_KEY = "disabledMods";
    private static final String DEFAULT_ACCOUNT_KEY = "defaultAccountId";
    private static final String INSTANCE_ACCOUNTS_KEY = "instanceAccounts";
    private static final String CLIENT_TOKEN_KEY = "clientToken";
    private static final String AUTHLIB_INJECTOR_KEY = "authlibInjectorPath";
    private static final Gson GSON = new Gson();
    private static final Type DISABLED_MODS_TYPE =
            new TypeToken<Map<String, List<String>>>() { }.getType();
    private static final Type INSTANCE_ACCOUNTS_TYPE =
            new TypeToken<Map<String, String>>() { }.getType();

    private final Path settingsFile;
    private final Object writeLock = new Object();

    public SettingsService(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.settingsFile = dataDirectory.resolve("settings.properties");
    }

    public int maxMemoryMb() throws IOException {
        String raw = load().getProperty(MAX_MEMORY_KEY);
        if (raw == null) {
            return DEFAULT_MAX_MEMORY_MB;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= MIN_MAX_MEMORY_MB ? value : DEFAULT_MAX_MEMORY_MB;
        } catch (NumberFormatException ignored) {
            return DEFAULT_MAX_MEMORY_MB;
        }
    }

    public void setMaxMemoryMb(int value) throws IOException {
        if (value < MIN_MAX_MEMORY_MB) {
            throw new IllegalArgumentException("最大内存不能小于 " + MIN_MAX_MEMORY_MB + " MB");
        }

        update(properties -> properties.setProperty(MAX_MEMORY_KEY, Integer.toString(value)));
    }

    public String jvmArguments() throws IOException {
        String raw = load().getProperty(JVM_ARGUMENTS_KEY);
        return raw == null ? DEFAULT_JVM_ARGUMENTS : raw.trim();
    }

    public void setJvmArguments(String arguments) throws IOException {
        String normalized = arguments == null ? DEFAULT_JVM_ARGUMENTS : arguments.trim();
        update(properties -> {
            if (normalized.isEmpty()) {
                properties.remove(JVM_ARGUMENTS_KEY);
            } else {
                properties.setProperty(JVM_ARGUMENTS_KEY, normalized);
            }
        });
    }

    public Optional<Path> gameDirectory() throws IOException {
        String raw = load().getProperty(GAME_DIRECTORY_KEY);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(raw).toAbsolutePath().normalize());
        } catch (InvalidPathException ignored) {
            return Optional.empty();
        }
    }

    public void setGameDirectory(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        update(properties -> properties.setProperty(GAME_DIRECTORY_KEY,
                directory.toAbsolutePath().normalize().toString()));
    }

    public VersionIsolationMode versionIsolationMode() throws IOException {
        String raw = load().getProperty(VERSION_ISOLATION_KEY);
        if (raw == null) {
            return DEFAULT_VERSION_ISOLATION;
        }
        return switch (raw.trim().toLowerCase()) {
            case "full" -> VersionIsolationMode.FULL;
            case "none" -> VersionIsolationMode.NONE;
            default -> DEFAULT_VERSION_ISOLATION;
        };
    }

    public void setVersionIsolationMode(VersionIsolationMode mode) throws IOException {
        Objects.requireNonNull(mode, "mode");
        if (mode == VersionIsolationMode.AUTO) {
            throw new IllegalArgumentException("界面设置不能持久化自动隔离模式");
        }
        update(properties -> properties.setProperty(
                VERSION_ISOLATION_KEY, mode.name().toLowerCase()));
    }

    public int downloadConcurrency() throws IOException {
        String raw = load().getProperty(DOWNLOAD_CONCURRENCY_KEY);
        if (raw == null) {
            return DEFAULT_DOWNLOAD_CONCURRENCY;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= MIN_DOWNLOAD_CONCURRENCY && value <= MAX_DOWNLOAD_CONCURRENCY
                    ? value
                    : DEFAULT_DOWNLOAD_CONCURRENCY;
        } catch (NumberFormatException ignored) {
            return DEFAULT_DOWNLOAD_CONCURRENCY;
        }
    }

    public void setDownloadConcurrency(int value) throws IOException {
        if (value < MIN_DOWNLOAD_CONCURRENCY || value > MAX_DOWNLOAD_CONCURRENCY) {
            throw new IllegalArgumentException(
                    "下载并发数必须在 " + MIN_DOWNLOAD_CONCURRENCY + " 到 "
                            + MAX_DOWNLOAD_CONCURRENCY + " 之间");
        }
        update(properties -> properties.setProperty(
                DOWNLOAD_CONCURRENCY_KEY, Integer.toString(value)));
    }

    public DownloadSourceMode downloadSourceMode() throws IOException {
        return DownloadSourceMode.fromStored(load().getProperty(DOWNLOAD_SOURCE_KEY));
    }

    public String customDownloadApi() throws IOException {
        String value = load().getProperty(CUSTOM_DOWNLOAD_API_KEY);
        return value == null ? "" : value.trim();
    }

    public void setDownloadSource(DownloadSourceMode mode, String customApi) throws IOException {
        Objects.requireNonNull(mode, "mode");
        update(properties -> {
            properties.setProperty(DOWNLOAD_SOURCE_KEY, mode.storedName());
            properties.setProperty(CUSTOM_DOWNLOAD_API_KEY,
                    customApi == null ? "" : customApi.trim());
        });
    }

    public Optional<String> lastLaunchedInstanceId() throws IOException {
        String raw = load().getProperty(LAST_LAUNCHED_INSTANCE_KEY);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim());
    }

    public void setLastLaunchedInstanceId(String instanceId) throws IOException {
        String value = Objects.requireNonNull(instanceId, "instanceId").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("最后启动的实例不能为空");
        }
        update(properties -> properties.setProperty(LAST_LAUNCHED_INSTANCE_KEY, value));
    }

    public JavaSelectionMode javaSelectionMode() throws IOException {
        return JavaSelectionMode.fromStored(load().getProperty(JAVA_SELECTION_MODE_KEY));
    }

    public Optional<Path> selectedJavaPath() throws IOException {
        return parsePath(load().getProperty(SELECTED_JAVA_PATH_KEY));
    }

    public List<Path> manualJavaPaths() throws IOException {
        String raw = load().getProperty(MANUAL_JAVA_PATHS_KEY);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Path> paths = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            parsePath(line).ifPresent(paths::add);
        }
        return List.copyOf(paths);
    }

    public void addManualJavaPath(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        String value = path.toAbsolutePath().normalize().toString();
        update(properties -> {
            List<String> values = new ArrayList<>();
            String raw = properties.getProperty(MANUAL_JAVA_PATHS_KEY);
            if (raw != null && !raw.isBlank()) {
                for (String line : raw.split("\\R")) {
                    if (!line.isBlank()) {
                        values.add(line.trim());
                    }
                }
            }
            if (values.stream().noneMatch(value::equals)) {
                values.add(value);
            }
            properties.setProperty(MANUAL_JAVA_PATHS_KEY, String.join("\n", values));
        });
    }

    public void setJavaSelection(JavaSelectionMode mode, Path selectedJavaPath) throws IOException {
        Objects.requireNonNull(mode, "mode");
        update(properties -> {
            properties.setProperty(JAVA_SELECTION_MODE_KEY, mode.storedName());
            if (selectedJavaPath == null) {
                properties.remove(SELECTED_JAVA_PATH_KEY);
            } else {
                properties.setProperty(SELECTED_JAVA_PATH_KEY,
                        selectedJavaPath.toAbsolutePath().normalize().toString());
            }
        });
    }

    public LanguageSelectionMode languageSelectionMode() throws IOException {
        String value = load().getProperty(LANGUAGE_MODE_KEY);
        return value != null && "manual".equalsIgnoreCase(value.trim())
                ? LanguageSelectionMode.MANUAL
                : LanguageSelectionMode.AUTO;
    }

    public Optional<String> language() throws IOException {
        String raw = load().getProperty(LANGUAGE_KEY);
        return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(raw.trim());
    }

    public void setLanguage(LanguageSelectionMode mode, String locale) throws IOException {
        Objects.requireNonNull(mode, "mode");
        String value = locale == null
                ? ""
                : locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        update(properties -> {
            properties.setProperty(LANGUAGE_MODE_KEY, mode.name().toLowerCase(Locale.ROOT));
            if (value.isEmpty()) {
                properties.remove(LANGUAGE_KEY);
            } else {
                properties.setProperty(LANGUAGE_KEY, value);
            }
        });
    }

    public LanguageFallbackMode languageFallbackMode() throws IOException {
        String value = load().getProperty(LANGUAGE_FALLBACK_MODE_KEY);
        if (value == null) {
            return LanguageFallbackMode.KEY;
        }
        try {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            return "AUTO".equals(normalized)
                    ? LanguageFallbackMode.SYSTEM
                    : LanguageFallbackMode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return LanguageFallbackMode.KEY;
        }
    }

    public Optional<String> languageFallbackLocale() throws IOException {
        String raw = load().getProperty(LANGUAGE_FALLBACK_LOCALE_KEY);
        return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(raw.trim());
    }

    public void setLanguageFallback(LanguageFallbackMode mode, String locale) throws IOException {
        Objects.requireNonNull(mode, "mode");
        String value = locale == null
                ? ""
                : locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        update(properties -> {
            properties.setProperty(LANGUAGE_FALLBACK_MODE_KEY, mode.name().toLowerCase(Locale.ROOT));
            if (value.isEmpty()) {
                properties.remove(LANGUAGE_FALLBACK_LOCALE_KEY);
            } else {
                properties.setProperty(LANGUAGE_FALLBACK_LOCALE_KEY, value);
            }
        });
    }

    public boolean autoGameLanguage() throws IOException {
        String raw = load().getProperty(AUTO_GAME_LANGUAGE_KEY);
        return raw == null || !"false".equalsIgnoreCase(raw.trim());
    }

    public void setAutoGameLanguage(boolean enabled) throws IOException {
        update(properties -> properties.setProperty(
                AUTO_GAME_LANGUAGE_KEY, Boolean.toString(enabled)));
    }

    /** 读取某个实例在 CAML 配置中的禁用 Mod 文件名。 */
    public Set<String> disabledMods(Path gameDirectory, String instanceId) throws IOException {
        Map<String, List<String>> states = parseDisabledMods(
                load().getProperty(DISABLED_MODS_KEY));
        List<String> files = states.getOrDefault(modStateKey(gameDirectory, instanceId), List.of());
        return Set.copyOf(new LinkedHashSet<>(files.stream().filter(Objects::nonNull).toList()));
    }

    /** 更新某个实例的 Mod 禁用状态；文件名按未带 .disabled 的 jar 名保存。 */
    public void setModDisabled(Path gameDirectory, String instanceId, String fileName,
                               boolean disabled) throws IOException {
        Objects.requireNonNull(fileName, "fileName");
        update(properties -> {
            Map<String, List<String>> states = parseDisabledMods(
                    properties.getProperty(DISABLED_MODS_KEY));
            String key = modStateKey(gameDirectory, instanceId);
            LinkedHashSet<String> files = new LinkedHashSet<>(
                    states.getOrDefault(key, List.of()));
            if (disabled) {
                files.add(fileName);
            } else {
                files.remove(fileName);
            }
            if (files.isEmpty()) {
                states.remove(key);
            } else {
                states.put(key, List.copyOf(files));
            }
            if (states.isEmpty()) {
                properties.remove(DISABLED_MODS_KEY);
            } else {
                properties.setProperty(DISABLED_MODS_KEY, GSON.toJson(states));
            }
        });
    }

    // ------------------------------------------------------------------ 账号

    /** 全局默认账号；没有设置时返回空。 */
    public Optional<String> defaultAccountId() throws IOException {
        String raw = load().getProperty(DEFAULT_ACCOUNT_KEY);
        return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(raw.trim());
    }

    public void setDefaultAccountId(String accountId) throws IOException {
        update(properties -> {
            if (accountId == null || accountId.isBlank()) {
                properties.remove(DEFAULT_ACCOUNT_KEY);
            } else {
                properties.setProperty(DEFAULT_ACCOUNT_KEY, accountId.trim());
            }
        });
    }

    /** 某个实例指定的账号；没有单独设置时返回空，由调用方回退到全局默认账号。 */
    public Optional<String> instanceAccountId(Path gameDirectory, String instanceId)
            throws IOException {
        Map<String, String> states = parseInstanceAccounts(
                load().getProperty(INSTANCE_ACCOUNTS_KEY));
        String value = states.get(modStateKey(gameDirectory, instanceId));
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    /** 设置实例账号；{@code accountId} 为空表示清除该实例的单独设置。 */
    public void setInstanceAccountId(Path gameDirectory, String instanceId, String accountId)
            throws IOException {
        update(properties -> {
            Map<String, String> states = parseInstanceAccounts(
                    properties.getProperty(INSTANCE_ACCOUNTS_KEY));
            String key = modStateKey(gameDirectory, instanceId);
            if (accountId == null || accountId.isBlank()) {
                states.remove(key);
            } else {
                states.put(key, accountId.trim());
            }
            if (states.isEmpty()) {
                properties.remove(INSTANCE_ACCOUNTS_KEY);
            } else {
                properties.setProperty(INSTANCE_ACCOUNTS_KEY, GSON.toJson(states));
            }
        });
    }

    /** 删除账号时清理所有指向它的实例绑定。 */
    public void clearInstanceAccount(String accountId) throws IOException {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        update(properties -> {
            Map<String, String> states = parseInstanceAccounts(
                    properties.getProperty(INSTANCE_ACCOUNTS_KEY));
            if (states.values().removeIf(accountId::equals) && !states.isEmpty()) {
                properties.setProperty(INSTANCE_ACCOUNTS_KEY, GSON.toJson(states));
            } else if (states.isEmpty()) {
                properties.remove(INSTANCE_ACCOUNTS_KEY);
            }
        });
    }

    /** 稳定的 clientToken：第一次使用时生成并持久化，用于 refresh 校验。 */
    public String clientToken() throws IOException {
        String existing = load().getProperty(CLIENT_TOKEN_KEY);
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        String created = java.util.UUID.randomUUID().toString().replace("-", "");
        update(properties -> properties.setProperty(CLIENT_TOKEN_KEY, created));
        return created;
    }

    /** 用户自定义的 authlib-injector jar；为空表示使用内置固定版本。 */
    public Optional<Path> authlibInjectorPath() throws IOException {
        return parsePath(load().getProperty(AUTHLIB_INJECTOR_KEY));
    }

    public void setAuthlibInjectorPath(Path jar) throws IOException {
        update(properties -> {
            if (jar == null) {
                properties.remove(AUTHLIB_INJECTOR_KEY);
            } else {
                properties.setProperty(AUTHLIB_INJECTOR_KEY,
                        jar.toAbsolutePath().normalize().toString());
            }
        });
    }

    private static Map<String, String> parseInstanceAccounts(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> parsed = GSON.fromJson(raw, INSTANCE_ACCOUNTS_TYPE);
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, List<String>> parseDisabledMods(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, List<String>> parsed = GSON.fromJson(raw, DISABLED_MODS_TYPE);
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }

    private static String modStateKey(Path gameDirectory, String instanceId) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(instanceId, "instanceId");
        String value = gameDirectory.toAbsolutePath().normalize() + "\n" + instanceId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private Properties load() throws IOException {
        synchronized (writeLock) {
            Properties properties = new Properties();
            if (Files.isRegularFile(settingsFile)) {
                try (InputStream input = Files.newInputStream(settingsFile)) {
                    properties.load(input);
                }
            }
            return properties;
        }
    }

    private static Optional<Path> parsePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(raw.trim()).toAbsolutePath().normalize());
        } catch (InvalidPathException ignored) {
            return Optional.empty();
        }
    }

    private void update(Consumer<Properties> change) throws IOException {
        synchronized (writeLock) {
            Properties properties = load();
            change.accept(properties);
            store(properties);
        }
    }

    private void store(Properties properties) throws IOException {
        Path parent = settingsFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "caml-settings-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "CAML settings");
            }
            try {
                Files.move(temporary, settingsFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
