package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("download.setup.nameExists", check.error().code());
        assertEquals("新实例", check.error().args().getFirst());
    }

    @Test
    void rejectsInvalidNamesWithSharedMessage() {
        assertEquals("download.setup.nameEmpty",
                InstanceNameRules.check(gameDirectory, "").error().code());
        assertEquals("download.setup.nameDotSegments",
                InstanceNameRules.check(gameDirectory, "..").error().code());
        assertEquals("download.setup.nameIllegalCharacter",
                InstanceNameRules.check(gameDirectory, "a/b").error().code());
        assertTrue(InstanceNameRules.check(gameDirectory, "a ").available());
    }

    @Test
    void ensureNewInstanceThrowsForConflict() throws Exception {
        Files.createDirectories(gameDirectory.resolve("versions/test"));

        assertThrows(IOException.class,
                () -> InstanceNameRules.ensureNewInstance(gameDirectory, "test"));
    }
}
