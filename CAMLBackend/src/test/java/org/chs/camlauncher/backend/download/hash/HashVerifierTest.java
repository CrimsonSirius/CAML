package org.chs.camlauncher.backend.download.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashVerifierTest {
    @TempDir
    Path tempDirectory;

    @Test
    void calculatesAndMatchesSha1() throws Exception {
        Path file = tempDirectory.resolve("hello.txt");
        Files.writeString(file, "hello");

        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", HashVerifier.sha1(file));
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d",
                HashVerifier.sha1("hello".getBytes(StandardCharsets.UTF_8)));
        assertTrue(HashVerifier.matches(file, "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"));
        assertFalse(HashVerifier.matches(file, "0000000000000000000000000000000000000000"));
        assertFalse(HashVerifier.matches(file, null));
        assertFalse(HashVerifier.matches(file, ""));
        assertFalse(HashVerifier.matches(file, "   "));
    }
}
