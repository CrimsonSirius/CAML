package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstanceModMetadataServiceTest {
    @TempDir
    Path root;

    @Test
    void readsFabricVersion() throws Exception {
        Path jar = root.resolve("fabric.jar");
        writeJar(jar, "fabric.mod.json", "{\"version\":\"1.2.3\"}");

        assertEquals("1.2.3", InstanceModMetadataService.version(jar));
    }

    @Test
    void readsForgeVersion() throws Exception {
        Path jar = root.resolve("forge.jar");
        writeJar(jar, "META-INF/mods.toml", "modLoader=\"javafml\"\nversion=\"4.5.6\"\n");

        assertEquals("4.5.6", InstanceModMetadataService.version(jar));
    }

    private static void writeJar(Path path, String name, String content) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new JarEntry(name));
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
