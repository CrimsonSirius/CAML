package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.api.InstanceDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceInspectorTest {
    @TempDir
    Path root;

    @Test
    void readsIsolatedInstanceMods() throws Exception {
        Path game = root.resolve("game");
        Path version = game.resolve("versions").resolve("1.21.4-test");
        Files.createDirectories(version.resolve("mods"));
        Files.writeString(version.resolve("mods").resolve("sodium.jar"), "12345");
        Files.writeString(version.resolve("mods").resolve("iris.jar.disabled"), "1234567");
        Files.writeString(version.resolve("mods").resolve("readme.txt"), "ignored");

        InstanceDetails details = InstanceInspector.inspect(game, "1.21.4-test");

        assertEquals(version, details.gameDataDirectory());
        assertEquals(version.resolve("mods"), details.modsDirectory());
        assertEquals(2, details.modCount());
        assertEquals("iris.jar.disabled", details.mods().get(0).fileName());
        assertEquals("sodium.jar", details.mods().get(1).fileName());
        assertFalse(details.mods().stream()
                .filter(mod -> mod.fileName().equals("iris.jar.disabled"))
                .findFirst().orElseThrow().enabled());
        assertTrue(details.mods().stream()
                .filter(mod -> mod.fileName().equals("sodium.jar"))
                .findFirst().orElseThrow().enabled());
    }

    @Test
    void readsSharedInstanceModsFromGameRoot() throws Exception {
        Path game = root.resolve("game");
        Path version = game.resolve("versions").resolve("1.21.4");
        Files.createDirectories(version);
        Files.writeString(version.resolve("1.21.4.json"), "{}");
        Files.createDirectories(game.resolve("mods"));
        Files.writeString(game.resolve("mods").resolve("test.jar"), "1");

        InstanceDetails details = InstanceInspector.inspect(game, "1.21.4");

        assertEquals(game, details.gameDataDirectory());
        assertEquals(1, details.modCount());
    }

    @Test
    void rejectsInvalidInstanceId() {
        assertThrows(InstanceNameException.class,
                () -> InstanceInspector.inspect(root.resolve("game"), "../escape"));
    }
}
