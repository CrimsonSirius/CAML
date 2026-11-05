package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlSlider;

import java.lang.management.ManagementFactory;

/** 设置页中的“游戏设置”。 */
final class GameSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final CamlSlider memorySlider;
    private final Label memorySummary;
    private final long totalMemoryMb;
    private final long availableMemoryMb;

    GameSettingsView(LaunchSettings launchSettings) {
        this.launchSettings = launchSettings;

        MemorySnapshot memory = readSystemMemory();
        totalMemoryMb = memory.totalMb();
        availableMemoryMb = memory.availableMb();

        int maxMemoryMb = (int) Math.max(1024L, Math.min(Integer.MAX_VALUE, totalMemoryMb));
        int initialMemoryMb = Math.min(4096, maxMemoryMb);
        memorySlider = new CamlSlider(512, maxMemoryMb, initialMemoryMb);
        memorySlider.setBlockIncrement(256);
        memorySlider.setShowTickMarks(false);
        memorySlider.setShowTickLabels(false);
        memorySlider.setMaxWidth(Double.MAX_VALUE);
        memorySlider.getStyleClass().add("caml-memory-slider");

        memorySummary = new Label();
        memorySummary.getStyleClass().addAll("caml-body-medium", "app-memory-summary");
        memorySummary.setAlignment(Pos.CENTER);
        memorySummary.setMaxWidth(Double.MAX_VALUE);

        launchSettings.setMaxMemoryMb(roundMemory(memorySlider.getValue()));
        updateMemorySummary(memorySlider.getValue());
        memorySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateMemorySummary(newValue.doubleValue());
            launchSettings.setMaxMemoryMb(roundMemory(newValue.doubleValue()));
        });

        buildLayout();
    }

    private void buildLayout() {
        Label heading = new Label("游戏设置");
        heading.getStyleClass().add("caml-title-large");

        Label description = new Label("调整启动游戏时使用的内存。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label memoryLabel = new Label("内存分配");
        memoryLabel.getStyleClass().add("caml-label-medium");
        memoryLabel.setLabelFor(memorySlider);

        VBox memoryBlock = new VBox(10, memoryLabel, memorySummary, memorySlider);
        memoryBlock.getStyleClass().add("app-memory-block");

        Label hint = new Label("拖动小球调整，启动游戏时生效。可用内存为当前系统快照。");
        hint.getStyleClass().addAll("caml-body-small", "field-hint");
        hint.setWrapText(true);

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(new VBox(6, heading, description), memoryBlock, hint);
    }

    private int roundMemory(double value) {
        int max = (int) memorySlider.getMax();
        int rounded = (int) Math.round(value / 256.0) * 256;
        return Math.max(512, Math.min(max, rounded));
    }

    private void updateMemorySummary(double value) {
        memorySummary.setText(String.format(
                "游戏 %d MB / 可用 %d MB / 总内存 %d MB",
                roundMemory(value), availableMemoryMb, totalMemoryMb));
    }

    private static MemorySnapshot readSystemMemory() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                long total = os.getTotalMemorySize() / (1024 * 1024);
                long free = os.getFreeMemorySize() / (1024 * 1024);
                if (total > 0) {
                    return new MemorySnapshot(total, Math.max(0, free));
                }
            }
        } catch (RuntimeException ignored) {
        }

        long total = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long free = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        return new MemorySnapshot(Math.max(1024, total), Math.max(0, free));
    }

    private record MemorySnapshot(long totalMb, long availableMb) {
    }
}
