package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.config.BackendConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BackendSingleInstanceTest {
    @TempDir
    Path root;

    @Test
    void rejectsSecondBackendForSameDataDirectory() {
        BackendConfig config = new BackendConfig(root.resolve("data"), root.resolve("game"));
        try (CAMLBackend first = new CAMLBackend(config)) {
            first.start();
            try (CAMLBackend second = new CAMLBackend(config)) {
                assertThrows(UncheckedIOException.class, second::start);
            }
        }
    }
}
