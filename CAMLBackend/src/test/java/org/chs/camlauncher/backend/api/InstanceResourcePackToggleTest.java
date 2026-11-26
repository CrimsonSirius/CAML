package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.config.BackendConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceResourcePackToggleTest {
    @TempDir
    Path root;

    @Test
    void togglesDisabledSuffix() throws Exception {
        Path game = root.resolve("game");
        Path resourcePacks = game.resolve("versions").resolve("1.21.4").resolve("resourcepacks");
        Files.createDirectories(resourcePacks);
        Path enabled = resourcePacks.resolve("demo.zip");
        Files.writeString(enabled, "zip");

        CAMLBackend backend = new CAMLBackend(new BackendConfig(root.resolve("data"), game));
        try (backend) {
            backend.instances().setResourcePackDisabled(game, "1.21.4", "demo.zip", true);

            Path disabled = resourcePacks.resolve("demo.zip.disabled");
            assertFalse(Files.exists(enabled));
            assertTrue(Files.isRegularFile(disabled));
            assertFalse(backend.instances().inspectInstance(game, "1.21.4")
                    .resourcePacks().get(0).enabled());

            backend.instances().setResourcePackDisabled(game, "1.21.4", "demo.zip", false);
            assertTrue(Files.isRegularFile(enabled));
            assertFalse(Files.exists(disabled));
            assertTrue(backend.instances().inspectInstance(game, "1.21.4")
                    .resourcePacks().get(0).enabled());
        }
    }
}
