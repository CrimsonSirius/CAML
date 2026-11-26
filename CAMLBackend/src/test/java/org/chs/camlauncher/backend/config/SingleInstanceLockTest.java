package org.chs.camlauncher.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleInstanceLockTest {
    @TempDir
    Path dataDirectory;

    @Test
    void rejectsSecondInstanceUntilFirstCloses() throws Exception {
        try (SingleInstanceLock first = SingleInstanceLock.tryAcquire(dataDirectory)
                .orElseThrow()) {
            Optional<SingleInstanceLock> second = SingleInstanceLock.tryAcquire(dataDirectory);
            assertTrue(second.isEmpty());
        }

        try (SingleInstanceLock third = SingleInstanceLock.tryAcquire(dataDirectory)
                .orElseThrow()) {
            assertFalse(third == null);
        }
    }
}
