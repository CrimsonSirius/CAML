package org.chs.camlauncher.backend.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackendConfigTest {
    @Test
    void usesHomeMinecraftOnLinux() {
        Path home = Path.of("/home/test");
        Path launcher = Path.of("/opt/caml");

        assertEquals(
                home.resolve(".minecraft"),
                BackendConfig.defaultGameDirectory(home, launcher, "Linux")
        );
    }

    @Test
    void usesLauncherMinecraftOnWindows() {
        Path home = Path.of("C:/Users/test");
        Path launcher = Path.of("D:/Games/CAML");

        assertEquals(
                launcher.resolve(".minecraft"),
                BackendConfig.defaultGameDirectory(home, launcher, "Windows 11")
        );
    }

    @Test
    void defaultsKeepLauncherDataSeparate() {
        BackendConfig config = BackendConfig.defaults();

        assertEquals(Path.of(System.getProperty("user.home"), ".caml"), config.dataDirectory());
        assertNotNull(config.gameDirectory());
        assertEquals(".minecraft", config.gameDirectory().getFileName().toString());
    }
}
