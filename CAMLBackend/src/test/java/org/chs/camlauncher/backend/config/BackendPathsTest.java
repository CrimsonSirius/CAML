package org.chs.camlauncher.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendPathsTest {

    @Test
    void createsManagedJavaRepositoryBesideThemes(@TempDir Path root) throws Exception {
        BackendConfig config = new BackendConfig(root.resolve("caml"), root.resolve("minecraft"));

        BackendPaths.ensure(config);

        assertTrue(Files.isDirectory(config.dataDirectory().resolve("themes")));
        assertTrue(Files.isDirectory(config.dataDirectory().resolve("java")));
    }
}
