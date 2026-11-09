package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceNameRulesTest {
    @TempDir
    Path gameDirectory;

    @Test
    void acceptsNewValidName() {
        InstanceNameCheck check = InstanceNameRules.check(gameDirectory, " 新实例 ");

        assertTrue(check.available());
    }

    @Test
    void rejectsExistingInstanceDirectory() throws Exception {
        Files.createDirectories(gameDirectory.resolve("versions/新实例"));

        InstanceNameCheck check = InstanceNameRules.check(gameDirectory, "新实例");

        assertFalse(check.available());
        assertTrue(check.message().contains("已存在"));
    }

    @Test
    void rejectsInvalidNamesWithSharedMessage() {
        assertFalse(InstanceNameRules.check(gameDirectory, "").available());
        assertFalse(InstanceNameRules.check(gameDirectory, "..").available());
        assertFalse(InstanceNameRules.check(gameDirectory, "a/b").available());
        assertTrue(InstanceNameRules.check(gameDirectory, "a ").available());
    }

    @Test
    void ensureNewInstanceThrowsForConflict() throws Exception {
        Files.createDirectories(gameDirectory.resolve("versions/test"));

        assertThrows(IOException.class,
                () -> InstanceNameRules.ensureNewInstance(gameDirectory, "test"));
    }
}
