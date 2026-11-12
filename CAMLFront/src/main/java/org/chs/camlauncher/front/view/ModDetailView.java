package org.chs.camlauncher.front.view;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.mod.model.ModDependency;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 单个 Mod 的详情、依赖和版本下载页面。 */
final class ModDetailView extends VBox implements PageLifecycle {
    private final DownloadsController controller;
    private final LaunchSettings launchSettings;
    private final ModProjectSummary summary;
    private final Runnable onBack;
    private final Label statusLabel = new Label(UiText.tr("mods.detail.loading"));
    private final Label titleLabel = new Label();
    private final Label metaLabel = new Label();
    private final Label summaryLabel = new Label();
    private final CamlComboBox<ModChoice<String>> versionBox = new CamlComboBox<>();
    private final CamlComboBox<ModChoice<String>> loaderBox = new CamlComboBox<>();
    private final VBox headerCard;
    private final VBox filterHost = new VBox();
    private final VBox groupsBox = new VBox();
    private final ScrollPane scroll = new ScrollPane(groupsBox);
    private ModDownloadLayout.Plan appliedPlan;
    private boolean disposed;
    private FrontTask<ModProjectDetail> loadTask;
    private List<ModFile> files = List.of();

    ModDetailView(DownloadsController controller, LaunchSettings launchSettings,
                  ModProjectSummary summary, Runnable onBack) {
        this.controller = controller;
        this.launchSettings = launchSettings;
        this.summary = summary;
        this.onBack = onBack;
        getStyleClass().add("app-download-content");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton back = ViewSupport.backButton(onBack);
        titleLabel.getStyleClass().add("app-mod-detail-title");
        titleLabel.setText(summary.name());
        titleLabel.setMinWidth(0);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        Label source = new Label(ModViewSupport.sourceLabel(summary.source()));
        source.getStyleClass().add("app-mod-source");
        source.setMinWidth(Region.USE_PREF_SIZE);
        HBox titleRow = new HBox(12, back, titleLabel, source);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        summaryLabel.setText(summary.summary());
        summaryLabel.getStyleClass().add("app-mod-summary");
        summaryLabel.setWrapText(true);
        metaLabel.setText(UiText.tr("mods.detail.meta",
                ModViewSupport.downloads(summary.downloads()),
                ViewSupport.releaseDate(summary.updatedAt())));
        metaLabel.getStyleClass().add("app-mod-meta");

        headerCard = new VBox(titleRow, summaryLabel, metaLabel);
        headerCard.getStyleClass().addAll("app-download-card", "app-mod-detail-header");
        headerCard.setMaxWidth(Double.MAX_VALUE);

        versionBox.setMaxWidth(Double.MAX_VALUE);
        loaderBox.setMaxWidth(Double.MAX_VALUE);
        versionBox.valueProperty().addListener((observable, oldValue, newValue) -> rebuildGroups());
        loaderBox.valueProperty().addListener((observable, oldValue, newValue) -> rebuildGroups());

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint", "app-mod-status");
        statusLabel.setWrapText(true);

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("app-instance-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(headerCard, filterHost, statusLabel, scroll);

        // 首帧宽度尚未确定，先按 1080p 宽屏规则摆放；宽度可用后按断点校正。
        applyPlan(ModDownloadLayout.WIDE);
        widthProperty().addListener((observable, oldValue, newValue) ->
                updateLayout(newValue.doubleValue()));
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateLayout(getWidth());
            }
        });

        loadProject();
    }

    private void updateLayout(double width) {
        if (width <= 0) {
            return;
        }
        applyPlan(ModDownloadLayout.plan(width));
    }

    private void applyPlan(ModDownloadLayout.Plan plan) {
        if (plan.equals(appliedPlan)) {
            return;
        }
        appliedPlan = plan;

        setPadding(new Insets(plan.contentPaddingVertical(), plan.contentPaddingHorizontal(),
                plan.contentPaddingVertical(), plan.contentPaddingHorizontal()));
        setSpacing(plan.sectionSpacing());
        headerCard.setPadding(new Insets(plan.cardPadding()));
        headerCard.setSpacing(plan.cardSpacing());
        filterHost.setSpacing(plan.formGap());
        filterHost.getChildren().setAll(buildFilterLayout(plan));
        groupsBox.setSpacing(plan.listSpacing());
    }

    private Pane buildFilterLayout(ModDownloadLayout.Plan plan) {
        return switch (plan.mode()) {
            case INLINE -> ModViewSupport.weightedRow(plan,
                    ModViewSupport.inlineField(plan, "mods.detail.versionFilter", versionBox),
                    ModViewSupport.inlineField(plan, "mods.detail.loaderFilter", loaderBox));
            case GRID -> {
                GridPane grid = ModViewSupport.evenGrid(plan, 2);
                grid.add(ModViewSupport.stackedField("mods.detail.versionFilter", versionBox), 0, 0);
                grid.add(ModViewSupport.stackedField("mods.detail.loaderFilter", loaderBox), 1, 0);
                yield grid;
            }
            case STACKED -> new VBox(plan.formGap(),
                    ModViewSupport.stackedField("mods.detail.versionFilter", versionBox),
                    ModViewSupport.stackedField("mods.detail.loaderFilter", loaderBox));
        };
    }

    private void loadProject() {
        loadTask = controller.fetchModProject(summary.source(), summary.projectId(),
                this::showProject, this::showError);
    }

    private void showProject(ModProjectDetail project) {
        if (disposed) {
            return;
        }
        files = project.files();
        populateFilters();
        rebuildGroups();
        setStatus(UiText.tr("mods.detail.count", files.size()), false);
    }

    private void populateFilters() {
        Set<String> versions = new LinkedHashSet<>();
        Set<String> loaders = new LinkedHashSet<>();
        for (ModFile file : files) {
            versions.addAll(file.gameVersions());
            loaders.addAll(file.loaders());
        }
        List<String> sortedVersions = new ArrayList<>(versions);
        sortedVersions.sort(Comparator.reverseOrder());
        List<String> sortedLoaders = new ArrayList<>(loaders);
        sortedLoaders.sort(String::compareTo);

        versionBox.getItems().setAll(new ModChoice<>(UiText.tr("mods.detail.allVersions"), null));
        sortedVersions.forEach(version -> versionBox.getItems().add(
                new ModChoice<>(version, version)));
        versionBox.setValue(versionBox.getItems().getFirst());

        loaderBox.getItems().setAll(new ModChoice<>(UiText.tr("mods.detail.allLoaders"), null));
        sortedLoaders.forEach(loader -> loaderBox.getItems().add(
                new ModChoice<>(ModViewSupport.loaderLabel(loader), loader)));
        loaderBox.setValue(loaderBox.getItems().getFirst());
    }

    private void rebuildGroups() {
        if (disposed) {
            return;
        }
        String versionFilter = selectedValue(versionBox);
        String loaderFilter = selectedValue(loaderBox);
        Map<String, List<ModFile>> groups = new LinkedHashMap<>();
        for (ModFile file : files) {
            List<String> versions = file.gameVersions().isEmpty()
                    ? List.of(UiText.tr("mods.value.unknown"))
                    : file.gameVersions();
            List<String> loaders = file.loaders().isEmpty()
                    ? List.of(UiText.tr("mods.value.unknown"))
                    : file.loaders();
            for (String version : versions) {
                if (versionFilter != null && !versionFilter.equals(version)) {
                    continue;
                }
                for (String loader : loaders) {
                    if (loaderFilter != null && !loaderFilter.equals(loader)) {
                        continue;
                    }
                    String key = ModViewSupport.loaderLabel(loader) + " " + version;
                    groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(file);
                }
            }
        }
        groupsBox.getChildren().clear();
        if (groups.isEmpty()) {
            groupsBox.getChildren().add(new Label(UiText.tr("mods.detail.empty")));
            return;
        }
        groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .forEach(entry -> {
                    groupsBox.getChildren().add(groupCard(entry.getKey(), entry.getValue()));
                });
    }

    private VBox groupCard(String title, List<ModFile> groupFiles) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("app-mod-group-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label arrow = new Label("▶");
        arrow.getStyleClass().add("app-mod-group-arrow");
        HBox headerGraphic = new HBox(12, titleLabel, spacer, arrow);
        headerGraphic.setAlignment(Pos.CENTER_LEFT);

        CamlButton header = CamlButton.outlined("");
        header.getStyleClass().add("app-mod-group-header");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        headerGraphic.prefWidthProperty().bind(
                Bindings.max(0, header.widthProperty().subtract(52)));

        VBox content = new VBox(10);
        content.getStyleClass().add("app-mod-group-content");
        content.setVisible(false);
        content.setManaged(false);

        boolean[] expanded = {false};
        header.setOnAction(event -> {
            expanded[0] = !expanded[0];
            if (expanded[0] && content.getChildren().isEmpty()) {
                for (ModFile file : groupFiles) {
                    content.getChildren().add(versionRow(file));
                }
            }
            content.setManaged(expanded[0]);
            content.setVisible(expanded[0]);
            arrow.setText(expanded[0] ? "▼" : "▶");
            if (expanded[0]) {
                FadeTransition fade = new FadeTransition(Duration.millis(120), content);
                fade.setFromValue(0.25);
                fade.setToValue(1);
                fade.play();
            }
        });

        VBox card = new VBox(header, content);
        card.getStyleClass().add("app-mod-group");
        return card;
    }

    private VBox versionRow(ModFile file) {
        Label title = new Label(file.name() + " · " + file.versionNumber());
        title.getStyleClass().add("app-mod-version-title");
        title.setWrapText(true);
        Label meta = new Label(UiText.tr("mods.detail.versionMeta",
                ViewSupport.releaseDate(file.publishedAt()),
                ModViewSupport.downloads(file.downloads()),
                file.fileName()));
        meta.getStyleClass().add("app-mod-meta");
        meta.setWrapText(true);
        Label dependencies = new Label(dependencyText(file.dependencies()));
        dependencies.getStyleClass().add("app-mod-dependency");
        dependencies.setWrapText(true);
        HBox top = new HBox(12, title, meta);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        CamlButton download = CamlButton.filled(UiText.tr("mods.detail.download"));
        download.setMinWidth(plan().paginationButtonWidth());
        download.setDisable(file.downloadUri() == null);
        download.setOnAction(event -> startDownload(file, download));
        HBox actions = new HBox(12, dependencies, download);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dependencies, Priority.ALWAYS);

        VBox row = new VBox(8, top, actions);
        row.getStyleClass().add("app-mod-version-row");
        return row;
    }

    private ModDownloadLayout.Plan plan() {
        return appliedPlan == null ? ModDownloadLayout.WIDE : appliedPlan;
    }

    private void startDownload(ModFile file, CamlButton button) {
        String instance = launchSettings.selectedVersion();
        if (launchSettings.gameDirectory() == null) {
            setStatus(UiText.tr("mods.detail.noGameDirectory"), true);
            return;
        }
        if (instance == null || instance.isBlank()) {
            setStatus(UiText.tr("mods.detail.noInstance"), true);
            return;
        }
        button.setDisable(true);
        setStatus(UiText.tr("mods.detail.downloading", file.name()), false);
        controller.downloadMod(
                file,
                launchSettings.gameDirectory(),
                instance,
                launchSettings.fullVersionIsolation(),
                DownloadListener.NONE,
                result -> {
                    button.setDisable(false);
                    setStatus(UiText.tr("mods.detail.downloaded", result.path()), false);
                },
                message -> {
                    button.setDisable(false);
                    setStatus(UiText.tr("mods.search.failed", message), true);
                }
        );
    }

    private static String dependencyText(List<ModDependency> dependencies) {
        if (dependencies.isEmpty()) {
            return UiText.tr("mods.detail.noDependencies");
        }
        return UiText.tr("mods.detail.dependencies") + " "
                + dependencies.stream()
                .map(dependency -> dependency.name() + " ("
                        + ModViewSupport.dependencyType(dependency.type()) + ")")
                .collect(Collectors.joining(", "));
    }

    private static String selectedValue(CamlComboBox<ModChoice<String>> box) {
        return box.getValue() == null ? null : box.getValue().value();
    }

    private void showError(String message) {
        if (disposed) {
            return;
        }
        setStatus(UiText.tr("mods.detail.failed", message), true);
    }

    private void setStatus(String text, boolean error) {
        statusLabel.getStyleClass().removeAll("field-hint", "field-hint-error");
        statusLabel.getStyleClass().add(error ? "field-hint-error" : "field-hint");
        statusLabel.setText(text);
    }

    @Override
    public void dispose() {
        disposed = true;
        if (loadTask != null) {
            loadTask.cancel();
            loadTask = null;
        }
    }
}