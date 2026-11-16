package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstanceModIconServiceTest {
    @TempDir
    Path root;

    @Test
    void loadsFabricIconFromJar() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
        Files.createDirectories(mods);
        byte[] icon = new byte[]{1, 2, 3, 4};
        writeJar(mods.resolve("demo.jar"), new Entry(
                "fabric.mod.json",
                "{\"icon\":\"assets/demo/custom-icon.png\"}".getBytes(StandardCharsets.UTF_8)
        ), new Entry("assets/demo/custom-icon.png", icon));

        assertArrayEquals(icon, InstanceModIconService.load(game, "1.21.4", "demo.jar"));
    }

    @Test
    void loadsForgeLogoFile() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
        Files.createDirectories(mods);
        byte[] icon = new byte[]{5, 6, 7};
        writeJar(mods.resolve("demo.jar"), new Entry(
                "META-INF/mods.toml",
                "logoFile=\"assets/demo/logo.png\"".getBytes(StandardCharsets.UTF_8)
        ), new Entry("assets/demo/logo.png", icon));

        assertArrayEquals(icon, InstanceModIconService.load(game, "1.21.4", "demo.jar"));
    }

    @Test
    void fallsBackToCommonIconName() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
        Files.createDirectories(mods);
        byte[] icon = new byte[]{8, 9};
        writeJar(mods.resolve("demo.jar"), new Entry("icon.png", icon));

        assertArrayEquals(icon, InstanceModIconService.load(game, "1.21.4", "demo.jar"));
    }

    @Test
    void loadsIconFromDisabledJar() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
        Files.createDirectories(mods);
        byte[] icon = new byte[]{10, 11};
        writeJar(mods.resolve("demo.jar.disabled"), new Entry("icon.png", icon));

        assertArrayEquals(icon, InstanceModIconService.load(game, "1.21.4", "demo.jar"));
    }

    @Test
    void returnsEmptyWhenJarHasNoIcon() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions").resolve("1.21.4").resolve("mods");
        Files.createDirectories(mods);
        writeJar(mods.resolve("demo.jar"), new Entry(
                "fabric.mod.json",
                "{}".getBytes(StandardCharsets.UTF_8)
        ));

        assertEquals(0, InstanceModIconService.load(game, "1.21.4", "demo.jar").length);
    }

    private static void writeJar(Path path, Entry... entries) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(path))) {
            for (Entry entry : entries) {
                output.putNextEntry(new JarEntry(entry.name()));
                output.write(entry.bytes());
                output.closeEntry();
            }
        }
    }

    private record Entry(String name, byte[] bytes) {
    }
}
