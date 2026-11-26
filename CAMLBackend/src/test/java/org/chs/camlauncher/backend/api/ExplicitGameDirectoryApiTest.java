package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.launch.GameLaunchService;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExplicitGameDirectoryApiTest {
    @Test
    void installAndLaunchEntryPointsRequireAnExplicitGameDirectory() {
        assertThrows(NoSuchMethodException.class, () -> CAMLBackend.class.getMethod(
                "installVersion", String.class, Path.class, DownloadListener.class));
        assertThrows(NoSuchMethodException.class, () -> CAMLBackend.class.getMethod(
                "launchGame", String.class, Path.class, String.class, LaunchOptions.class,
                DownloadListener.class, ProcessListener.class));
        assertThrows(NoSuchMethodException.class, () -> BackendDownloads.class.getMethod(
                "installVersion", String.class, DownloadListener.class));
        assertThrows(NoSuchMethodException.class, () -> BackendLaunch.class.getMethod(
                "launchGame", String.class, String.class, LaunchOptions.class,
                DownloadListener.class, ProcessListener.class));

        assertDoesNotThrow(() -> BackendDownloads.class.getMethod(
                "installVersion", String.class, Path.class, DownloadListener.class));
        assertDoesNotThrow(() -> BackendLaunch.class.getMethod(
                "launchGame", String.class, Path.class, String.class, LaunchOptions.class,
                DownloadListener.class, ProcessListener.class));
    }

    @Test
    void gameLaunchServiceHasNoImplicitDirectoryLaunch() {
        assertThrows(NoSuchMethodException.class, () -> GameLaunchService.class.getMethod(
                "launch", String.class, String.class, LaunchOptions.class,
                DownloadListener.class, ProcessListener.class));
    }
}
