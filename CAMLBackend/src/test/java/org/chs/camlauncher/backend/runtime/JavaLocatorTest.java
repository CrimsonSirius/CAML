package org.chs.camlauncher.backend.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLocatorTest {
    @TempDir
    Path tempDirectory;

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

    @Test
    void mergesManualAndScannedJavaWithoutDuplicates() {
        Path home = Path.of(System.getProperty("java.home"));
        Path executable = home.resolve("bin").resolve(
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");

        List<JavaCandidate> candidates = new JavaLocator().discoverCandidates(
                List.of(executable, executable), executable);
        List<JavaCandidate> manualCandidates = candidates.stream()
                .filter(JavaCandidate::manual)
                .toList();

        assertEquals(1, manualCandidates.size());
        assertTrue(manualCandidates.getFirst().available());
    }

    @Test
    void scansNestedManagedJavaRepository() throws Exception {
        Path repository = tempDirectory.resolve("java");
        Path executable = repository.resolve("linux-x86_64/zulu-8-jdk/bin/java");
        Files.createDirectories(executable.getParent());
        Files.createFile(executable);

        List<Path> executables = JavaLocator.managedJavaExecutables(repository);

        assertEquals(List.of(executable), executables);
    }
}
