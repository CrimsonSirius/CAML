package org.chs.camlauncher.backend.modpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurseForgeModpackManifestTest {
    @Test
    void parsesLoaderFilesAndOverrides() {
        CurseForgeModpackManifest manifest = CurseForgeModpackManifest.parse("""
                {
                  "manifestType": "minecraftModpack",
                  "name": "Test Pack",
                  "version": "1.2.3",
                  "minecraft": {
                    "version": "1.20.1",
                    "modLoaders": [
                      {"id": "forge-47.2.0", "primary": false},
                      {"id": "forge-47.4.0", "primary": true}
                    ]
                  },
                  "files": [
                    {"projectID": 123, "fileID": 456, "required": true},
                    {"projectID": 789, "fileID": 987, "required": false}
                  ],
                  "overrides": "pack-overrides"
                }
                """);

        assertEquals("Test Pack", manifest.name());
        assertEquals("1.2.3", manifest.version());
        assertEquals("1.20.1", manifest.dependencies().get("minecraft"));
        assertEquals("47.4.0", manifest.dependencies().get("forge"));
        assertEquals("pack-overrides", manifest.overrides());
        assertEquals(2, manifest.files().size());
        assertEquals(123, manifest.files().getFirst().projectId());
        assertTrue(manifest.files().getFirst().required());
        assertFalse(manifest.files().get(1).required());
    }

    @Test
    void mapsFabricLoaderId() {
        CurseForgeModpackManifest manifest = CurseForgeModpackManifest.parse("""
                {
                  "minecraft": {
                    "version": "1.20.1",
                    "modLoaders": [{"id": "fabric-0.15.11", "primary": true}]
                  }
                }
                """);

        assertEquals("0.15.11", manifest.dependencies().get("fabric-loader"));
        assertEquals("overrides", manifest.overrides());
    }

    @Test
    void rejectsUnsupportedManifestTypeAndUnsafeOverrides() {
        assertThrows(IllegalArgumentException.class, () -> CurseForgeModpackManifest.parse("""
                {"manifestType": "other", "minecraft": {"version": "1.20.1"}}
                """));
        assertThrows(IllegalArgumentException.class, () -> CurseForgeModpackManifest.parse("""
                {"minecraft": {"version": "1.20.1"}, "overrides": "../outside"}
                """));
    }
}
