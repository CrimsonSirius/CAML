package org.chs.camlauncher.backend.runtime;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaRuntimeParserTest {
    @Test
    void parsesLegacyAndModernJavaVersions() {
        assertEquals(8, JavaRuntimeParser.parseMajorVersion("1.8.0_402"));
        assertEquals(17, JavaRuntimeParser.parseMajorVersion("17.0.10"));
        assertEquals(25, JavaRuntimeParser.parseMajorVersion("25.0.4.1.1"));
        assertEquals(-1, JavaRuntimeParser.parseMajorVersion("unknown"));
    }

    @Test
    void normalizesArchitecturesAndBits() {
        assertEquals("x86_64", JavaRuntimeParser.normalizeArchitecture("amd64"));
        assertEquals("arm64", JavaRuntimeParser.normalizeArchitecture("aarch64"));
        assertEquals("x86", JavaRuntimeParser.normalizeArchitecture("i686"));
        assertEquals(64, JavaRuntimeParser.parseBits("64", "x86_64"));
        assertEquals(32, JavaRuntimeParser.parseBits(null, "x86"));
    }

    @Test
    void parsesPropertyOutput() {
        Map<String, String> properties = JavaRuntimeParser.parseProperties("""
                Property settings:
                    java.home = /usr/lib/jvm/zulu-25
                    java.version = 25.0.4.1
                    java.vendor = Azul Systems, Inc.
                    os.arch = amd64
                    sun.arch.data.model = 64
                """);

        assertEquals("/usr/lib/jvm/zulu-25", properties.get("java.home"));
        assertEquals("25.0.4.1", properties.get("java.version"));
        assertEquals("64", properties.get("sun.arch.data.model"));
    }
}
