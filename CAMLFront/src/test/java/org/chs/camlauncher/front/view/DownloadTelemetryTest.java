package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.front.translation.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadTelemetryTest {
    @BeforeEach
    void useChineseLocale() {
        Language.setLocale("zh_cn");
    }

    @Test
    void keepsStageOrderAndIgnoresRegressions() {
        DownloadTelemetry telemetry = new DownloadTelemetry();

        telemetry.onStage("获取版本信息");
        telemetry.onStage("下载游戏文件与依赖库");
        telemetry.onStage("获取版本信息");

        DownloadTelemetry.Snapshot snapshot = telemetry.snapshot();
        assertEquals(
                List.of(Language.tr("download.stage.preparing"), "获取版本信息", "下载游戏文件与依赖库"),
                snapshot.stages()
        );
        assertEquals("下载游戏文件与依赖库", snapshot.currentStage());
        assertFalse(snapshot.finished());
        assertFalse(snapshot.failed());
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
        assertEquals(Language.tr("download.stage.completed"), completed.currentStage());
        assertTrue(completed.stages().contains(Language.tr("download.stage.completed")));
    }

    @Test
    void recordsFailureMessage() {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        telemetry.markFailed("网络中断");

        DownloadTelemetry.Snapshot snapshot = telemetry.snapshot();
        assertTrue(snapshot.failed());
        assertEquals(Language.tr("download.stage.failed"), snapshot.currentStage());
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