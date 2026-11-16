package org.chs.camlauncher.backend.api;

import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceModUpdateTest {
    @TempDir
    Path root;

    @Test
    void replacesModAndKeepsDisabledState() throws Exception {
        byte[] content = "new-mod".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/new.jar", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            try (var output = exchange.getResponseBody()) {
                output.write(content);
            }
        });
        server.start();

        try {
            Path game = root.resolve("game");
            Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
            Files.createDirectories(mods);
            Files.writeString(mods.resolve("old.jar"), "old-mod");
            CAMLBackend backend = new CAMLBackend(new BackendConfig(root.resolve("data"), game));
            try (backend) {
                backend.setModDisabled(game, "1.21.4", "old.jar", true);
                ModFile latest = new ModFile(
                        ModSource.MODRINTH, "project", "version", "New", "2.0", "new.jar",
                        List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                                + "/new.jar")),
                        HashVerifier.sha1(content), null, content.length,
                        List.of("1.21.4"), List.of("fabric"), List.of(), "", 1);

                backend.updateInstanceMod(game, "1.21.4", "old.jar", latest, DownloadListener.NONE);

                assertFalse(Files.exists(mods.resolve("old.jar")));
                assertFalse(Files.exists(mods.resolve("old.jar.disabled")));
                assertTrue(Files.isRegularFile(mods.resolve("new.jar.disabled")));
                assertTrue(new SettingsService(root.resolve("data"))
                        .disabledMods(game, "1.21.4").contains("new.jar"));
            }
        } finally {
            server.stop(0);
        }
    }
}
