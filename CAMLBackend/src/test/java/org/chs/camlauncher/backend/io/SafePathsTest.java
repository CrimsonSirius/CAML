package org.chs.camlauncher.backend.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafePathsTest {
    @TempDir
    Path root;

    @Test
    void resolvesNestedPathInsideRoot() {
        assertEquals(
                root.toAbsolutePath().normalize().resolve("a/b.jar"),
                SafePaths.resolveUnder(root, "a/b.jar", "test")
        );
    }

    @Test
    void rejectsTraversalAndAbsolutePaths() {
        assertThrows(IllegalArgumentException.class,
                () -> SafePaths.resolveUnder(root, "../outside", "test"));
        assertThrows(IllegalArgumentException.class,
                () -> SafePaths.resolveUnder(root, root.resolve("outside").toString(), "test"));
        assertThrows(IllegalArgumentException.class,
                () -> SafePaths.resolveUnder(root, "../../outside", "test"));
    }

    @Test
    void rejectsSeparatorsForSingleNames() {
        assertThrows(IllegalArgumentException.class,
                () -> SafePaths.resolveChild(root, "a/b", "test"));
        assertThrows(IllegalArgumentException.class,
                () -> SafePaths.resolveChild(root, "a\\b", "test"));
        assertThrows(IllegalArgumentException.class,
                () -> SafePaths.resolveChild(root, "..", "test"));
    }
}
