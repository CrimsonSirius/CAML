package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadPlannerTest {
    @TempDir
    Path gameDirectory;

    @Test
    void plansClientLibrariesNativesAndAssetIndex() throws Exception {
        VersionInfo version = new MetadataService().parseVersionInfo("""
                {
                  "id": "1.20.1",
                  "downloads": {
                    "client": {
                      "sha1": "client-sha1",
                      "size": 10,
                      "url": "https://example.com/client.jar"
                    }
                  },
                  "assetIndex": {
                    "id": "5",
                    "sha1": "asset-index-sha1",
                    "size": 11,
                    "totalSize": 20,
                    "url": "https://example.com/assets.json"
                  },
                  "logging": {
                    "client": {
                      "argument": "-Dlog4j.configurationFile=${path}",
                      "file": {
                        "id": "client-1.12.xml",
                        "sha1": "log-sha1",
                        "size": 14,
                        "url": "https://example.com/log.xml"
                      },
                      "type": "log4j2-xml"
                    }
                  },
                  "libraries": [
                    {
                      "name": "com.example:lib:1.0",
                      "downloads": {
                        "artifact": {
                          "path": "com/example/lib/1.0/lib-1.0.jar",
                          "sha1": "lib-sha1",
                          "size": 12,
                          "url": "https://example.com/lib.jar"
                        }
                      }
                    },
                    {
                      "name": "com.example:native:1.0",
                      "natives": {"linux": "natives-linux"},
                      "downloads": {
                        "classifiers": {
                          "natives-linux": {
                            "path": "com/example/native/1.0/native-1.0-natives-linux.jar",
                            "sha1": "native-sha1",
                            "size": 13,
                            "url": "https://example.com/native.jar"
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        DownloadPlanner planner = new DownloadPlanner(
                List.of(OfficialDownloadSource.INSTANCE),
                new RuleEvaluator("linux", "x86_64", "6.0", Map.of()),
                "linux",
                64
        );

        DownloadPlan plan = planner.plan(version, gameDirectory);

        assertEquals(5, plan.items().size());
        assertTrue(plan.items().stream().anyMatch(item ->
                item.destination().endsWith(Path.of("versions", "1.20.1", "1.20.1.jar"))));
        assertTrue(plan.items().stream().anyMatch(item ->
                item.destination().endsWith(Path.of("libraries", "com", "example", "lib", "1.0", "lib-1.0.jar"))));
        assertTrue(plan.items().stream().anyMatch(item ->
                item.destination().endsWith(Path.of("libraries", "com", "example", "native", "1.0",
                        "native-1.0-natives-linux.jar"))));
        assertTrue(plan.items().stream().anyMatch(item ->
                item.destination().endsWith(Path.of("assets", "indexes", "5.json"))));
        assertTrue(plan.items().stream().anyMatch(item ->
                item.destination().endsWith(Path.of("assets", "log_configs", "client-1.12.xml"))));
        assertEquals(5, plan.missing().size());
    }

    @Test
    void downloadsClientJarToInheritedJarId() throws Exception {
        VersionInfo version = new MetadataService().parseVersionInfo("""
                {
                  "id": "forge-instance",
                  "jar": "1.20.1",
                  "downloads": {
                    "client": {
                      "sha1": "client-sha1",
                      "size": 10,
                      "url": "https://example.com/client.jar"
                    }
                  }
                }
                """);

        DownloadPlan plan = new DownloadPlanner(
                List.of(OfficialDownloadSource.INSTANCE),
                new RuleEvaluator("linux", "x86_64", "6.0", Map.of()),
                "linux",
                64
        ).plan(version, gameDirectory);

        assertEquals(1, plan.items().size());
        assertEquals(
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar").toAbsolutePath().normalize(),
                plan.items().getFirst().destination()
        );
        assertEquals("1.20.1", version.clientJarId());
    }

    @Test
    void plansLegacyForgeLibrariesWithoutDownloads() throws Exception {
        VersionInfo version = new MetadataService().parseVersionInfo("""
                {
                  "id": "old-forge",
                  "libraries": [
                    {
                      "name": "jline:jline:2.13",
                      "url": "https://maven.example/repo/",
                      "checksums": ["jline-sha1"]
                    },
                    {
                      "name": "org.ow2.asm:asm-all:5.0.3"
                    },
                    {
                      "name": "server.only:library:1.0",
                      "url": "https://maven.example/repo/",
                      "clientreq": false
                    }
                  ]
                }
                """);

        DownloadPlan plan = new DownloadPlanner(
                List.of(OfficialDownloadSource.INSTANCE),
                new RuleEvaluator("linux", "x86_64", "6.0", Map.of()),
                "linux",
                64
        ).plan(version, gameDirectory);

        assertEquals(2, plan.items().size());
        DownloadItem jline = plan.items().stream()
                .filter(item -> item.label().equals("jline:jline:2.13"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                gameDirectory.resolve("libraries/jline/jline/2.13/jline-2.13.jar").toAbsolutePath().normalize(),
                jline.destination()
        );
        assertEquals("https://maven.example/repo/jline/jline/2.13/jline-2.13.jar",
                jline.candidates().getFirst().toString());
        assertEquals("jline-sha1", jline.sha1());

        DownloadItem asm = plan.items().stream()
                .filter(item -> item.label().equals("org.ow2.asm:asm-all:5.0.3"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                "https://libraries.minecraft.net/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar",
                asm.candidates().getFirst().toString()
        );
    }
}
