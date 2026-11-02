package org.chs.camlauncher.backend.metadata;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetadataServiceTest {
    private final MetadataService metadata = new MetadataService();

    @Test
    void parsesVersionManifest() {
        VersionManifest manifest = metadata.parseVersionManifest("""
                {
                  "latest": {
                    "release": "1.21.5",
                    "snapshot": "25w14a"
                  },
                  "versions": [
                    {
                      "id": "1.21.5",
                      "type": "release",
                      "url": "https://example.com/1.21.5.json",
                      "time": "2026-01-01T00:00:00+00:00",
                      "releaseTime": "2026-01-01T00:00:00+00:00",
                      "sha1": "abc123",
                      "complianceLevel": 1
                    }
                  ]
                }
                """);

        assertEquals("1.21.5", manifest.latest().release());
        assertEquals("25w14a", manifest.latest().snapshot());
        assertEquals(1, manifest.versions().size());
        assertEquals("abc123", manifest.versions().getFirst().sha1());
        assertEquals(1, manifest.versions().getFirst().complianceLevel());
    }

    @Test
    void parsesVersionInfoAndRules() {
        VersionInfo info = metadata.parseVersionInfo("""
                {
                  "id": "1.20.1",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assets": "5",
                  "assetIndex": {
                    "id": "5",
                    "sha1": "asset-index-sha1",
                    "size": 10,
                    "totalSize": 20,
                    "url": "https://example.com/assets.json"
                  },
                  "arguments": {
                    "game": [
                      "--username",
                      "${auth_player_name}",
                      {
                        "rules": [{"action": "allow", "os": {"name": "linux"}}],
                        "value": ["--demo"]
                      },
                      {
                        "rules": [{"action": "allow", "features": {"has_custom_resolution": true}}],
                        "value": ["--width", "${resolution_width}"]
                      }
                    ],
                    "jvm": [
                      {
                        "rules": [{"action": "allow", "os": {"name": "windows"}}],
                        "value": ["-Dos.name=Windows 10"]
                      }
                    ]
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
                    {
                      "name": "com.example:test:1.0",
                      "rules": [{"action": "allow", "os": {"name": "linux"}}]
                    }
                  ]
                }
                """);

        assertEquals("1.20.1", info.id());
        assertEquals("net.minecraft.client.main.Main", info.mainClass());
        assertEquals("5", info.assetIndex().id());
        assertEquals(17, info.javaVersion().majorVersion());
        assertEquals("client-sha1", info.downloads().get("client").sha1());
        assertEquals(1, info.libraries().size());
        assertEquals(4, info.arguments().game().size());
        assertInstanceOf(StringArgument.class, info.arguments().game().getFirst());
        assertInstanceOf(RuledArgument.class, info.arguments().game().get(2));

        RuleEvaluator linux = new RuleEvaluator("linux", "x86_64", "6.0", Map.of());
        RuleEvaluator windows = new RuleEvaluator("windows", "x86_64", "10.0", Map.of());
        RuleEvaluator customResolution = new RuleEvaluator(
                "linux", "x86_64", "6.0", Map.of("has_custom_resolution", true));

        assertEquals(List.of("--demo"), info.arguments().game().get(2).resolve(linux));
        assertEquals(List.of(), info.arguments().game().get(2).resolve(windows));
        assertEquals(List.of("--width", "${resolution_width}"),
                info.arguments().game().get(3).resolve(customResolution));
        assertEquals(List.of(), info.arguments().game().get(3).resolve(linux));
        assertEquals(List.of("-Dos.name=Windows 10"), info.arguments().jvm().getFirst().resolve(windows));
        assertEquals(List.of(), info.arguments().jvm().getFirst().resolve(linux));
    }

    @Test
    void readsVersionInfoFromFile() throws Exception {
        var file = java.nio.file.Files.createTempFile("caml-version-", ".json");
        try {
            java.nio.file.Files.writeString(file, """
                    {
                      "id": "1.21.5",
                      "type": "release",
                      "mainClass": "net.minecraft.client.main.Main"
                    }
                    """);

            VersionInfo info = metadata.readVersionInfo(file);

            assertNotNull(info);
            assertEquals("1.21.5", info.id());
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }

    @Test
    void parsesAssetIndex() {
        AssetIndexFile index = metadata.parseAssetIndex("""
                {
                  "objects": {
                    "minecraft/sounds/click.ogg": {
                      "hash": "0123456789abcdef0123456789abcdef01234567",
                      "size": 42
                    }
                  }
                }
                """);

        assertEquals(1, index.objects().size());
        assertEquals(42, index.objects().get("minecraft/sounds/click.ogg").size());
    }
}
