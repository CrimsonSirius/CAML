package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDetailViewQuiltFilterTest {

    @Test
    void hidesQuiltWhenNonQuiltVersionsExist() {
        List<ModFile> files = List.of(file("fabric"), file("quilt"));

        assertFalse(ModDetailView.shouldShowQuilt(null, files));
        assertFalse(ModDetailView.shouldShowQuilt("fabric", files));
    }

    @Test
    void showsQuiltWhenExplicitlySelected() {
        List<ModFile> files = List.of(file("fabric"), file("quilt"));

        assertTrue(ModDetailView.shouldShowQuilt("quilt", files));
    }

    @Test
    void showsQuiltWhenModHasOnlyQuiltVersions() {
        assertTrue(ModDetailView.shouldShowQuilt(null, List.of(file("quilt"))));
    }

    @Test
    void multiLoaderFileStillCountsAsNonQuiltVersion() {
        assertFalse(ModDetailView.shouldShowQuilt(null, List.of(file("fabric", "quilt"))));
    }

    @Test
    void unknownLoaderCountsAsNonQuiltVersion() {
        assertFalse(ModDetailView.shouldShowQuilt(null, List.of(file("quilt"), file())));
    }

    private static ModFile file(String... loaders) {
        return new ModFile(
                ModSource.MODRINTH,
                "project",
                "version",
                "Test Mod",
                "1.0.0",
                "test-mod.jar",
                List.of(URI.create("https://example.com/test-mod.jar")),
                "",
                null,
                1L,
                List.of("1.20.1"),
                Arrays.asList(loaders),
                List.of(),
                "2026-01-01T00:00:00Z",
                0L
        );
    }
}
