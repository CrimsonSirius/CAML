package org.chs.camlauncher.backend.metadata;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManifestResolverTest {
    private final MetadataService metadata = new MetadataService();
    private final ManifestResolver resolver = new ManifestResolver();

    @Test
    void mergesParentAndChildManifest() {
        VersionInfo parent = metadata.parseVersionInfo("""
                {
                  "id": "1.20.1",
                  "type": "release",
                  "mainClass": "parent.Main",
                  "assets": "5",
                  "assetIndex": {
                    "id": "5",
                    "sha1": "asset-index",
                    "size": 10,
                    "totalSize": 20,
                    "url": "https://example.com/assets.json"
                  },
                  "arguments": {
                    "game": ["--base-game"],
                    "jvm": ["-Xmx1G"]
                  },
                  "downloads": {
                    "client": {
                      "sha1": "client-sha1",
                      "size": 100,
                      "url": "https://example.com/client.jar"
                    }
                  },
                  "javaVersion": {
                    "component": "java-runtime-gamma",
                    "majorVersion": 17
                  },
                  "libraries": [
                    {"name": "base:library:1.0"}
                  ]
                }
                """);

        VersionInfo child = metadata.parseVersionInfo("""
                {
                  "id": "1.20.1-loader",
                  "inheritsFrom": "1.20.1",
                  "jar": "1.20.1",
                  "mainClass": "child.Main",
                  "arguments": {
                    "game": ["--loader-game"]
                  },
                  "libraries": [
                    {"name": "loader:library:2.0"}
                  ]
                }
                """);

        VersionInfo resolved = resolver.resolve(child, Map.of("1.20.1", parent));

        assertEquals("1.20.1-loader", resolved.id());
        assertNull(resolved.inheritsFrom());
        assertEquals("child.Main", resolved.mainClass());
        assertEquals("5", resolved.assetIndex().id());
        assertEquals(17, resolved.javaVersion().majorVersion());
        assertEquals("client-sha1", resolved.downloads().get("client").sha1());
        assertEquals(2, resolved.libraries().size());
        assertEquals("loader:library:2.0", resolved.libraries().get(0).name());
        assertEquals("base:library:1.0", resolved.libraries().get(1).name());
        assertEquals("1.20.1", resolved.jar());
        assertEquals("1.20.1", resolved.clientJarId());
        assertEquals(2, resolved.arguments().game().size());
        assertEquals(1, resolved.arguments().jvm().size());
    }

    @Test
    void rejectsMissingParent() {
        VersionInfo child = metadata.parseVersionInfo("""
                {
                  "id": "1.20.1-loader",
                  "inheritsFrom": "1.20.1"
                }
                """);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(child, Map.of()));
    }

    @Test
    void rejectsCircularInheritance() {
        VersionInfo first = metadata.parseVersionInfo("""
                {"id": "first", "inheritsFrom": "second"}
                """);
        VersionInfo second = metadata.parseVersionInfo("""
                {"id": "second", "inheritsFrom": "first"}
                """);

        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(first, Map.of("first", first, "second", second)));
    }
}
