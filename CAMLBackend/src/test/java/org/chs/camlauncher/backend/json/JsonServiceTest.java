package org.chs.camlauncher.backend.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void writesAndReadsJson() throws IOException {
        JsonService json = new JsonService();
        Path file = tempDirectory.resolve("config.json");
        Sample sample = new Sample("CAML", 1);

        json.write(file, sample);

        assertEquals(sample, json.read(file, Sample.class));
    }

    record Sample(String name, int value) {
    }
}
