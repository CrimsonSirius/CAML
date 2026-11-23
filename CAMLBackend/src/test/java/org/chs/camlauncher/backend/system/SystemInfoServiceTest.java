package org.chs.camlauncher.backend.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SystemInfoServiceTest {

    @Test
    void readsPrettyNameFromOsRelease() {
        String name = SystemInfoService.parseOsRelease(List.of(
                "NAME=\"Ubuntu\"",
                "VERSION=\"24.04.1 LTS (Noble Numbat)\"",
                "PRETTY_NAME=\"Ubuntu 24.04.1 LTS\""
        ));

        assertEquals("Ubuntu 24.04.1 LTS", name);
    }

    @Test
    void combinesNameAndVersionWhenPrettyNameIsMissing() {
        String name = SystemInfoService.parseOsRelease(List.of(
                "NAME=Fedora Linux",
                "VERSION=41 (Workstation Edition)"
        ));

        assertEquals("Fedora Linux 41 (Workstation Edition)", name);
    }

    @Test
    void ignoresMalformedLines() {
        String name = SystemInfoService.parseOsRelease(List.of(
                "not-a-key-value",
                "NAME='Debian GNU/Linux'"
        ));

        assertEquals("Debian GNU/Linux", name);
    }

    @Test
    void exposesJavaRuntimeDetails() {
        SystemInfo info = new SystemInfoService().read();

        assertNotNull(info.versionKind());
        assertFalse(info.operatingSystem().isBlank());
        assertFalse(info.architecture().isBlank());
        assertFalse(info.javaVersion().isBlank());
    }
}
