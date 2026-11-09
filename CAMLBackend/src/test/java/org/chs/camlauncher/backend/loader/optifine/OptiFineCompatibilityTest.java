package org.chs.camlauncher.backend.loader.optifine;

import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptiFineCompatibilityTest {
    @Test
    void rejectsForgeVersionsWithKnownOptiFineI6Conflict() {
        OptiFineVersion optiFine = optiFine("1.20.1", "HD_U", "I6");

        assertTrue(OptiFineCompatibility.isForgeCompatible(forge("47.4.26"), optiFine));
        assertFalse(OptiFineCompatibility.isForgeCompatible(forge("47.4.27"), optiFine));
        assertFalse(OptiFineCompatibility.isForgeCompatible(forge("47.4.29"), optiFine));
    }

    @Test
    void leavesOtherOptiFineVersionsUnrestricted() {
        OptiFineVersion optiFine = optiFine("1.21.1", "HD_U", "J1");

        assertTrue(OptiFineCompatibility.isForgeCompatible(forge("52.0.16"), optiFine));
    }

    private static ForgeVersion forge(String version) {
        return new ForgeVersion("1.20.1", version, URI.create("https://example.invalid/forge.jar"));
    }

    private static OptiFineVersion optiFine(String gameVersion, String type, String patch) {
        return new OptiFineVersion(gameVersion, type, patch,
                "OptiFine_" + gameVersion + "_" + type + "_" + patch + ".jar",
                null,
                URI.create("https://example.invalid/optifine.jar"));
    }
}
