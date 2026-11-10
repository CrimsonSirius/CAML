package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.front.translation.Language;
import org.chs.camlauncher.ui.control.CamlMemorySlider;

import java.util.function.IntConsumer;
import java.util.function.Consumer;

/** 设置页中的“游戏设置”。 */
final class GameSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final SettingsController controller;
    private final IntConsumer onMemoryChanged;
    private final Consumer<Boolean> onVersionIsolationChanged;
    private final JavaSettingsView javaSettings;
    private final CamlMemorySlider memorySlider;
    private final Label memorySummary;
    private long totalMemoryMb;
    private long availableMemoryMb;
    private int usedMemoryMb;
    private boolean savePending;
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(300));

    GameSettingsView(LaunchSettings launchSettings, IntConsumer onMemoryChanged,
                     Consumer<Boolean> onVersionIsolationChanged,
                     SystemMemoryInfo systemMemory, SettingsController controller) {
        this.launchSettings = launchSettings;
        this.controller = controller;
        this.onMemoryChanged = onMemoryChanged;
        this.onVersionIsolationChanged = onVersionIsolationChanged;
        javaSettings = new JavaSettingsView(launchSettings, controller);

        totalMemoryMb = systemMemory.totalMb();
        availableMemoryMb = systemMemory.availableMb();

        int maxMemoryMb = (int) Math.max(1024L, Math.min(Integer.MAX_VALUE, totalMemoryMb));
        int initialMemoryMb = Math.max(512, Math.min(maxMemoryMb, launchSettings.maxMemoryMb()));
        usedMemoryMb = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0, totalMemoryMb - availableMemoryMb));
        memorySlider = new CamlMemorySlider(512, maxMemoryMb, 256, initialMemoryMb, usedMemoryMb);
        memorySlider.setMaxWidth(Double.MAX_VALUE);

        memorySummary = new Label();
        memorySummary.getStyleClass().addAll("caml-body-medium", "app-memory-summary");
        memorySummary.setAlignment(Pos.CENTER);
        memorySummary.setMaxWidth(Double.MAX_VALUE);

        launchSettings.setMaxMemoryMb(memorySlider.getValue());
        updateMemorySummary(memorySlider.getValue());
        saveDebounce.setOnFinished(event -> {
            savePending = false;
            onMemoryChanged.accept(launchSettings.maxMemoryMb());
        });
        memorySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int memoryMb = newValue.intValue();
            launchSettings.setMaxMemoryMb(memoryMb);
            updateMemorySummary(memoryMb);
            savePending = true;
            saveDebounce.playFromStart();
        });

        buildLayout();
    }

    /** 由 500ms 内存监视器在 JavaFX 线程调用。 */
    void updateSystemMemory(SystemMemoryInfo systemMemory) {
        totalMemoryMb = systemMemory.totalMb();
        availableMemoryMb = Math.min(totalMemoryMb, systemMemory.availableMb());
        usedMemoryMb = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0, totalMemoryMb - availableMemoryMb));
        memorySlider.setUsedMemoryMb(usedMemoryMb);
        updateMemorySummary(memorySlider.getValue());
    }

    /** 关闭前立即提交仍在防抖窗口内的内存修改。 */
    void flushPendingSave() {
        if (savePending) {
            savePending = false;
            saveDebounce.stop();
            onMemoryChanged.accept(launchSettings.maxMemoryMb());
        }
    }

    private void buildLayout() {
        Label heading = new Label(Language.tr("settings.game.memory.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(Language.tr("settings.game.memory.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label memoryLabel = new Label(Language.tr("settings.game.memory.label"));
        memoryLabel.getStyleClass().add("caml-label-medium");
        memoryLabel.setLabelFor(memorySlider);

        VBox memoryBlock = new VBox(10, memoryLabel, memorySummary, memorySlider);
        memoryBlock.getStyleClass().add("app-memory-block");

        Label hint = new Label(Language.tr("settings.game.memory.hint"));
        hint.getStyleClass().addAll("caml-body-small", "field-hint");
        hint.setWrapText(true);

        VBox group = new VBox(16, heading, description, memoryBlock, hint);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(
                new GameLocationSettingsView(launchSettings, controller),
                new VersionIsolationSettingsView(launchSettings, onVersionIsolationChanged),
                javaSettings,
                group
        );
    }

    private void updateMemorySummary(int value) {
        memorySummary.setText(Language.tr("settings.game.memory.summary",
                usedMemoryMb, value, availableMemoryMb, totalMemoryMb));
    }

}