package org.chs.camlauncher.backend.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLocatorTest {
    @Test
    void inspectsCurrentJavaRuntime() {
        Path home = Path.of(System.getProperty("java.home"));
        Path executable = home.resolve("bin").resolve(
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        assertTrue(Files.isRegularFile(executable));

        JavaRuntime runtime = new JavaLocator().inspect(executable).orElseThrow();

        assertEquals(Runtime.version().feature(), runtime.majorVersion());
        assertTrue(runtime.bits() == 32 || runtime.bits() == 64);
    }
}
