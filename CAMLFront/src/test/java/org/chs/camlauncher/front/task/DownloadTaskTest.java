package org.chs.camlauncher.front.task;

import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadTaskTest {
    @TempDir
    Path dataDirectory;

    @BeforeEach
    void initializeText() throws Exception {
        LanguageService language = new LanguageService(
                dataDirectory, new SettingsService(dataDirectory));
        language.initialize();
        language.switchLanguage(LanguageSelectionMode.MANUAL, "zh_cn");
        UiText.initialize(language);
    }

    @Test
    void cancelUpdatesTelemetryAndNotifiesListeners() {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        DownloadTask task = new DownloadTask(
                "download.progress.title", new Object[]{"1.21.4"}, "1.21.4",
                telemetry, new DownloadCancellationToken());
        AtomicInteger notifications = new AtomicInteger();
        task.addListener(notifications::incrementAndGet);
        task.attach(() -> true);

        assertTrue(task.cancel());
        assertTrue(task.snapshot().cancelled());
        assertFalse(task.isRunning());
        assertEquals(1, notifications.get());
        assertEquals("正在下载 1.21.4", task.title());
    }

    @Test
    void completionAndFailureArePropagated() {
        DownloadTelemetry completedTelemetry = new DownloadTelemetry();
        DownloadTask completed = new DownloadTask(
                "operation.downloadMod", new Object[]{"sodium.jar"}, "sodium.jar",
                completedTelemetry, new DownloadCancellationToken());
        AtomicInteger completedNotifications = new AtomicInteger();
        completed.addListener(completedNotifications::incrementAndGet);

        completed.complete();

        assertTrue(completed.snapshot().finished());
        assertEquals(1, completedNotifications.get());

        DownloadTelemetry failedTelemetry = new DownloadTelemetry();
        DownloadTask failed = new DownloadTask(
                "operation.downloadMod", new Object[]{"sodium.jar"}, "sodium.jar",
                failedTelemetry, new DownloadCancellationToken());
        AtomicInteger failedNotifications = new AtomicInteger();
        failed.addListener(failedNotifications::incrementAndGet);

        failed.fail("网络中断");

        assertTrue(failed.snapshot().failed());
        assertEquals("网络中断", failed.snapshot().failureMessage());
        assertEquals(1, failedNotifications.get());
    }
}
