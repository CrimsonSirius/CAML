package org.chs.camlauncher.backend.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatestLogServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void keepsLogOnNormalCloseWithoutArchive() throws Exception {
        Path file = tempDirectory.resolve("latest.log");
        LatestLogService service = new LatestLogService(file);
        service.start();
        service.append("normal line");
        service.close();

        assertTrue(Files.exists(file));
        assertEquals("normal line" + System.lineSeparator(), Files.readString(file));
        assertEquals(List.of(), archives(file));
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
        List<Path> archives = archives(file);
        assertEquals(1, archives.size());
        try (var input = new GZIPInputStream(Files.newInputStream(archives.getFirst()))) {
            assertTrue(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("[FATAL]"));
        }
    }

    @Test
    void keepsLogWhenGameReportsError() throws Exception {
        Path file = tempDirectory.resolve("latest.log");
        LatestLogService service = new LatestLogService(file);
        service.start();
        service.append("[ERR] game stderr");
        service.close();

        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("[ERR]"));
        assertEquals(1, archives(file).size());
    }

    private static List<Path> archives(Path latest) throws Exception {
        try (var files = Files.list(latest.getParent())) {
            return files.filter(path -> path.getFileName().toString().endsWith(".log.gz")).toList();
        }
    }
}
