package org.chs.camlauncher.front.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.chs.camlauncher.backend.runtime.JavaCandidate;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlRadioButton;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 设置页中的“Java”分组：自动选择或手动选择 Java。 */
final class JavaSettingsView extends VBox {
    private final LaunchSettings launchSettings;
    private final SettingsController controller;
    private final CamlRadioButton automatic = new CamlRadioButton("自动选择");
    private final CamlRadioButton manual = new CamlRadioButton("手动选择");
    private final CamlComboBox<JavaCandidate> javaBox = new CamlComboBox<>();
    private final CamlButton rescanButton = CamlButton.outlined("重新扫描");
    private final CamlButton addButton = CamlButton.outlined("添加 Java");
    private final Label detailLabel = new Label();
    private boolean scanning;
    private boolean updatingMode;

    JavaSettingsView(LaunchSettings launchSettings, SettingsController controller) {
        this.launchSettings = launchSettings;
        this.controller = controller;

        Label heading = new Label("Java");
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label("选择游戏启动时使用的 Java 运行时。自动选择会按游戏版本要求挑选可用 Java。");
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        ToggleGroup modeGroup = new ToggleGroup();
        automatic.setToggleGroup(modeGroup);
        manual.setToggleGroup(modeGroup);
        automatic.setSelected(launchSettings.javaSelectionMode() != JavaSelectionMode.MANUAL);
        manual.setSelected(launchSettings.javaSelectionMode() == JavaSelectionMode.MANUAL);
        HBox modeRow = new HBox(18, automatic, manual);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        rescanButton.getStyleClass().add("app-java-tool-button");
        addButton.getStyleClass().add("app-java-tool-button");
        rescanButton.setOnAction(event -> scanJava());
        addButton.setOnAction(event -> chooseJavaFile());
        HBox tools = new HBox(10, rescanButton, addButton);
        tools.setAlignment(Pos.CENTER_LEFT);

        javaBox.setMaxWidth(Double.MAX_VALUE);
        javaBox.setPromptText("正在扫描 Java...");
        javaBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(JavaCandidate candidate) {
                return candidate == null ? "" : candidate.executable().toString();
            }

            @Override
            public JavaCandidate fromString(String value) {
                if (value == null) {
                    return null;
                }
                return javaBox.getItems().stream()
                        .filter(candidate -> candidate.executable().toString().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        javaBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, candidate) -> {
            if (candidate == null) {
                updateDetail();
                return;
            }
            launchSettings.setSelectedJavaPath(candidate.executable());
            updateDetail();
            if (manual.isSelected()) {
                saveSelection();
            }
        });

        detailLabel.getStyleClass().addAll("caml-body-small", "field-hint");
        detailLabel.setWrapText(true);

        VBox manualBlock = new VBox(8, tools, javaBox, detailLabel);
        manualBlock.setMaxWidth(Double.MAX_VALUE);

        modeGroup.selectedToggleProperty().addListener((observable, oldValue, selected) -> {
            if (updatingMode || selected == null) {
                return;
            }
            applyMode(selected == manual, true);
        });

        setSpacing(16);
        setPadding(new Insets(24));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-group");
        getChildren().addAll(heading, description, modeRow, manualBlock);

        applyMode(launchSettings.javaSelectionMode() == JavaSelectionMode.MANUAL, false);
        scanJava();
    }

    private void scanJava() {
        if (scanning) {
            return;
        }
        scanning = true;
        rescanButton.setDisable(true);
        javaBox.setPromptText("正在扫描 Java...");
        detailLabel.setText("正在扫描整机 Java...");
        detailLabel.getStyleClass().remove("field-hint-error");
        controller.scanJavaCandidates(this::applyCandidates, message -> {
            scanning = false;
            rescanButton.setDisable(false);
            javaBox.setPromptText("扫描 Java 失败");
            detailLabel.setText("扫描 Java 失败：" + message);
            detailLabel.getStyleClass().remove("field-hint");
            detailLabel.getStyleClass().add("field-hint-error");
        });
    }

    private void applyCandidates(List<JavaCandidate> candidates) {
        scanning = false;
        rescanButton.setDisable(false);
        Path desired = launchSettings.selectedJavaPath();
        List<JavaCandidate> sorted = new ArrayList<>(candidates);
        sortCandidates(sorted);

        javaBox.getItems().setAll(sorted);
        javaBox.refreshDisplay();

        JavaCandidate selected = findByPath(sorted, desired);
        if (manual.isSelected()) {
            if (selected == null) {
                selected = sorted.stream()
                        .filter(JavaCandidate::available)
                        .findFirst()
                        .orElse(null);
            }
            if (selected != null) {
                javaBox.setValue(selected);
                launchSettings.setSelectedJavaPath(selected.executable());
                saveSelection();
            } else {
                javaBox.setValue(null);
                javaBox.setPromptText(sorted.isEmpty() ? "没有发现 Java" : "请选择 Java");
                javaBox.refreshDisplay();
            }
        } else {
            javaBox.setValue(null);
            javaBox.setPromptText("自动选择");
            javaBox.refreshDisplay();
        }
        javaBox.setDisable(!manual.isSelected());
        updateDetail();
    }

    private void chooseJavaFile() {
        if (getScene() == null) {
            return;
        }
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("选择 Java 可执行文件");
        Path current = launchSettings.selectedJavaPath();
        if (current != null && current.getParent() != null && current.getParent().toFile().isDirectory()) {
            chooser.setInitialDirectory(current.getParent().toFile());
        }
        File selected = chooser.showOpenDialog(getScene().getWindow());
        if (selected == null) {
            return;
        }

        addButton.setDisable(true);
        detailLabel.setText("正在检查 Java...");
        detailLabel.getStyleClass().remove("field-hint-error");
        controller.addManualJava(selected.toPath(), candidate -> {
            addButton.setDisable(false);
            List<JavaCandidate> currentItems = new ArrayList<>(javaBox.getItems());
            currentItems.removeIf(item -> samePath(item.executable(), candidate.executable()));
            currentItems.add(candidate);
            sortCandidates(currentItems);
            javaBox.getItems().setAll(currentItems);
            javaBox.refreshDisplay();
            if (!manual.isSelected()) {
                updatingMode = true;
                manual.setSelected(true);
                updatingMode = false;
            }
            applyMode(true, false);
            javaBox.setValue(candidate);
            launchSettings.setSelectedJavaPath(candidate.executable());
            saveSelection();
            updateDetail();
        }, message -> {
            addButton.setDisable(false);
            detailLabel.setText("添加 Java 失败：" + message);
            detailLabel.getStyleClass().remove("field-hint");
            detailLabel.getStyleClass().add("field-hint-error");
        });
    }

    private void applyMode(boolean manualMode, boolean persist) {
        launchSettings.setJavaSelectionMode(
                manualMode ? JavaSelectionMode.MANUAL : JavaSelectionMode.AUTO);
        javaBox.setDisable(!manualMode);
        if (manualMode) {
            if (javaBox.getValue() == null) {
                JavaCandidate saved = findByPath(javaBox.getItems(), launchSettings.selectedJavaPath());
                if (saved == null) {
                    saved = javaBox.getItems().stream()
                            .filter(JavaCandidate::available)
                            .findFirst()
                            .orElse(null);
                }
                if (saved != null) {
                    javaBox.setValue(saved);
                    launchSettings.setSelectedJavaPath(saved.executable());
                }
            }
        } else {
            javaBox.setValue(null);
            javaBox.setPromptText("自动选择");
            javaBox.refreshDisplay();
        }
        updateDetail();
        if (persist) {
            saveSelection();
        }
    }

    private void saveSelection() {
        controller.saveJavaSelectionAsync(
                launchSettings.javaSelectionMode(),
                launchSettings.selectedJavaPath()
        );
    }

    private void updateDetail() {
        JavaCandidate selected = javaBox.getValue();
        detailLabel.getStyleClass().removeAll("field-hint", "field-hint-error", "app-java-detail");
        detailLabel.getStyleClass().add("app-java-detail");
        if (!manual.isSelected()) {
            detailLabel.setText("自动选择会从扫描结果中挑选满足当前游戏版本要求的 Java。");
            return;
        }
        if (selected == null) {
            detailLabel.setText("请选择一个 Java，或点击“添加 Java”。");
            return;
        }
        if (!selected.available()) {
            detailLabel.setText("该 Java 当前不可用：" + selected.executable());
            detailLabel.getStyleClass().add("field-hint-error");
            return;
        }
        detailLabel.setText(String.format(
                "版本 %s · %s · %s · %d 位%s",
                selected.version(),
                selected.vendor(),
                selected.architecture(),
                selected.bits(),
                selected.jdk() ? " · JDK" : " · JRE"
        ));
        detailLabel.getStyleClass().add("field-hint");
    }

    private static JavaCandidate findByPath(List<JavaCandidate> candidates, Path path) {
        if (path == null) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> samePath(candidate.executable(), path))
                .findFirst()
                .orElse(null);
    }

    private static boolean samePath(Path left, Path right) {
        return left != null && right != null
                && left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private static void sortCandidates(List<JavaCandidate> candidates) {
        candidates.sort(Comparator
                .comparing(JavaCandidate::available).reversed()
                .thenComparing(Comparator.comparingInt(JavaCandidate::majorVersion).reversed())
                .thenComparing(candidate -> candidate.executable().toString()));
    }
}
