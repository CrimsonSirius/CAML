package org.chs.camlauncher.backend.launch.patch;

import org.chs.camlauncher.backend.metadata.model.Arguments;
import org.chs.camlauncher.backend.metadata.model.StringArgument;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForge262VulkanFixTest {
    @TempDir
    Path gameDataDirectory;

    @Test
    void disablesEarlyWindowForNeoForge262WithVulkan() throws Exception {
        writeOptions("vulkan");
        Path config = gameDataDirectory.resolve("config/fml.toml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, """
                disableConfigWatcher = false
                earlyWindowControl = true
                # keep this comment
                """, StandardCharsets.UTF_8);

        NeoForge262VulkanFix.apply(neoforgeManifest("26.2.0.93"), gameDataDirectory);

        String content = Files.readString(config, StandardCharsets.UTF_8);
        assertTrue(content.contains("earlyWindowControl = false"));
        assertTrue(content.contains("disableConfigWatcher = false"));
        assertTrue(content.contains("# keep this comment"));
    }

    @Test
    void createsFmlConfigWhenMissing() throws Exception {
        writeOptions("vulkan");

        NeoForge262VulkanFix.apply(neoforgeManifest("26.2.0.93"), gameDataDirectory);

        assertEquals(
                "earlyWindowControl = false\n",
                Files.readString(gameDataDirectory.resolve("config/fml.toml"), StandardCharsets.UTF_8)
        );
    }

    @Test
    void doesNotChangeOpenGlInstance() throws Exception {
        writeOptions("default");
        Path config = gameDataDirectory.resolve("config/fml.toml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "earlyWindowControl = true\n", StandardCharsets.UTF_8);

        NeoForge262VulkanFix.apply(neoforgeManifest("26.2.0.93"), gameDataDirectory);

        assertEquals(
                "earlyWindowControl = true\n",
                Files.readString(config, StandardCharsets.UTF_8)
        );
    }

    @Test
    void doesNotChangeNeoForge263() throws Exception {
        writeOptions("vulkan");
        Path config = gameDataDirectory.resolve("config/fml.toml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "earlyWindowControl = true\n", StandardCharsets.UTF_8);

        NeoForge262VulkanFix.apply(neoforgeManifest("26.3.0.101"), gameDataDirectory);

        assertEquals(
                "earlyWindowControl = true\n",
                Files.readString(config, StandardCharsets.UTF_8)
        );
    }

    @Test
    void recognizesVulkanOptionsWithoutChangingOtherValues() throws Exception {
        writeOptions("\"vulkan\"");
        Path config = gameDataDirectory.resolve("config/fml.toml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "earlyWindowControl = false\n", StandardCharsets.UTF_8);

        NeoForge262VulkanFix.apply(neoforgeManifest("26.2.0.93"), gameDataDirectory);

        assertEquals(
                "earlyWindowControl = false\n",
                Files.readString(config, StandardCharsets.UTF_8)
        );
    }

    @Test
    void ignoresNonNeoForgeVersions() throws Exception {
        writeOptions("vulkan");

        NeoForge262VulkanFix.apply(
                new VersionInfo("1.21.1", "release", null, "net.minecraft.client.main.Main",
                        null, null, null, null, null, null, List.of(), null, null,
                        null, null, null, null),
                gameDataDirectory
        );

        assertFalse(Files.exists(gameDataDirectory.resolve("config/fml.toml")));
    }

    private void writeOptions(String backend) throws Exception {
        Files.writeString(
                gameDataDirectory.resolve("options.txt"),
                "preferredGraphicsBackend:" + backend + "\n",
                StandardCharsets.UTF_8
        );
    }

    private static VersionInfo neoforgeManifest(String version) {
        return new VersionInfo(
                "neoforge-" + version,
                "release",
                "26.2",
                "net.neoforged.fml.startup.Client",
                null,
                null,
                null,
                new Arguments(
                        List.of(
                                new StringArgument("--fml.neoForgeVersion"),
                                new StringArgument(version)
                        ),
                        List.of()
                ),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
