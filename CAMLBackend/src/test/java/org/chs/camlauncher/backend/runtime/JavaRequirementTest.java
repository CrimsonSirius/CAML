package org.chs.camlauncher.backend.runtime;

import org.chs.camlauncher.backend.metadata.model.JavaVersion;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaRequirementTest {
    @Test
    void derivesRequirementFromVersionInfo() {
        VersionInfo version = new VersionInfo(
                "1.20.1", "release", null, "main", "5", null, null, null,
                null, new JavaVersion("java-runtime-gamma", 17), List.of(), null,
                null, null, null, null, null
        );

        JavaRequirement requirement = JavaRequirement.from(version);

        assertTrue(requirement.isSatisfiedBy(java(17, "x86_64", 64)));
        assertTrue(requirement.isSatisfiedBy(java(21, "x86_64", 64)));
        assertFalse(requirement.isSatisfiedBy(java(8, "x86_64", 64)));
        assertFalse(requirement.isSatisfiedBy(java(17, "arm64", 64)));
    }

    private static JavaRuntime java(int major, String arch, int bits) {
        return new JavaRuntime(Path.of("/tmp/java"), major + ".0", major, "test", arch, bits, true);
    }
}
