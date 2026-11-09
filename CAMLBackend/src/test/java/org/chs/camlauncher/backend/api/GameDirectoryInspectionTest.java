package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameDirectoryInspectionTest {

    @TempDir
    Path root;

    @Test
    void reportsInstalledVersionsAndMissingDirectory() throws Exception {
        Path gameDirectory = root.resolve("game");
        Path installedVersion = gameDirectory.resolve("versions").resolve("1.21.4");
        Files.createDirectories(installedVersion);
        Files.writeString(installedVersion.resolve("1.21.4.json"), "{}");
        // 没有版本 JSON 的目录不算安装完成。
        Files.createDirectories(gameDirectory.resolve("versions").resolve("incomplete"));

        CAMLBackend backend = new CAMLBackend(
                new BackendConfig(root.resolve("data"), gameDirectory));
        try (backend) {
            GameDirectoryInfo info = backend.inspectGameDirectory(gameDirectory);

            assertTrue(info.directoryExists());
            assertTrue(info.hasInstalledVersions());
            assertEquals(List.of("1.21.4"), info.installedVersions());

            GameDirectoryInfo missing = backend.inspectGameDirectory(root.resolve("missing"));
            assertFalse(missing.directoryExists());
            assertTrue(missing.installedVersions().isEmpty());
        }
    }

    @Test
    void refusesToInstallIntoExistingInstanceDirectory() throws Exception {
        Path gameDirectory = root.resolve("game");
        Files.createDirectories(gameDirectory.resolve("versions").resolve("existing"));

        CAMLBackend backend = new CAMLBackend(
                new BackendConfig(root.resolve("data"), gameDirectory));
        try (backend) {
            assertThrows(IOException.class, () -> backend.installVersion(
                    "1.20.1", "existing", gameDirectory, DownloadListener.NONE));
        }
    }
}
