package org.chs.camlauncher.backend.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
}
