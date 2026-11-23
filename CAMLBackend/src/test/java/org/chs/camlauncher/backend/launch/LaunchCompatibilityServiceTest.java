package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.metadata.MetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchCompatibilityServiceTest {
    @TempDir
    Path gameDirectory;

    @Test
    void warnsForLwjgl2WhenXrandrIsMissing() throws Exception {
        writeVersion("1.12.2", """
                {"id":"1.12.2","mainClass":"net.minecraft.client.main.Main",
                 "libraries":[{"name":"org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209"}]}
                """);
        LaunchCompatibilityService service = new LaunchCompatibilityService(
                new MetadataService(), "linux", command -> false);

        assertTrue(service.needsXrandrWarning(gameDirectory, "1.12.2"));
    }

    @Test
    void doesNotWarnWhenXrandrExistsOrVersionUsesLwjgl3() throws Exception {
        writeVersion("1.12.2", """
                {"id":"1.12.2","mainClass":"net.minecraft.client.main.Main",
                 "libraries":[{"name":"org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209"}]}
                """);
        writeVersion("1.21.4", """
                {"id":"1.21.4","mainClass":"net.minecraft.client.main.Main",
                 "libraries":[{"name":"org.lwjgl:lwjgl:3.3.3"}]}
                """);

        assertFalse(new LaunchCompatibilityService(
                new MetadataService(), "linux", command -> true)
                .needsXrandrWarning(gameDirectory, "1.12.2"));
        assertFalse(new LaunchCompatibilityService(
                new MetadataService(), "linux", command -> false)
                .needsXrandrWarning(gameDirectory, "1.21.4"));
        assertFalse(new LaunchCompatibilityService(
                new MetadataService(), "windows", command -> false)
                .needsXrandrWarning(gameDirectory, "1.12.2"));
    }

    @Test
    void resolvesLwjgl2ThroughInheritedVersion() throws Exception {
        writeVersion("1.12.2", """
                {"id":"1.12.2","mainClass":"net.minecraft.client.main.Main",
                 "libraries":[{"name":"org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209"}]}
                """);
        writeVersion("forge-instance", """
                {"id":"forge-instance","inheritsFrom":"1.12.2",
                 "mainClass":"net.minecraft.launchwrapper.Launch","libraries":[]}
                """);

        assertTrue(new LaunchCompatibilityService(
                new MetadataService(), "linux", command -> false)
                .needsXrandrWarning(gameDirectory, "forge-instance"));
    }

    private void writeVersion(String id, String json) throws Exception {
        Path directory = gameDirectory.resolve("versions").resolve(id);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(id + ".json"), json);
    }
}
