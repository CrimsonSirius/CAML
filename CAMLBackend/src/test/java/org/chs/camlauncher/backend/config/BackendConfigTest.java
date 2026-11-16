package org.chs.camlauncher.backend.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackendConfigTest {
    @Test
    void usesHomeMinecraft() {
        Path home = Path.of("/home/test");

        // Windows 与 Linux 一致：游戏目录固定在用户主目录，避免写入 Program Files。
        assertEquals(home.resolve(".minecraft"), BackendConfig.defaultGameDirectory(home));
    }

    @Test
    void defaultsKeepLauncherDataSeparate() {
        BackendConfig config = BackendConfig.defaults();

        assertEquals(Path.of(System.getProperty("user.home"), ".caml"), config.dataDirectory());
        assertNotNull(config.gameDirectory());
        assertEquals(".minecraft", config.gameDirectory().getFileName().toString());
    }
}