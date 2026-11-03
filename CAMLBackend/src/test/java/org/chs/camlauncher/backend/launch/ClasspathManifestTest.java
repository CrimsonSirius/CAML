package org.chs.camlauncher.backend.launch;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathManifestTest {
    @Test
    void writesAbsoluteClasspathUrls() throws Exception {
        Path first = Path.of("/tmp/a.jar");
        Path second = Path.of("/tmp/b with space.jar");
        Path jar = ClasspathManifest.create(List.of(first, second));
        try {
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                String classPath = jarFile.getManifest().getMainAttributes()
                        .getValue(Attributes.Name.CLASS_PATH);
                assertNotNull(classPath);
                assertTrue(classPath.contains(first.toUri().toASCIIString()));
                assertTrue(classPath.contains(second.toUri().toASCIIString()));
            }
        } finally {
            Files.deleteIfExists(jar);
        }
    }
}
