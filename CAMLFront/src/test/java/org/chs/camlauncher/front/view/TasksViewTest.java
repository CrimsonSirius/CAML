package org.chs.camlauncher.front.view;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.front.task.DownloadTask;
import org.chs.camlauncher.front.task.DownloadTelemetry;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasksViewTest {
    @TempDir
    Path dataDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @BeforeEach
    void useChineseLocale() throws Exception {
        LanguageService languageService = new LanguageService(
                dataDirectory, new SettingsService(dataDirectory));
        languageService.initialize();
        languageService.switchLanguage(LanguageSelectionMode.MANUAL, "zh_cn");
        UiText.initialize(languageService);
    }

    @Test
    void listsTasksAsModRowsAndOpensDownloadProgressDetail() throws Exception {
        boolean[] measured = onFxThread(() -> {
            DownloadTask task = task();
            TasksView view = new TasksView(
                    FXCollections.observableArrayList(task));
            Scene scene = layout(view);
            view.onShow();
            layout(view, scene);

            Label heading = (Label) view.lookup(".app-task-title");
            Button row = (Button) view.lookup(".app-mod-item");
            Label running = (Label) view.lookup(".app-task-status-running");
            assertNotNull(heading);
            assertNotNull(row);
            assertNotNull(running);
            assertEquals("任务", heading.getText());
            assertEquals("进行中", running.getText());

            row.fire();
            layout(view, scene);
            assertSame(task, view.currentTask());
            assertNotNull(view.lookup(".app-download-progress-page"));
            List<Button> pills = view.lookupAll(".app-pill-button").stream()
                    .map(Button.class::cast)
                    .toList();
            assertEquals(2, pills.size());

            Button cancel = pills.stream()
                    .filter(button -> UiText.tr("common.cancel").equals(button.getText()))
                    .findFirst()
                    .orElseThrow();
            cancel.fire();
            assertTrue(task.snapshot().cancelled());

            Button back = pills.stream()
                    .filter(button -> UiText.tr("tasks.back").equals(button.getText()))
                    .findFirst()
                    .orElseThrow();
            back.fire();
            layout(view, scene);
            assertNull(view.currentTask());
            assertNotNull(view.lookup(".app-mod-item"));

            return new boolean[]{true};
        });

        assertTrue(measured[0]);
    }

    @Test
    void completedTaskKeepsItsHistoryRowAndShowsCompletedStatus() throws Exception {
        onFxThread(() -> {
            DownloadTask task = task();
            TasksView view = new TasksView(FXCollections.observableArrayList(task));
            Scene scene = layout(view);
            view.onShow();

            task.complete();
            view.refreshNow();
            layout(view, scene);

            Label completed = (Label) view.lookup(".app-task-status-completed");
            assertNotNull(completed);
            assertEquals("已完成", completed.getText());
            assertNotNull(view.lookup(".app-mod-item"));
            return null;
        });
    }

    private static DownloadTask task() {
        DownloadTelemetry telemetry = new DownloadTelemetry();
        DownloadTask task = new DownloadTask(
                "operation.installLoader", new Object[]{"Fabric", "fabric-test"},
                "fabric-test", telemetry, new DownloadCancellationToken());
        task.attach(() -> true);
        return task;
    }

    private static Scene layout(TasksView view) {
        Scene scene = new Scene(view, 900, 600);
        CamlTheme.install(scene);
        addStylesheet(scene, "/themes/coffee.css");
        addStylesheet(scene, "/styles/app.css");
        return scene;
    }

    private static void layout(TasksView view, Scene scene) {
        view.applyCss();
        view.resize(scene.getWidth(), scene.getHeight());
        view.applyCss();
        view.layout();
    }
}
