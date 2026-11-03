package org.chs.camlauncher.backend.launch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentFileTest {
    @TempDir
    Path tempDirectory;

    @Test
    void quotesArgumentsWithWhitespaceAndComments() throws Exception {
        Path file = ArgumentFile.write(List.of(
                "-cp",
                "/tmp/a b.jar",
                "#not-a-comment",
                "plain"
        ));
        try {
            String content = Files.readString(file);
            assertTrue(content.contains("/tmp/a b.jar"));
            assertTrue(content.contains("\"/tmp/a b.jar\""));
            assertTrue(content.contains("\"#not-a-comment\""));
            assertTrue(content.contains("plain"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
