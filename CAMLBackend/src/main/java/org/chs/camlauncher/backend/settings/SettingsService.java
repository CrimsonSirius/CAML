package org.chs.camlauncher.backend.settings;

import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** 启动器设置持久化：游戏位置、最大内存、版本隔离和下载并发数。 */
public final class SettingsService {
    public static final int DEFAULT_MAX_MEMORY_MB = 4096;
    public static final int MIN_MAX_MEMORY_MB = 512;
    public static final VersionIsolationMode DEFAULT_VERSION_ISOLATION = VersionIsolationMode.FULL;
    public static final int MIN_DOWNLOAD_CONCURRENCY = DownloadConcurrency.MIN;
    public static final int MAX_DOWNLOAD_CONCURRENCY = DownloadConcurrency.MAX;
    public static final int DEFAULT_DOWNLOAD_CONCURRENCY = DownloadConcurrency.DEFAULT;
    public static final DownloadSourceMode DEFAULT_DOWNLOAD_SOURCE = DownloadSourceMode.MIRROR;

    private static final String MAX_MEMORY_KEY = "maxMemoryMb";
    private static final String GAME_DIRECTORY_KEY = "gameDirectory";
    private static final String VERSION_ISOLATION_KEY = "versionIsolation";
    private static final String DOWNLOAD_CONCURRENCY_KEY = "downloadConcurrency";
    private static final String DOWNLOAD_SOURCE_KEY = "downloadSource";
    private static final String CUSTOM_DOWNLOAD_API_KEY = "customDownloadApi";
    private static final String LAST_LAUNCHED_INSTANCE_KEY = "lastInstanceId";

    private final Path settingsFile;

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

        Properties properties = load();
        properties.setProperty(MAX_MEMORY_KEY, Integer.toString(value));
        store(properties);
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
        Properties properties = load();
        properties.setProperty(GAME_DIRECTORY_KEY,
                directory.toAbsolutePath().normalize().toString());
        store(properties);
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
        Properties properties = load();
        properties.setProperty(VERSION_ISOLATION_KEY, mode.name().toLowerCase());
        store(properties);
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
        Properties properties = load();
        properties.setProperty(DOWNLOAD_CONCURRENCY_KEY, Integer.toString(value));
        store(properties);
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
        Properties properties = load();
        properties.setProperty(DOWNLOAD_SOURCE_KEY, mode.storedName());
        properties.setProperty(CUSTOM_DOWNLOAD_API_KEY,
                customApi == null ? "" : customApi.trim());
        store(properties);
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
        Properties properties = load();
        properties.setProperty(LAST_LAUNCHED_INSTANCE_KEY, value);
        store(properties);
    }

    private Properties load() throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(settingsFile)) {
            try (InputStream input = Files.newInputStream(settingsFile)) {
                properties.load(input);
            }
        }
        return properties;
    }

    private void store(Properties properties) throws IOException {
        Files.createDirectories(settingsFile.getParent());
        try (OutputStream output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "CAML settings");
        }
    }
}
