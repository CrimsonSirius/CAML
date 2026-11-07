package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.ui.control.CamlMemorySlider;

import java.util.function.IntConsumer;
import java.util.function.Consumer;

/** 设置页中的“游戏设置”。 */
final class GameSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final SettingsController controller;
    private final Consumer<Boolean> onVersionIsolationChanged;
    private final CamlMemorySlider memorySlider;
    private final Label memorySummary;
    private long totalMemoryMb;
    private long availableMemoryMb;
    private int usedMemoryMb;
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(300));

    GameSettingsView(LaunchSettings launchSettings, IntConsumer onMemoryChanged,
                     Consumer<Boolean> onVersionIsolationChanged,
                     SystemMemoryInfo systemMemory, SettingsController controller) {
        this.launchSettings = launchSettings;
        this.controller = controller;
        this.onVersionIsolationChanged = onVersionIsolationChanged;

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
        saveDebounce.setOnFinished(event -> onMemoryChanged.accept(launchSettings.maxMemoryMb()));
        memorySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int memoryMb = newValue.intValue();
            launchSettings.setMaxMemoryMb(memoryMb);
            updateMemorySummary(memoryMb);
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

    private void buildLayout() {
        Label heading = new Label("内存");
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label("调整游戏启动时使用的内存。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label memoryLabel = new Label("内存分配");
        memoryLabel.getStyleClass().add("caml-label-medium");
        memoryLabel.setLabelFor(memorySlider);

        VBox memoryBlock = new VBox(10, memoryLabel, memorySummary, memorySlider);
        memoryBlock.getStyleClass().add("app-memory-block");

        Label hint = new Label("拖动小球调整游戏分配；灰色条为已使用 + 游戏分配，右侧淡灰为未使用内存。");
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
                group
        );
    }

    private void updateMemorySummary(int value) {
        memorySummary.setText(String.format(
                "已使用 %d MB / 游戏 %d MB / 可用 %d MB / 总内存 %d MB",
                usedMemoryMb, value, availableMemoryMb, totalMemoryMb));
    }

}
