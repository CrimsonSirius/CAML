package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        InstanceDetails details = InstanceInspector.inspect(
                game, "1.21.4-test", VersionIsolationMode.FULL, java.util.Set.of());

        assertEquals(version, details.gameDataDirectory());
        assertEquals(version.resolve("mods"), details.modsDirectory());
        assertEquals(2, details.modCount());
        assertEquals("iris.jar", details.mods().get(0).fileName());
        assertEquals("sodium.jar", details.mods().get(1).fileName());
        assertEquals("", details.mods().get(0).version());
        assertFalse(details.mods().stream()
                .filter(mod -> mod.fileName().equals("iris.jar"))
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

        InstanceDetails details = InstanceInspector.inspect(
                game, "1.21.4", VersionIsolationMode.NONE, java.util.Set.of());

        assertEquals(game, details.gameDataDirectory());
        assertEquals(1, details.modCount());
    }

    @Test
    void readsResourcePacksAndPackFormat() throws Exception {
        Path game = root.resolve("game");
        Path version = game.resolve("versions").resolve("1.21.4-test");
        Path resourcePacks = version.resolve("resourcepacks");
        Files.createDirectories(resourcePacks);
        writeResourcePack(resourcePacks.resolve("Vanilla.zip"), 34);
        writeResourcePack(resourcePacks.resolve("Old.zip.disabled"), 15);
        Path folderPack = resourcePacks.resolve("FolderPack");
        Files.createDirectories(folderPack);
        Files.writeString(folderPack.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":22}}", StandardCharsets.UTF_8);
        Files.writeString(resourcePacks.resolve("readme.txt"), "ignored");

        InstanceDetails details = InstanceInspector.inspect(
                game, "1.21.4-test", VersionIsolationMode.FULL, java.util.Set.of());

        assertEquals(resourcePacks, details.resourcePacksDirectory());
        assertEquals(3, details.resourcePackCount());
        assertEquals("FolderPack", details.resourcePacks().get(0).fileName());
        assertEquals("22", details.resourcePacks().get(0).version());
        assertEquals("Old.zip", details.resourcePacks().get(1).fileName());
        assertEquals("15", details.resourcePacks().get(1).version());
        assertFalse(details.resourcePacks().get(1).enabled());
        assertEquals("Vanilla.zip", details.resourcePacks().get(2).fileName());
        assertEquals("34", details.resourcePacks().get(2).version());
        assertTrue(details.resourcePacks().get(2).enabled());
    }

    @Test
    void rejectsInvalidInstanceId() {
        assertThrows(InstanceNameException.class,
                () -> InstanceInspector.inspect(
                        root.resolve("game"), "../escape", VersionIsolationMode.FULL,
                        java.util.Set.of()));
    }

    private static void writeResourcePack(Path path, int packFormat) throws Exception {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new ZipEntry("pack.mcmeta"));
            output.write(("{\"pack\":{\"pack_format\":" + packFormat + "}}")
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
