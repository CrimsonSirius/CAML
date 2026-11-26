package org.chs.camlauncher.backend.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArchiveExtractionGuardTest {
    @Test
    void rejectsTooManyEntries() throws Exception {
        ArchiveExtractionGuard guard = new ArchiveExtractionGuard(1, 1024, 1024);
        guard.beginEntry();

        assertThrows(IOException.class, guard::beginEntry);
    }

    @Test
    void rejectsSingleEntryOverLimit() {
        ArchiveExtractionGuard guard = new ArchiveExtractionGuard(10, 4, 1024);

        assertThrows(IOException.class, () -> guard.copy(
                stream("12345"), new ByteArrayOutputStream()));
    }

    @Test
    void rejectsTotalBytesOverLimitAcrossEntries() throws Exception {
        ArchiveExtractionGuard guard = new ArchiveExtractionGuard(10, 4, 6);
        guard.copy(stream("1234"), new ByteArrayOutputStream());

        assertThrows(IOException.class, () -> guard.copy(
                stream("567"), new ByteArrayOutputStream()));
    }

    @Test
    void readAllIsAlsoBounded() {
        ArchiveExtractionGuard guard = new ArchiveExtractionGuard(10, 4, 1024);

        assertThrows(IOException.class, () -> guard.readAll(stream("12345")));
    }

    @Test
    void acceptsContentWithinLimits() throws Exception {
        ArchiveExtractionGuard guard = new ArchiveExtractionGuard(2, 5, 8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        guard.beginEntry();
        guard.copy(stream("1234"), output);
        guard.beginEntry();
        guard.copy(stream("5678"), output);

        assertEquals("12345678", output.toString(StandardCharsets.UTF_8));
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
