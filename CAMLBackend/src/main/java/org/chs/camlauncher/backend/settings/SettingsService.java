package org.chs.camlauncher.backend.settings;

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

/** 启动器设置持久化：游戏位置、最大内存和版本隔离。 */
public final class SettingsService {
    public static final int DEFAULT_MAX_MEMORY_MB = 4096;
    public static final int MIN_MAX_MEMORY_MB = 512;
    public static final VersionIsolationMode DEFAULT_VERSION_ISOLATION = VersionIsolationMode.FULL;

    private static final String MAX_MEMORY_KEY = "maxMemoryMb";
    private static final String GAME_DIRECTORY_KEY = "gameDirectory";
    private static final String VERSION_ISOLATION_KEY = "versionIsolation";

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
