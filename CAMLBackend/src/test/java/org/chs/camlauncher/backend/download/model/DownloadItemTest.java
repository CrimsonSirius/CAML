package org.chs.camlauncher.backend.download.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadItemTest {
    @TempDir
    Path tempDirectory;

    @Test
    void recognizesExistingValidFile() throws Exception {
        Path file = tempDirectory.resolve("hello.txt");
        Files.writeString(file, "hello");

        DownloadItem item = new DownloadItem(
                "hello",
                List.of(URI.create("https://example.com/hello.txt")),
                file,
                "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d",
                5
        );

        assertTrue(item.existsAndValid());
    }

    @Test
    void rejectsEmptyExistingFileWithoutChecksum() throws Exception {
        Path file = tempDirectory.resolve("empty.jar");
        Files.createFile(file);

        DownloadItem item = new DownloadItem(
                "empty",
                List.of(URI.create("https://example.com/empty.jar")),
                file,
                null,
                -1
        );

        assertFalse(item.existsAndValid());
    }

    @Test
    void reusesNonEmptyFileWithoutChecksum() throws Exception {
        Path file = tempDirectory.resolve("downloaded.jar");
        Files.writeString(file, "content");

        DownloadItem item = new DownloadItem(
                "downloaded",
                List.of(URI.create("https://example.com/downloaded.jar")),
                file,
                null,
                -1
        );

        assertFalse(item.hasSha1());
        assertTrue(item.existsAndValid());
    }

    @Test
    void rejectsExistingFileWithMismatchedSizeWithoutChecksum() throws Exception {
        Path file = tempDirectory.resolve("incomplete.jar");
        Files.writeString(file, "abc");

        DownloadItem item = new DownloadItem(
                "incomplete",
                List.of(URI.create("https://example.com/incomplete.jar")),
                file,
                null,
                5
        );

        assertFalse(item.existsAndValid());
    }
}
