package org.chs.camlauncher.backend.metadata;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionManifestTest {

    @Test
    void returnsOnlyReleasesSortedNewestFirst() {
        VersionManifest manifest = new VersionManifest(null, List.of(
                version("old", "release", "2020-01-01T00:00:00+00:00"),
                version("snapshot", "snapshot", "2025-01-01T00:00:00+00:00"),
                version("new", "release", "2024-01-01T00:00:00+00:00"),
                version("unknown", null, "2030-01-01T00:00:00+00:00")
        ));

        assertEquals(
                List.of("new", "old"),
                manifest.releasesNewestFirst().stream().map(VersionManifest.Version::id).toList()
        );
    }

    private static VersionManifest.Version version(String id, String type, String releaseTime) {
        return new VersionManifest.Version(
                id, type, "https://example.invalid/" + id + ".json",
                releaseTime, releaseTime, null, null
        );
    }
}
