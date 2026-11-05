package org.chs.camlauncher.backend.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SystemMemoryServiceTest {
    @Test
    void usesMemAvailableInsteadOfMemFree() {
        SystemMemoryInfo info = SystemMemoryService.parseLinuxMeminfo(List.of(
                "MemTotal:       32856644 kB",
                "MemFree:          400000 kB",
                "MemAvailable:   18000000 kB"
        ));

        assertEquals(32086, info.totalMb());
        assertEquals(17578, info.availableMb());
    }

    @Test
    void fallsBackToMemFreeWhenAvailableIsMissing() {
        SystemMemoryInfo info = SystemMemoryService.parseLinuxMeminfo(List.of(
                "MemTotal:       8388608 kB",
                "MemFree:        2097152 kB"
        ));

        assertEquals(8192, info.totalMb());
        assertEquals(2048, info.availableMb());
    }

    @Test
    void rejectsMissingTotal() {
        assertNull(SystemMemoryService.parseLinuxMeminfo(List.of("MemAvailable: 1024 kB")));
    }
}
