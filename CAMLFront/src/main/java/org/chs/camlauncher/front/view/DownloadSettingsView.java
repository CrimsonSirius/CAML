package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.ui.control.CamlSlider;

import java.util.function.IntConsumer;

/** 设置页中的“下载设置”。 */
final class DownloadSettingsView extends VBox {
    private final CamlSlider concurrencySlider;
    private final Label concurrencyValue;
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(300));

    DownloadSettingsView(int initialConcurrency, IntConsumer onConcurrencyChanged) {
        int initial = Math.max(
                SettingsService.MIN_DOWNLOAD_CONCURRENCY,
                Math.min(SettingsService.MAX_DOWNLOAD_CONCURRENCY, initialConcurrency)
        );

        concurrencySlider = new CamlSlider(
                SettingsService.MIN_DOWNLOAD_CONCURRENCY,
                SettingsService.MAX_DOWNLOAD_CONCURRENCY,
                initial
        );
        concurrencySlider.setMaxWidth(Double.MAX_VALUE);
        concurrencySlider.setAccessibleText("下载并发数");

        concurrencyValue = new Label();
        concurrencyValue.getStyleClass().add("app-download-concurrency-value");
        concurrencyValue.setAlignment(Pos.CENTER);
        concurrencyValue.setMaxWidth(Double.MAX_VALUE);
        updateValue(initial);

        saveDebounce.setOnFinished(event ->
                onConcurrencyChanged.accept(concurrencySlider.getValue()));
        concurrencySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int value = newValue.intValue();
            updateValue(value);
            saveDebounce.playFromStart();
        });

        buildLayout();
    }

    private void buildLayout() {
        Label heading = new Label("设定下载并发数");
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label("同时下载的文件数量，范围 1–256。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label sliderLabel = new Label("下载并发数");
        sliderLabel.getStyleClass().add("caml-label-medium");
        sliderLabel.setLabelFor(concurrencySlider);

        VBox sliderBlock = new VBox(10, sliderLabel, concurrencyValue, concurrencySlider);

        VBox group = new VBox(16, heading, description, sliderBlock);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().add(group);
    }

    private void updateValue(int value) {
        concurrencyValue.setText("当前并发数：" + value);
    }
}
