package org.chs.camlauncher.backend.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatestLogServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void deletesLogOnNormalClose() throws Exception {
        Path file = tempDirectory.resolve("latest.log");
        LatestLogService service = new LatestLogService(file);
        service.start();
        service.append("normal line");
        service.close();

        assertFalse(Files.exists(file));
    }

    @Test
    void keepsLogOnFailure() throws Exception {
        Path file = tempDirectory.resolve("latest.log");
        LatestLogService service = new LatestLogService(file);
        service.start();
        service.append("[FATAL] launcher failed");
        service.close();

        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("[FATAL]"));
    }
}
