package org.chs.camlauncher.backend.config;

import java.nio.file.Path;
import java.util.Objects;

public record BackendConfig(Path dataDirectory, Path gameDirectory) {
    public BackendConfig {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
    }

    public static BackendConfig defaults() {
        Path dataDirectory = Path.of(System.getProperty("user.home"), ".caml");
        return new BackendConfig(dataDirectory, dataDirectory.resolve("minecraft"));
    }
}
