package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.ui.control.CamlRadioButton;
import org.chs.camlauncher.ui.control.CamlSlider;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/** 设置页中的“下载设置”。 */
final class DownloadSettingsView extends VBox {
    private final CamlSlider concurrencySlider;
    private final Label concurrencyValue;
    private final CamlRadioButton mirrorSource;
    private final CamlRadioButton officialSource;
    private final CamlRadioButton customSource;
    private final CamlTextField customApiField;
    private final BiConsumer<DownloadSourceMode, String> onSourceChanged;
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(300));
    private final PauseTransition apiDebounce = new PauseTransition(Duration.millis(500));

    DownloadSettingsView(
            int initialConcurrency,
            IntConsumer onConcurrencyChanged,
            DownloadSourceMode initialSource,
            String initialCustomApi,
            BiConsumer<DownloadSourceMode, String> onSourceChanged
    ) {
        this.onSourceChanged = onSourceChanged;
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

        ToggleGroup sourceGroup = new ToggleGroup();
        mirrorSource = new CamlRadioButton("尽可能使用镜像源");
        officialSource = new CamlRadioButton("尽可能使用官方源");
        customSource = new CamlRadioButton("导入自定义 API");
        mirrorSource.setToggleGroup(sourceGroup);
        officialSource.setToggleGroup(sourceGroup);
        customSource.setToggleGroup(sourceGroup);
        mirrorSource.setSelected(initialSource == DownloadSourceMode.MIRROR);
        officialSource.setSelected(initialSource == DownloadSourceMode.OFFICIAL);
        customSource.setSelected(initialSource == DownloadSourceMode.CUSTOM);

        customApiField = new CamlTextField(
                initialCustomApi == null ? "" : initialCustomApi,
                "例如 https://bmclapi2.bangbang93.com"
        );
        customApiField.setMaxWidth(Double.MAX_VALUE);
        customApiField.setDisable(initialSource != DownloadSourceMode.CUSTOM);

        sourceGroup.selectedToggleProperty().addListener((observable, oldValue, selected) -> {
            if (selected == null) {
                return;
            }
            DownloadSourceMode mode = selected == mirrorSource
                    ? DownloadSourceMode.MIRROR
                    : selected == officialSource
                    ? DownloadSourceMode.OFFICIAL
                    : DownloadSourceMode.CUSTOM;
            customApiField.setDisable(mode != DownloadSourceMode.CUSTOM);
            saveSource(mode);
        });
        apiDebounce.setOnFinished(event -> {
            if (customSource.isSelected()) {
                saveSource(DownloadSourceMode.CUSTOM);
            }
        });
        customApiField.textProperty().addListener((observable, oldValue, newValue) ->
                apiDebounce.playFromStart());
        customApiField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused && customSource.isSelected()) {
                saveSource(DownloadSourceMode.CUSTOM);
            }
        });

        buildLayout();
    }

    private void saveSource(DownloadSourceMode mode) {
        apiDebounce.stop();
        onSourceChanged.accept(mode, customApiField.getText().trim());
    }

    private void buildLayout() {
        Label sourceHeading = new Label("下载源");
        sourceHeading.getStyleClass().add("app-settings-group-title");

        Label sourceDescription = new Label("选择版本、库和资源使用的优先下载地址。");
        sourceDescription.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        sourceDescription.setWrapText(true);

        VBox sourceOptions = new VBox(12, mirrorSource, officialSource, customSource);
        VBox sourceGroup = new VBox(16, sourceHeading, sourceDescription, sourceOptions, customApiField);
        sourceGroup.setSpacing(16);
        sourceGroup.setPadding(new Insets(24));
        sourceGroup.setMaxWidth(Double.MAX_VALUE);
        sourceGroup.getStyleClass().add("app-settings-group");

        Label heading = new Label("设定下载并发数");
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label("同时下载的文件数量，范围 1–256。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label sliderLabel = new Label("下载并发数");
        sliderLabel.getStyleClass().add("caml-label-medium");
        sliderLabel.setLabelFor(concurrencySlider);

        VBox sliderBlock = new VBox(10, sliderLabel, concurrencyValue, concurrencySlider);

        VBox concurrencyGroup = new VBox(16, heading, description, sliderBlock);
        concurrencyGroup.setPadding(new Insets(24));
        concurrencyGroup.getStyleClass().add("app-settings-group");
        concurrencyGroup.setMaxWidth(Double.MAX_VALUE);

        setSpacing(20);
        setMinWidth(0);
        setPrefWidth(560);
        setMaxWidth(560);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(sourceGroup, concurrencyGroup);
    }

    private void updateValue(int value) {
        concurrencyValue.setText("当前并发数：" + value);
    }
}
