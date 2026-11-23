package org.chs.camlauncher.front.view;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.backend.system.DeviceInfo;
import org.chs.camlauncher.backend.system.SystemInfo;
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

class AboutSettingsViewTest {

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
    void linuxUsesKernelVersionLabel() {
        SystemInfo linux = new SystemInfo(
                "Ubuntu 24.04.1 LTS", "6.8.0-90-generic", SystemInfo.VersionKind.KERNEL,
                "amd64", "25", "Eclipse Adoptium");

        assertEquals("settings.about.kernelVersion", AboutSettingsView.versionLabelKey(linux));
    }

    @Test
    void windowsUsesSystemVersionLabel() {
        SystemInfo windows = new SystemInfo(
                "Windows 11", "10.0", SystemInfo.VersionKind.SYSTEM,
                "amd64", "25", "Eclipse Adoptium");

        assertEquals("settings.about.osVersion", AboutSettingsView.versionLabelKey(windows));
    }

    @Test
    void rendersGroupsKernelLabelAndLiveGameDirectory() throws Exception {
        long[] measured = onFxThread(() -> {
            BackendStatus status = new BackendStatus(
                    true, "1.0-SNAPSHOT", Path.of("/tmp/caml"), Path.of("/tmp/minecraft"));
            SystemInfo linux = new SystemInfo(
                    "Arch Linux", "7.3.5-zen1-1-zen", SystemInfo.VersionKind.KERNEL,
                    "amd64", "25", "Oracle Corporation");
            SimpleObjectProperty<Path> gameDirectory =
                    new SimpleObjectProperty<>(Path.of("/tmp/minecraft"));
            DeviceInfo device = new DeviceInfo(
                    "AMD Ryzen 9 7940HS",
                    List.of("AMD Radeon 780M", "NVIDIA GeForce RTX 4070 Laptop GPU"),
                    List.of(),
                    32768
            );
            AboutSettingsView view = new AboutSettingsView(status, linux, device, gameDirectory);

            VBox root = new VBox(view);
            root.setPrefSize(900, 700);
            Scene scene = new Scene(root, 900, 700);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
            root.applyCss();
            root.resize(900, 700);
            root.applyCss();
            root.layout();

            long groups = view.lookupAll(".app-settings-group").size();
            long kernelLabels = view.lookupAll(".app-about-label").stream()
                    .filter(node -> node instanceof Label label
                            && "内核版本".equals(label.getText()))
                    .count();
            long runtimeTitles = view.lookupAll(".app-settings-group-title").stream()
                    .filter(node -> node instanceof Label label
                            && "启动器运行环境".equals(label.getText()))
                    .count();
            long memoryValues = view.lookupAll(".app-about-value").stream()
                    .filter(node -> node instanceof Label label
                            && "32.0 GB".equals(label.getText()))
                    .count();

            gameDirectory.set(Path.of("/tmp/other-game"));
            root.applyCss();
            root.layout();
            long updatedPaths = view.lookupAll(".app-about-value").stream()
                    .filter(node -> node instanceof Label label
                            && "/tmp/other-game".equals(label.getText()))
                    .count();
            return new long[]{groups, kernelLabels, updatedPaths, runtimeTitles, memoryValues};
        });

        assertEquals(6, measured[0], "关于页应包含六个信息组");
        assertEquals(1, measured[1], "Linux 应显示内核版本标签");
        assertEquals(1, measured[2], "游戏目录应随共享设置实时更新");
        assertEquals(1, measured[3], "运行环境组应改名为启动器运行环境");
        assertEquals(1, measured[4], "运行内存只显示总容量");
    }
}
