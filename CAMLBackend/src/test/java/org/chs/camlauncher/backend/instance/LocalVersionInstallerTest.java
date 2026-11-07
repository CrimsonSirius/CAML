package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.download.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地自定义名称版本（如 1.21.4-DeepSeek）必须可以直接启动，
 * 不能因为官方版本清单里没有这个名字而失败。
 */
class LocalVersionInstallerTest {

    @TempDir
    Path gameDirectory;

    @Test
    void reusesLocallyInstalledVersionWithCustomDirectoryName() throws Exception {
        // Parent version: vanilla 1.21.4 installed by another launcher.
        writeVersionJson("1.21.4", """
                {
                  "id": "1.21.4",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assets": "17"
                }
                """);

        // Child version: the directory name is a custom display name,
        // while the JSON keeps the original vanilla id.
        String customJson = """
                {
                  "id": "1.21.4",
                  "inheritsFrom": "1.21.4",
                  "mainClass": "custom.loader.Main"
                }
                """;
        Path customFile = writeVersionJson("1.21.4-DeepSeek", customJson);
        Files.write(gameDirectory.resolve("versions/1.21.4-DeepSeek/1.21.4-DeepSeek.jar"),
                new byte[]{1});

        VersionInstaller installer = new VersionInstaller(
                new MetadataService(),
                new VersionManifestService(),
                new DownloadPlanner(),
                new AssetDownloadPlanner(),
                new FileDownloader()
        );

        // Pointing at an unreachable manifest proves the remote path is never used.
        InstalledVersion installed = installer.install(
                "1.21.4-DeepSeek",
                gameDirectory,
                URI.create("http://127.0.0.1:1/version_manifest_v2.json"),
                gameDirectory.resolve("cache/version_manifest_v2.json"),
                DownloadListener.NONE
        );

        assertEquals("1.21.4-DeepSeek", installed.id());
        assertEquals("1.21.4-DeepSeek", installed.manifest().id());
        assertEquals("custom.loader.Main", installed.manifest().mainClass());
        assertEquals("1.21.4-DeepSeek.jar", installed.clientJar().getFileName().toString());
        assertTrue(Files.isRegularFile(installed.versionJson()));
        // The local JSON file must not be rewritten or replaced.
        assertEquals(customJson, Files.readString(customFile, StandardCharsets.UTF_8));
    }

    private Path writeVersionJson(String versionId, String content) throws Exception {
        Path directory = gameDirectory.resolve("versions").resolve(versionId);
        Files.createDirectories(directory);
        Path file = directory.resolve(versionId + ".json");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}

