package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.settings.SettingsService;
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
import java.util.stream.IntStream;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 下载页的滚动与虚拟化布局回归测试。 */
class DownloadViewsLayoutTest {

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
    void versionListOnlyCreatesVisibleCells() throws Exception {
        int[] measured = onFxThread(() -> {
            ListView<VersionManifest.Version> list =
                    DownloadVersionsView.createVersionList(version -> { });
            list.getStyleClass().add("app-download-version-list");
            list.getItems().setAll(versions(5_000));

            VBox root = new VBox(list);
            VBox.setVgrow(list, Priority.ALWAYS);
            root.setPrefSize(900, 600);
            Scene scene = new Scene(root, 900, 600);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
            root.applyCss();
            root.resize(900, 600);
            root.applyCss();
            root.layout();

            Node firstRow = list.lookup(".app-download-version-item");
            return new int[]{
                    list.getItems().size(),
                    list.lookupAll(".app-download-version-cell").size(),
                    firstRow == null ? 0 : (int) firstRow.getBoundsInParent().getWidth(),
                    (int) list.getWidth()
            };
        });

        assertEquals(5_000, measured[0], "版本数据必须完整交给列表模型");
        assertTrue(measured[1] > 0, "列表至少应创建可见行");
        assertTrue(measured[1] < 100,
                "版本列表必须虚拟化，不能为 5000 个版本一次性创建全部行");
        assertTrue(measured[2] > 0 && measured[2] >= measured[3] - 20,
                "版本行必须铺满列表宽度，日期才能稳定靠右");
    }

    @Test
    void setupOptionsUseAnIndependentVerticalScrollPane() throws Exception {
        boolean[] measured = onFxThread(() -> {
            VBox options = new VBox();
            for (int index = 0; index < 8; index++) {
                options.getChildren().add(new Label("option-" + index));
            }

            ScrollPane scroll = DownloadVersionSetupView.createOptionsScroll(options);
            return new boolean[]{
                    scroll.isFitToWidth(),
                    scroll.getHbarPolicy() == ScrollPane.ScrollBarPolicy.NEVER,
                    scroll.getVbarPolicy() == ScrollPane.ScrollBarPolicy.AS_NEEDED,
                    scroll.getContent() == options,
                    VBox.getVgrow(scroll) == Priority.ALWAYS
            };
        });

        assertTrue(measured[0], "选项内容应跟随滚动视口宽度");
        assertTrue(measured[1], "选项区不应出现横向滚动条");
        assertTrue(measured[2], "高度不足时应显示纵向滚动条");
        assertTrue(measured[3], "选项内容必须放进滚动容器");
        assertTrue(measured[4], "滚动区应占用页面剩余高度");
    }

    @Test
    void progressPageKeepsHeaderVisibleAndScrollsLongStageLists() throws Exception {
        boolean[] measured = onFxThread(() -> {
            DownloadTelemetry telemetry = new DownloadTelemetry();
            for (int index = 0; index < 80; index++) {
                telemetry.onStage("install-stage-" + index);
            }
            DownloadRequest request = new DownloadRequest(
                    versions(1).getFirst(), "forge-optifine-test", dataDirectory,
                    null, null, null, null, null, null, null);
            DownloadProgressView view = new DownloadProgressView(request, telemetry, () -> { });

            VBox root = new VBox(view);
            VBox.setVgrow(view, Priority.ALWAYS);
            root.setPrefSize(900, 520);
            Scene scene = new Scene(root, 900, 520);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
            root.applyCss();
            root.resize(900, 520);
            root.applyCss();
            root.layout();

            ScrollPane scroll = (ScrollPane) view.lookup(".app-download-progress-scroll");
            Node header = view.lookup(".app-download-header");
            boolean result = scroll != null
                    && header != null
                    && scroll.isFitToWidth()
                    && scroll.getHbarPolicy() == ScrollPane.ScrollBarPolicy.NEVER
                    && scroll.getVbarPolicy() == ScrollPane.ScrollBarPolicy.AS_NEEDED
                    && VBox.getVgrow(scroll) == Priority.ALWAYS
                    && header.getBoundsInParent().getHeight() > 0
                    && scroll.getContent().getBoundsInLocal().getHeight()
                    > scroll.getViewportBounds().getHeight();
            view.dispose();
            return new boolean[]{result};
        });

        assertTrue(measured[0], "阶段很多时页面应只滚动内容，并保持标题和返回按钮可见");
    }

    private static List<VersionManifest.Version> versions(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new VersionManifest.Version(
                        "1." + index,
                        "release",
                        "https://example.invalid/" + index + ".json",
                        "2026-01-01T00:00:00+00:00",
                        "2026-01-01T00:00:00+00:00",
                        "0123456789abcdef0123456789abcdef01234567",
                        1
                ))
                .toList();
    }
}
