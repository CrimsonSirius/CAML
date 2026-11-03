package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.AssetIndex;
import org.chs.camlauncher.backend.metadata.JavaVersion;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualAssetInstallerTest {
    @TempDir
    Path gameDirectory;

    @Test
    void reconstructsVirtualAssetsAndReturnsVirtualDirectory() throws Exception {
        String hash = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d";
        Path assetObject = gameDirectory.resolve("assets/objects/aa/" + hash);
        Files.createDirectories(assetObject.getParent());
        byte[] content = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(assetObject, content);

        Path assetIndexFile = gameDirectory.resolve("assets/indexes/legacy.json");
        Files.createDirectories(assetIndexFile.getParent());
        Files.writeString(assetIndexFile, """
                {
                  "virtual": true,
                  "objects": {
                    "minecraft/sounds/click.ogg": {
                      "hash": "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d",
                      "size": 5
                    }
                  }
                }
                """);

        AssetIndex assetIndex = new AssetIndex("legacy", "sha1", 1L, 1L, "https://example.com/legacy.json");
        VersionInfo manifest = new VersionInfo(
                "1.6.4", "release", null, "net.minecraft.client.main.Main", "legacy", assetIndex,
                null, null, null, new JavaVersion("jre-legacy", 8), List.of(), null,
                null, null, null, null
        );
        InstalledVersion instance = new InstalledVersion(
                "1.6.4", manifest, gameDirectory,
                gameDirectory.resolve("versions/1.6.4"),
                gameDirectory.resolve("versions/1.6.4/1.6.4.json"),
                gameDirectory.resolve("versions/1.6.4/1.6.4.jar"),
                gameDirectory.resolve("libraries"),
                gameDirectory.resolve("assets"),
                assetIndexFile
        );
        LaunchPlan plan = new LaunchPlan(
                instance,
                manifest,
                new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}"),
                new JavaRuntime(Path.of("/tmp/java"), "8.0", 8, "test", "x86_64", 64, true),
                LaunchOptions.defaults(),
                gameDirectory,
                gameDirectory.resolve("assets"),
                gameDirectory.resolve("versions/1.6.4/natives")
        );

        LaunchPlan prepared = new VirtualAssetInstaller().prepare(plan);

        Path virtualFile = gameDirectory.resolve("assets/virtual/legacy/minecraft/sounds/click.ogg");
        assertTrue(Files.isRegularFile(virtualFile));
        assertArrayEquals(content, Files.readAllBytes(virtualFile));
        assertEquals(gameDirectory.resolve("assets/virtual/legacy"), prepared.assetsDirectory());
    }
}
