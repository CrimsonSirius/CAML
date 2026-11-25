package org.chs.camlauncher.backend.modpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthModpackIndexTest {
    @Test
    void parsesDependenciesAndFiles() {
        ModrinthModpackIndex index = ModrinthModpackIndex.parse("""
                {
                  "name": "Test Pack",
                  "versionId": "1.2.3",
                  "summary": "A test modpack",
                  "dependencies": {
                    "minecraft": "1.20.1",
                    "fabric-loader": "0.15.0"
                  },
                  "files": [
                    {
                      "path": "mods/example.jar",
                      "hashes": {"sha1": "abc"},
                      "downloads": ["https://example.invalid/example.jar"],
                      "fileSize": 42,
                      "env": {"client": "required"}
                    },
                    {
                      "path": "mods/client-only.jar",
                      "hashes": {"sha1": "def"},
                      "downloads": ["https://example.invalid/client.jar"],
                      "fileSize": 7,
                      "env": {"client": "unsupported"}
                    }
                  ]
                }
                """);

        assertEquals("Test Pack", index.name());
        assertEquals("1.20.1", index.dependencies().get("minecraft"));
        assertEquals("0.15.0", index.dependencies().get("fabric-loader"));
        assertEquals(2, index.files().size());
        assertEquals("mods/example.jar", index.files().getFirst().path());
        assertEquals("abc", index.files().getFirst().sha1());
        assertEquals(42, index.files().getFirst().size());
        assertFalse(index.files().getFirst().clientUnsupported());
        assertTrue(index.files().get(1).clientUnsupported());
    }

    @Test
    void rejectsFileWithoutPath() {
        String json = """
                {"dependencies":{"minecraft":"1.20.1"},"files":[{"downloads":[]}]}
                """;

        assertThrows(IllegalArgumentException.class, () -> ModrinthModpackIndex.parse(json));
    }
}
