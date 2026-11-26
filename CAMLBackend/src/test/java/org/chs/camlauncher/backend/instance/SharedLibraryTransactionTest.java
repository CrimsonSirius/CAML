package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedLibraryTransactionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void replacesSameSizeDifferentContentAndRestoresOnRollback() throws Exception {
        Path libraries = temporaryDirectory.resolve("libraries");
        Path staged = temporaryDirectory.resolve("staged");
        Path target = libraries.resolve("org/example/demo/1.0/demo-1.0.jar");
        Path source = staged.resolve("org/example/demo/1.0/demo-1.0.jar");
        Files.createDirectories(target.getParent());
        Files.createDirectories(source.getParent());
        Files.writeString(target, "old-content", StandardCharsets.UTF_8);
        Files.writeString(source, "new-content", StandardCharsets.UTF_8);
        assertEquals(Files.size(source), Files.size(target));

        try (SharedLibraryTransaction transaction = SharedLibraryTransaction.begin(libraries)) {
            transaction.merge(staged);
            assertEquals("new-content", Files.readString(target));
        }

        assertEquals("old-content", Files.readString(target));
    }

    @Test
    void commitKeepsReplacement() throws Exception {
        Path libraries = temporaryDirectory.resolve("libraries");
        Path staged = temporaryDirectory.resolve("staged");
        Path replaced = libraries.resolve("org/example/demo/1.0/demo-1.0.jar");
        Files.createDirectories(replaced.getParent());
        Files.createDirectories(staged.resolve("org/example/demo/1.0"));
        Files.writeString(replaced, "old", StandardCharsets.UTF_8);
        Files.writeString(staged.resolve("org/example/demo/1.0/demo-1.0.jar"),
                "new", StandardCharsets.UTF_8);

        try (SharedLibraryTransaction transaction = SharedLibraryTransaction.begin(libraries)) {
            transaction.merge(staged);
            transaction.commit();
        }

        assertEquals("new", Files.readString(replaced));
    }

    @Test
    void rollbackDeletesCreatedFile() throws Exception {
        Path libraries = temporaryDirectory.resolve("libraries");
        Path staged = temporaryDirectory.resolve("staged");
        Path created = libraries.resolve("org/example/new/1.0/new-1.0.jar");
        Files.createDirectories(staged.resolve("org/example/new/1.0"));
        Files.writeString(staged.resolve("org/example/new/1.0/new-1.0.jar"),
                "created", StandardCharsets.UTF_8);

        try (SharedLibraryTransaction transaction = SharedLibraryTransaction.begin(libraries)) {
            transaction.merge(staged);
            assertTrue(Files.isRegularFile(created));
        }

        assertFalse(Files.exists(created));
    }

    @Test
    void skipsIdenticalContent() throws Exception {
        Path libraries = temporaryDirectory.resolve("libraries");
        Path staged = temporaryDirectory.resolve("staged");
        Path target = libraries.resolve("org/example/demo/1.0/demo-1.0.jar");
        Path source = staged.resolve("org/example/demo/1.0/demo-1.0.jar");
        Files.createDirectories(target.getParent());
        Files.createDirectories(source.getParent());
        Files.writeString(target, "same", StandardCharsets.UTF_8);
        Files.writeString(source, "same", StandardCharsets.UTF_8);
        long modified = Files.getLastModifiedTime(target).toMillis();

        try (SharedLibraryTransaction transaction = SharedLibraryTransaction.begin(libraries)) {
            transaction.merge(staged);
            transaction.commit();
        }

        assertEquals(modified, Files.getLastModifiedTime(target).toMillis());
        assertEquals("same", Files.readString(target));
    }
}
