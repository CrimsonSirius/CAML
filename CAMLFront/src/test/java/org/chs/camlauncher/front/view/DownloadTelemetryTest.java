package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadTelemetryTest {
    @TempDir
    Path dataDirectory;

    private LanguageService languageService;

    @BeforeEach
    void useChineseLocale() throws Exception {
        languageService = new LanguageService(dataDirectory, new SettingsService(dataDirectory));
        languageService.initialize();
        languageService.switchLanguage(LanguageSelectionMode.MANUAL, "zh_cn");
        UiText.initialize(languageService);
    }

    @Test
    void keepsStageOrderAndIgnoresRegressions() {
        DownloadTelemetry telemetry = new DownloadTelemetry();

        telemetry.onStage(DownloadStage.of("version.fetch"));
        telemetry.onStage(DownloadStage.of("version.downloadFiles"));
        telemetry.onStage(DownloadStage.of("version.fetch"));

        DownloadTelemetry.Snapshot snapshot = telemetry.snapshot();
        assertEquals(
                List.of(
                        UiText.tr("download.stage.preparing"),
                        UiText.tr("download.stage.version.fetch"),
                        UiText.tr("download.stage.version.downloadFiles")
                ),
                snapshot.stages()
        );
        assertEquals(UiText.tr("download.stage.version.downloadFiles"), snapshot.currentStage());
        assertFalse(snapshot.finished());
        assertFalse(snapshot.failed());
    }

    @Test
    void translatesStageCodesWithCurrentLocale() throws Exception {
        languageService.switchLanguage(LanguageSelectionMode.MANUAL, "en_us");
        DownloadTelemetry telemetry = new DownloadTelemetry();

        telemetry.onStage(DownloadStage.of("loader.installer.download", "Forge"));

        assertEquals("Downloading Forge installer", telemetry.snapshot().currentStage());
    }

    @Test
    void aggregatesSummaryAndCompletion() {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        telemetry.onSummary(new DownloadListener.DownloadSummary(50, 100, 0, 2));

        DownloadTelemetry.Snapshot running = telemetry.snapshot();
        assertEquals(50, running.downloadedBytes());
        assertEquals(100, running.totalBytes());
        assertEquals(2, running.totalItems());
        assertEquals(0, running.completedItems());

        telemetry.markCompleted();
        DownloadTelemetry.Snapshot completed = telemetry.snapshot();
        assertTrue(completed.finished());
        assertEquals(100, completed.downloadedBytes());
        assertEquals(2, completed.completedItems());
        assertEquals(UiText.tr("download.stage.completed"), completed.currentStage());
        assertTrue(completed.stages().contains(UiText.tr("download.stage.completed")));
    }

    @Test
    void recordsFailureMessage() {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        telemetry.markFailed("网络中断");

        DownloadTelemetry.Snapshot snapshot = telemetry.snapshot();
        assertTrue(snapshot.failed());
        assertEquals(UiText.tr("download.stage.failed"), snapshot.currentStage());
        assertEquals("网络中断", snapshot.failureMessage());
    }

    @Test
    void accumulatesProgressAcrossDownloadPlans() {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        telemetry.onSummary(new DownloadListener.DownloadSummary(0, 100, 0, 1));
        telemetry.onSummary(new DownloadListener.DownloadSummary(100, 100, 1, 1));

        telemetry.onSummary(new DownloadListener.DownloadSummary(0, 50, 0, 2));
        DownloadTelemetry.Snapshot secondPlan = telemetry.snapshot();
        assertEquals(100, secondPlan.downloadedBytes());
        assertEquals(150, secondPlan.totalBytes());
        assertEquals(1, secondPlan.completedItems());
        assertEquals(3, secondPlan.totalItems());

        telemetry.onSummary(new DownloadListener.DownloadSummary(25, 50, 1, 2));
        DownloadTelemetry.Snapshot halfSecondPlan = telemetry.snapshot();
        assertEquals(125, halfSecondPlan.downloadedBytes());
        assertEquals(2, halfSecondPlan.completedItems());
    }
}
