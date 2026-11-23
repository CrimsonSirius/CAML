package org.chs.camlauncher.front.view;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.system.DeviceInfo;
import org.chs.camlauncher.backend.system.SystemInfo;
import org.chs.camlauncher.front.text.UiText;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** 设置页中的“关于”：启动器、启动器运行环境、系统、设备、目录和项目信息。 */
final class AboutSettingsView extends VBox {

    AboutSettingsView(BackendStatus status, SystemInfo systemInfo, DeviceInfo deviceInfo,
                      ObservableValue<Path> gameDirectory) {
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-content");

        Label gameDirectoryValue = valueLabel(pathText(gameDirectory.getValue()), true);
        gameDirectoryValue.textProperty().bind(Bindings.createStringBinding(
                () -> pathText(gameDirectory.getValue()), gameDirectory));

        getChildren().addAll(
                group("settings.about.application.title",
                        infoRow("settings.about.version", status.version()),
                        infoRow("settings.about.backend", UiText.tr(status.ready()
                                ? "footer.backend.ready"
                                : "footer.backend.notReady")),
                        infoRow("settings.about.license", "GPL-3.0-only")),
                group("settings.about.runtime.title",
                        infoRow("settings.about.javaVersion", systemInfo.javaVersion()),
                        infoRow("settings.about.javaVendor", systemInfo.javaVendor())),
                group("settings.about.system.title",
                        infoRow("settings.about.os", systemInfo.operatingSystem()),
                        infoRow(versionLabelKey(systemInfo), systemInfo.version()),
                        infoRow("settings.about.architecture", systemInfo.architecture())),
                group("settings.about.device.title",
                        infoRow("settings.about.cpu", deviceInfo.cpu()),
                        infoRow("settings.about.gpu", listText(deviceInfo.gpus())),
                        infoRow("settings.about.npu", listText(deviceInfo.npus())),
                        infoRow("settings.about.memory", memoryText(deviceInfo.totalMemoryMb()))),
                group("settings.about.directories.title",
                        pathRow("settings.about.dataDirectory", pathText(status.dataDirectory())),
                        row("settings.about.gameDirectory", gameDirectoryValue)),
                group("settings.about.project.title",
                        infoRow("settings.about.projectName", "CAML"),
                        infoRow("settings.about.copyright",
                                "Copyright (C) 2026 CrimsonSirius, namelessgod2008"))
        );
    }

    static String versionLabelKey(SystemInfo systemInfo) {
        return systemInfo.versionKind() == SystemInfo.VersionKind.KERNEL
                ? "settings.about.kernelVersion"
                : "settings.about.osVersion";
    }

    private static VBox group(String titleKey, Node... rows) {
        Label title = new Label(UiText.tr(titleKey));
        title.getStyleClass().add("app-settings-group-title");

        VBox group = new VBox();
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);
        group.getChildren().add(title);
        group.getChildren().addAll(rows);
        return group;
    }

    private static VBox infoRow(String labelKey, String value) {
        return row(labelKey, valueLabel(value, false));
    }

    private static VBox pathRow(String labelKey, String value) {
        return row(labelKey, valueLabel(value, true));
    }

    private static VBox row(String labelKey, Label value) {
        Label label = new Label(UiText.tr(labelKey));
        label.getStyleClass().addAll("caml-label-medium", "app-about-label");

        VBox row = new VBox(label, value);
        row.getStyleClass().add("app-about-row");
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static Label valueLabel(String value, boolean path) {
        Label label = new Label(value == null || value.isBlank()
                ? UiText.tr("settings.about.unknown")
                : value);
        label.getStyleClass().addAll("caml-body-medium", "app-about-value");
        if (path) {
            label.getStyleClass().add("app-about-path");
        }
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static String pathText(Path path) {
        return path == null ? UiText.tr("settings.about.unknown") : path.toString();
    }

    private static String listText(List<String> values) {
        return values == null || values.isEmpty()
                ? UiText.tr("settings.about.notDetected")
                : String.join("\n", values);
    }

    private static String memoryText(long totalMemoryMb) {
        return totalMemoryMb <= 0
                ? UiText.tr("settings.about.unknown")
                : String.format(Locale.ROOT, "%.1f GB", totalMemoryMb / 1024.0);
    }
}
