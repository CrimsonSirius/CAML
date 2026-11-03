package org.chs.camlauncher.backend.config;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Locale;

public record BackendConfig(Path dataDirectory, Path gameDirectory) {
    public BackendConfig {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
    }

    public static BackendConfig defaults() {
        Path userHome = Path.of(System.getProperty("user.home"));
        String osName = System.getProperty("os.name", "");
        Path gameDirectory = isWindows(osName)
                ? launcherDirectory().resolve(".minecraft")
                : userHome.resolve(".minecraft");
        return new BackendConfig(userHome.resolve(".caml"), gameDirectory);
    }

    static Path defaultGameDirectory(Path userHome, Path launcherDirectory, String osName) {
        Objects.requireNonNull(userHome, "userHome");
        Objects.requireNonNull(launcherDirectory, "launcherDirectory");
        return isWindows(osName)
                ? launcherDirectory.resolve(".minecraft")
                : userHome.resolve(".minecraft");
    }

    static Path launcherDirectory() {
        try {
            var codeSource = BackendConfig.class.getProtectionDomain().getCodeSource();
            Path location = Path.of(codeSource.getLocation().toURI());
            Path directory = Files.isDirectory(location)
                    ? Path.of(System.getProperty("user.dir"))
                    : location.getParent();
            if (directory != null) {
                return directory.toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {
        }
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private static boolean isWindows(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("win");
    }
}
