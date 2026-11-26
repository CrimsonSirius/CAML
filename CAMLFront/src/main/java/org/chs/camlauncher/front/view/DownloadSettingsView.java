package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlRadioButton;
import org.chs.camlauncher.ui.control.CamlButton;
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
    private final CamlTextField archiveEntriesField;
    private final CamlTextField archiveEntrySizeField;
    private final CamlTextField archiveTotalSizeField;
    private final Label archiveStatus = new Label();
    private final IntConsumer onConcurrencyChanged;
    private final BiConsumer<DownloadSourceMode, String> onSourceChanged;
    private final ArchiveLimitsConsumer onArchiveLimitsChanged;
    private boolean concurrencySavePending;
    private boolean apiSavePending;
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(300));
    private final PauseTransition apiDebounce = new PauseTransition(Duration.millis(500));

    DownloadSettingsView(
            int initialConcurrency,
            IntConsumer onConcurrencyChanged,
            DownloadSourceMode initialSource,
            String initialCustomApi,
            BiConsumer<DownloadSourceMode, String> onSourceChanged,
            int initialArchiveMaxEntries,
            long initialArchiveMaxEntryMiB,
            long initialArchiveMaxTotalMiB,
            ArchiveLimitsConsumer onArchiveLimitsChanged
    ) {
        this.onConcurrencyChanged = onConcurrencyChanged;
        this.onSourceChanged = onSourceChanged;
        this.onArchiveLimitsChanged = onArchiveLimitsChanged;
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
        concurrencySlider.setAccessibleText(UiText.tr("download.settings.concurrency.accessible"));

        concurrencyValue = new Label();
        concurrencyValue.getStyleClass().add("app-download-concurrency-value");
        concurrencyValue.setAlignment(Pos.CENTER);
        concurrencyValue.setMaxWidth(Double.MAX_VALUE);
        updateValue(initial);

        saveDebounce.setOnFinished(event -> {
            concurrencySavePending = false;
            onConcurrencyChanged.accept(concurrencySlider.getValue());
        });
        concurrencySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int value = newValue.intValue();
            updateValue(value);
            concurrencySavePending = true;
            saveDebounce.playFromStart();
        });

        ToggleGroup sourceGroup = new ToggleGroup();
        mirrorSource = new CamlRadioButton(UiText.tr("download.settings.source.mirror"));
        officialSource = new CamlRadioButton(UiText.tr("download.settings.source.official"));
        customSource = new CamlRadioButton(UiText.tr("download.settings.source.custom"));
        mirrorSource.setToggleGroup(sourceGroup);
        officialSource.setToggleGroup(sourceGroup);
        customSource.setToggleGroup(sourceGroup);
        mirrorSource.setSelected(initialSource == DownloadSourceMode.MIRROR);
        officialSource.setSelected(initialSource == DownloadSourceMode.OFFICIAL);
        customSource.setSelected(initialSource == DownloadSourceMode.CUSTOM);

        customApiField = new CamlTextField(
                initialCustomApi == null ? "" : initialCustomApi,
                UiText.tr("download.settings.customApi.placeholder")
        );
        customApiField.setMaxWidth(Double.MAX_VALUE);
        customApiField.setDisable(initialSource != DownloadSourceMode.CUSTOM);
        archiveEntriesField = numericField(Integer.toString(initialArchiveMaxEntries));
        archiveEntrySizeField = numericField(Long.toString(initialArchiveMaxEntryMiB));
        archiveTotalSizeField = numericField(Long.toString(initialArchiveMaxTotalMiB));

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
            apiSavePending = false;
            if (customSource.isSelected()) {
                saveSource(DownloadSourceMode.CUSTOM);
            }
        });
        customApiField.textProperty().addListener((observable, oldValue, newValue) -> {
            apiSavePending = true;
            apiDebounce.playFromStart();
        });
        customApiField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused && customSource.isSelected()) {
                saveSource(DownloadSourceMode.CUSTOM);
            }
        });

        buildLayout();
    }

    private void saveSource(DownloadSourceMode mode) {
        apiSavePending = false;
        apiDebounce.stop();
        onSourceChanged.accept(mode, customApiField.getText().trim());
    }

    /** 关闭前立即提交仍在防抖窗口内的并发数和自定义 API 修改。 */
    void flushPendingSaves() {
        if (concurrencySavePending) {
            concurrencySavePending = false;
            saveDebounce.stop();
            onConcurrencyChanged.accept(concurrencySlider.getValue());
        }
        if (apiSavePending) {
            apiSavePending = false;
            apiDebounce.stop();
            if (customSource.isSelected()) {
                saveSource(DownloadSourceMode.CUSTOM);
            }
        }
    }

    private void buildLayout() {
        Label sourceHeading = new Label(UiText.tr("download.settings.source"));
        sourceHeading.getStyleClass().add("app-settings-group-title");

        Label sourceDescription = new Label(UiText.tr("download.settings.source.description"));
        sourceDescription.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        sourceDescription.setWrapText(true);

        VBox sourceOptions = new VBox(mirrorSource, officialSource, customSource);
        sourceOptions.getStyleClass().add("app-settings-options");
        VBox sourceGroup = new VBox(sourceHeading, sourceDescription, sourceOptions, customApiField);
        sourceGroup.setMaxWidth(Double.MAX_VALUE);
        sourceGroup.getStyleClass().add("app-settings-group");

        Label heading = new Label(UiText.tr("download.settings.concurrency"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("download.settings.concurrency.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        Label sliderLabel = new Label(UiText.tr("download.settings.concurrency.label"));
        sliderLabel.getStyleClass().add("caml-label-medium");
        sliderLabel.setLabelFor(concurrencySlider);

        VBox sliderBlock = new VBox(sliderLabel, concurrencyValue, concurrencySlider);
        sliderBlock.getStyleClass().add("app-settings-block");

        VBox concurrencyGroup = new VBox(heading, description, sliderBlock);
        concurrencyGroup.getStyleClass().add("app-settings-group");
        concurrencyGroup.setMaxWidth(Double.MAX_VALUE);

        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-content");
        getChildren().addAll(sourceGroup, concurrencyGroup, buildArchiveGroup());
    }

    private VBox buildArchiveGroup() {
        Label heading = new Label(UiText.tr("download.settings.archive.title"));
        heading.getStyleClass().add("app-settings-group-title");
        Label description = new Label(UiText.tr("download.settings.archive.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        VBox group = new VBox(
                heading,
                description,
                archiveRow(UiText.tr("download.settings.archive.entries"),
                        archiveEntriesField, "archive-max-entries",
                        SettingsService.DEFAULT_ARCHIVE_MAX_ENTRIES),
                archiveRow(UiText.tr("download.settings.archive.entrySize"),
                        archiveEntrySizeField, "archive-max-entry-mib",
                        SettingsService.DEFAULT_ARCHIVE_MAX_ENTRY_MIB),
                archiveRow(UiText.tr("download.settings.archive.totalSize"),
                        archiveTotalSizeField, "archive-max-total-mib",
                        SettingsService.DEFAULT_ARCHIVE_MAX_TOTAL_MIB)
        );
        archiveStatus.setId("archive-limits-status");
        archiveStatus.getStyleClass().addAll("caml-body-small", "field-hint");
        archiveStatus.setWrapText(true);
        archiveStatus.setVisible(false);
        archiveStatus.setManaged(false);
        CamlButton save = CamlButton.filled(UiText.tr("common.save"));
        save.setId("archive-limits-save");
        save.setOnAction(event -> saveArchiveLimits());
        HBox actions = new HBox(save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("app-settings-actions");
        group.getChildren().addAll(archiveStatus, actions);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);
        return group;
    }

    private HBox archiveRow(String labelText, CamlTextField field, String id, long defaultValue) {
        Label label = new Label(labelText);
        label.getStyleClass().add("caml-label-medium");
        label.setMinWidth(Region.USE_PREF_SIZE);
        field.setId(id);
        field.setMaxWidth(260);
        CamlButton reset = CamlButton.outlined(UiText.tr("common.reset"));
        reset.setId(id + "-reset");
        reset.setOnAction(event -> {
            field.setText(Long.toString(defaultValue));
            setArchiveStatus("", false);
        });
        HBox row = new HBox(label, field, reset);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("app-settings-block");
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private void saveArchiveLimits() {
        try {
            int entries = Integer.parseInt(archiveEntriesField.getText().trim());
            long entryMiB = Long.parseLong(archiveEntrySizeField.getText().trim());
            long totalMiB = Long.parseLong(archiveTotalSizeField.getText().trim());
            if (entries < SettingsService.MIN_ARCHIVE_MAX_ENTRIES
                    || entries > SettingsService.MAX_ARCHIVE_MAX_ENTRIES
                    || entryMiB < SettingsService.MIN_ARCHIVE_SIZE_MIB
                    || entryMiB > SettingsService.MAX_ARCHIVE_SIZE_MIB
                    || totalMiB < entryMiB
                    || totalMiB > SettingsService.MAX_ARCHIVE_SIZE_MIB) {
                throw new NumberFormatException();
            }
            onArchiveLimitsChanged.accept(entries, entryMiB, totalMiB);
            setArchiveStatus(UiText.tr("download.settings.archive.saved"), false);
        } catch (NumberFormatException e) {
            setArchiveStatus(UiText.tr("download.settings.archive.invalid"), true);
        }
    }

    private void setArchiveStatus(String text, boolean error) {
        archiveStatus.setText(text);
        archiveStatus.setVisible(!text.isBlank());
        archiveStatus.setManaged(!text.isBlank());
        archiveStatus.getStyleClass().removeAll("field-hint", "field-hint-error");
        archiveStatus.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }

    private static CamlTextField numericField(String value) {
        CamlTextField field = new CamlTextField(value, "");
        field.getStyleClass().add("app-number-field");
        return field;
    }

    @FunctionalInterface
    interface ArchiveLimitsConsumer {
        void accept(int maxEntries, long maxEntryMiB, long maxTotalMiB);
    }

    private void updateValue(int value) {
        concurrencyValue.setText(UiText.tr("download.settings.concurrency.value", value));
    }
}
