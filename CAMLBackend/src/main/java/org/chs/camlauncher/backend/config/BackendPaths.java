package org.chs.camlauncher.backend.config;

import java.io.IOException;
import java.nio.file.Files;

public final class BackendPaths {
    private BackendPaths() {
    }

    public static void ensure(BackendConfig config) throws IOException {
        Files.createDirectories(config.dataDirectory());
        Files.createDirectories(config.dataDirectory().resolve("logs"));
        Files.createDirectories(config.dataDirectory().resolve("cache"));
        Files.createDirectories(config.gameDirectory());
        Files.createDirectories(config.gameDirectory().resolve("versions"));
    }
}
