package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceModToggleTest {
    @TempDir
    Path root;

    @Test
    void togglesJarSuffixAndCamlConfig() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
        Files.createDirectories(mods);
        Path enabled = mods.resolve("demo.jar");
        Files.writeString(enabled, "jar");

        CAMLBackend backend = new CAMLBackend(new BackendConfig(root.resolve("data"), game));
        try (backend) {
            backend.instances().setModDisabled(game, "1.21.4", "demo.jar", true);

            Path disabled = mods.resolve("demo.jar.disabled");
            assertFalse(Files.exists(enabled));
            assertTrue(Files.isRegularFile(disabled));
            assertFalse(backend.instances().inspectInstance(game, "1.21.4")
                    .mods().get(0).enabled());
            assertTrue(new SettingsService(root.resolve("data"))
                    .disabledMods(game, "1.21.4").contains("demo.jar"));

            backend.instances().setModDisabled(game, "1.21.4", "demo.jar", false);
            assertTrue(Files.isRegularFile(enabled));
            assertFalse(Files.exists(disabled));
            assertTrue(backend.instances().inspectInstance(game, "1.21.4")
                    .mods().get(0).enabled());
        }
    }
}
