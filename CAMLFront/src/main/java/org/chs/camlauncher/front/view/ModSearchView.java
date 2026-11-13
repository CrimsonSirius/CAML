package org.chs.camlauncher.front.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 下载页中的 Mod 搜索与分页列表。
 *
 * <p>页面内容整体放在纵向滚动容器中：窗口高度不足以同时容纳搜索卡、状态、结果和分页时
 * 由整页滚动接管，结果行不会被裁切，也不需要压缩行高和字号。搜索表单方向由
 * {@link ModDownloadLayout} 按页面宽度切换。</p>
 *
 * <p>结果行由 {@link ModViewSupport#resultRow} 组装：下层透明 {@code CamlButton} 负责
 * 点击、悬停和焦点，上层只读正文负责显示，行高因此由图标与内边距决定，
 * 不会被按钮样式里的 {@code -fx-min-height} 压缩。</p>
 */
final class ModSearchView extends VBox implements PageLifecycle {
    private static final int PAGE_SIZE = 20;
    private static final int ICON_RETRY_LIMIT = 5;
    private static final long ICON_RETRY_DELAY_MILLIS = 5_000;
    private static final long ICON_FIRST_ATTEMPT_TIMEOUT_MILLIS = 8_000;
    private static final byte[] EMPTY_ICON = new byte[0];

    private final DownloadsController controller;
    private final Consumer<ModProjectSummary> onProjectSelected;
    private final CamlTextField queryField = new CamlTextField();
    private final CamlTextField gameVersionField = new CamlTextField();
    private final CamlComboBox<ModChoice<ModSource>> sourceBox = new CamlComboBox<>();
    private final CamlComboBox<ModChoice<ModLoader>> loaderBox = new CamlComboBox<>();
    private final CamlButton searchButton;
    private final CamlButton resetButton;
    private final Label statusLabel = new Label(UiText.tr("mods.search.loading"));
    private final VBox formHost = new VBox();
    private final VBox searchCard;
    private final VBox resultsBox = new VBox();
    private final Label pageLabel = new Label();
    private final CamlButton previousButton = CamlButton.outlined(UiText.tr("mods.page.previous"));
    private final CamlButton nextButton = CamlButton.outlined(UiText.tr("mods.page.next"));
    private final HBox pagination;
    private final VBox content = new VBox();
    private final ScrollPane pageScroll = new ScrollPane(content);
    private ModDownloadLayout.Plan appliedPlan;
    private List<ModProjectSummary> lastItems = List.of();
    private int pageIndex;
    private boolean hasNext;
    private boolean loading;
    private boolean disposed;
    private FrontTask<ModSearchPage> loadTask;
    private final List<FrontTask<byte[]>> iconTasks = new ArrayList<>();
    private final List<FrontTask<byte[]>> iconRetryTasks = new ArrayList<>();
    private final List<PauseTransition> iconTimeouts = new ArrayList<>();
    private long iconGeneration;

    ModSearchView(DownloadsController controller, Consumer<ModProjectSummary> onProjectSelected) {
        this.controller = controller;
        this.onProjectSelected = onProjectSelected;
        getStyleClass().add("app-download-content");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label title = new Label(UiText.tr("mods.search.title"));
        title.getStyleClass().add("app-mod-search-title");

        queryField.setPromptText(UiText.tr("mods.search.nameHint"));
        gameVersionField.setPromptText(UiText.tr("mods.search.versionHint"));
        sourceBox.getItems().setAll(
                new ModChoice<>(UiText.tr("mods.search.allSources"), null),
                new ModChoice<>(UiText.tr("mods.source.modrinth"), ModSource.MODRINTH),
                new ModChoice<>(UiText.tr("mods.source.curseforge"), ModSource.CURSEFORGE)
        );
        sourceBox.setValue(sourceBox.getItems().getFirst());
        loaderBox.getItems().setAll(
                new ModChoice<>(UiText.tr("mods.search.allLoaders"), null),
                new ModChoice<>(UiText.tr("mods.loader.fabric"), ModLoader.FABRIC),
                new ModChoice<>(UiText.tr("mods.loader.forge"), ModLoader.FORGE),
                new ModChoice<>(UiText.tr("mods.loader.neoforge"), ModLoader.NEOFORGE),
                new ModChoice<>(UiText.tr("mods.loader.quilt"), ModLoader.QUILT)
        );
        loaderBox.setValue(loaderBox.getItems().getFirst());

        searchButton = CamlButton.filled(UiText.tr("mods.search.button"));
        resetButton = CamlButton.outlined(UiText.tr("mods.search.reset"));
        searchButton.setOnAction(event -> searchFirstPage());
        resetButton.setOnAction(event -> reset());
        queryField.setOnAction(event -> searchFirstPage());
        gameVersionField.setOnAction(event -> searchFirstPage());
        HBox actions = new HBox(searchButton, resetButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("app-mod-search-actions");

        searchCard = new VBox(title, formHost, actions);
        searchCard.getStyleClass().addAll("app-download-card", "app-mod-search");
        searchCard.setMaxWidth(Double.MAX_VALUE);
        searchCard.setFillWidth(true);

        statusLabel.getStyleClass().addAll("caml-body-small", "field-hint", "app-mod-status");
        statusLabel.setWrapText(true);

        resultsBox.getStyleClass().add("app-mod-list");

        previousButton.setOnAction(event -> changePage(-1));
        nextButton.setOnAction(event -> changePage(1));
        pageLabel.getStyleClass().add("app-mod-page-info");
        pagination = new HBox(previousButton, pageLabel, nextButton);
        pagination.setAlignment(Pos.CENTER);
        pagination.getStyleClass().add("app-mod-pagination");

        content.getChildren().addAll(searchCard, statusLabel, resultsBox, pagination);
        content.getStyleClass().add("app-mod-page");
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        pageScroll.setContent(content);
        pageScroll.setFitToWidth(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.getStyleClass().add("app-instance-scroll");
        pageScroll.setMaxWidth(Double.MAX_VALUE);
        pageScroll.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(pageScroll, Priority.ALWAYS);
        getChildren().setAll(pageScroll);

        // 首帧宽度与缩放尚未确定，先按宽屏方向摆放；尺寸可用后按断点与缩放校正。
        applyPlan(ModDownloadLayout.WIDE);
        ViewSupport.onLayoutMetricsChanged(this, this::updateLayout);

        searchFirstPage();
    }

    private void updateLayout() {
        double width = getWidth();
        if (width <= 0) {
            return;
        }
        applyPlan(ModDownloadLayout.plan(width, ViewSupport.uiFactor(this)));
    }

    private void applyPlan(ModDownloadLayout.Plan plan) {
        if (plan.equals(appliedPlan)) {
            return;
        }
        boolean scaleChanged = appliedPlan != null && appliedPlan.factor() != plan.factor();
        appliedPlan = plan;

        content.setPadding(new Insets(plan.contentPaddingVertical(), plan.contentPaddingHorizontal(),
                plan.contentPaddingVertical(), plan.contentPaddingHorizontal()));
        content.setSpacing(plan.sectionSpacing());
        searchCard.setPadding(new Insets(plan.cardPadding()));
        searchCard.setSpacing(plan.cardSpacing());
        formHost.setSpacing(plan.formGap());
        formHost.getChildren().setAll(buildForm(plan));
        resultsBox.setSpacing(plan.listSpacing());
        pagination.setSpacing(plan.paginationSpacing());
        previousButton.setMinWidth(plan.paginationButtonWidth());
        nextButton.setMinWidth(plan.paginationButtonWidth());

        if (scaleChanged) {
            // 图标与行内边距在创建行时就写进了节点，缩放变化后需要重建，否则行高与文字会错位。
            rebuildRows();
        }
    }

    /** 按当前缩放重建结果行，不重新请求接口、不重置分页。 */
    private void rebuildRows() {
        if (lastItems.isEmpty()) {
            return;
        }
        cancelIconTasks();
        iconGeneration++;
        resultsBox.getChildren().clear();
        List<IconTarget> iconTargets = new ArrayList<>();
        for (ModProjectSummary project : lastItems) {
            resultsBox.getChildren().add(projectRow(project, iconTargets));
        }
        loadIcons(iconTargets, iconGeneration);
    }

    private Pane buildForm(ModDownloadLayout.Plan plan) {
        return switch (plan.mode()) {
            case INLINE -> {
                VBox rows = new VBox(plan.formGap(),
                        ModViewSupport.weightedRow(plan,
                                ModViewSupport.inlineField(plan, "mods.search.name", queryField),
                                ModViewSupport.inlineField(plan, "mods.search.source", sourceBox)),
                        ModViewSupport.weightedRow(plan,
                                ModViewSupport.inlineField(plan, "mods.search.gameVersion", gameVersionField),
                                ModViewSupport.inlineField(plan, "mods.search.loader", loaderBox)));
                rows.getStyleClass().add("app-mod-search-form");
                yield rows;
            }
            case GRID -> {
                GridPane grid = ModViewSupport.evenGrid(plan, 2);
                grid.add(ModViewSupport.stackedField("mods.search.name", queryField), 0, 0);
                grid.add(ModViewSupport.stackedField("mods.search.source", sourceBox), 1, 0);
                grid.add(ModViewSupport.stackedField("mods.search.gameVersion", gameVersionField), 0, 1);
                grid.add(ModViewSupport.stackedField("mods.search.loader", loaderBox), 1, 1);
                grid.getStyleClass().add("app-mod-search-form");
                yield grid;
            }
            case STACKED -> {
                VBox form = new VBox(plan.formGap(),
                        ModViewSupport.stackedField("mods.search.name", queryField),
                        ModViewSupport.stackedField("mods.search.source", sourceBox),
                        ModViewSupport.stackedField("mods.search.gameVersion", gameVersionField),
                        ModViewSupport.stackedField("mods.search.loader", loaderBox));
                form.getStyleClass().add("app-mod-search-form");
                yield form;
            }
        };
    }

    private void searchFirstPage() {
        pageIndex = 0;
        loadPage();
    }

    private void changePage(int delta) {
        if (loading || (delta > 0 && !hasNext) || (delta < 0 && pageIndex == 0)) {
            return;
        }
        pageIndex += delta;
        loadPage();
    }

    private void loadPage() {
        if (disposed || loading) {
            return;
        }
        if (loadTask != null) {
            loadTask.cancel();
        }
        cancelIconTasks();
        iconGeneration++;
        loading = true;
        setControlsDisabled(true);
        setStatus(UiText.tr("mods.search.loading"), false);
        ModSearchQuery query = new ModSearchQuery(
                queryField.getText(),
                sourceBox.getValue() == null ? null : sourceBox.getValue().value(),
                gameVersionField.getText(),
                loaderBox.getValue() == null ? null : loaderBox.getValue().value(),
                pageIndex,
                PAGE_SIZE
        );
        loadTask = controller.searchMods(query, this::showPage, this::showError);
    }

    private void showPage(ModSearchPage page) {
        if (disposed) {
            return;
        }
        loading = false;
        setControlsDisabled(false);
        hasNext = page.hasNext();
        lastItems = page.items();
        resultsBox.getChildren().clear();
        long generation = iconGeneration;
        List<IconTarget> iconTargets = new ArrayList<>();
        if (page.items().isEmpty()) {
            setStatus(UiText.tr("mods.search.empty"), false);
        } else {
            setStatus(UiText.tr("mods.search.count", page.totalCount()), false);
            for (ModProjectSummary project : page.items()) {
                resultsBox.getChildren().add(projectRow(project, iconTargets));
            }
        }
        loadIcons(iconTargets, generation);
        pageLabel.setText(UiText.tr("mods.page.info", pageIndex + 1));
        previousButton.setDisable(pageIndex == 0);
        nextButton.setDisable(!hasNext);
        pageScroll.setVvalue(0);
    }

    private Region projectRow(ModProjectSummary project, List<IconTarget> iconTargets) {
        ModDownloadLayout.Plan plan = plan();
        int iconSize = (int) Math.round(plan.iconSize());

        Label iconText = new Label(project.name() == null || project.name().isBlank()
                ? "?"
                : project.name().substring(0, 1).toUpperCase());
        iconText.getStyleClass().add("app-mod-icon-text");
        StackPane placeholder = new StackPane(iconText);
        placeholder.getStyleClass().add("app-mod-icon-placeholder");
        placeholder.setMinSize(iconSize, iconSize);
        placeholder.setPrefSize(iconSize, iconSize);
        placeholder.setMaxSize(iconSize, iconSize);
        StackPane icon = new StackPane(placeholder);
        icon.getStyleClass().add("app-mod-icon-frame");
        icon.setMinSize(iconSize, iconSize);
        icon.setPrefSize(iconSize, iconSize);
        icon.setMaxSize(iconSize, iconSize);
        if (project.iconUrl() != null && !project.iconUrl().isBlank()) {
            iconTargets.add(new IconTarget(project.iconUrl(), icon, placeholder, iconSize));
        }

        Label name = new Label(project.name());
        name.getStyleClass().add("app-mod-title");
        name.setWrapText(false);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        Label source = new Label(ModViewSupport.sourceLabel(project.source()));
        source.getStyleClass().add("app-mod-source");
        source.setMinWidth(Region.USE_PREF_SIZE);
        source.setAlignment(Pos.CENTER_RIGHT);
        HBox heading = new HBox(name, source);
        heading.getStyleClass().add("app-mod-title-row");
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label summary = new Label(project.summary());
        summary.getStyleClass().add("app-mod-summary");
        summary.setWrapText(false);
        summary.setTextOverrun(OverrunStyle.ELLIPSIS);
        summary.setMinWidth(0);
        summary.setMaxWidth(Double.MAX_VALUE);
        Label meta = new Label(UiText.tr("mods.list.meta",
                ModViewSupport.joinLoaders(project.loaders()),
                ModViewSupport.joinVersions(project.gameVersions()),
                ModViewSupport.downloads(project.downloads()),
                ViewSupport.releaseDate(project.updatedAt())));
        meta.getStyleClass().add("app-mod-meta");
        meta.setWrapText(false);
        meta.setTextOverrun(OverrunStyle.ELLIPSIS);
        meta.setMinWidth(0);
        meta.setMaxWidth(Double.MAX_VALUE);

        VBox text = new VBox(heading, summary, meta);
        text.getStyleClass().add("app-mod-text");
        text.setFillWidth(true);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        return ModViewSupport.resultRow(plan, icon, text,
                project.name() + " " + project.summary(),
                () -> onProjectSelected.accept(project));
    }

    private ModDownloadLayout.Plan plan() {
        return appliedPlan == null ? ModDownloadLayout.WIDE : appliedPlan;
    }

    private void loadIcons(List<IconTarget> targets, long generation) {
        IconBatch batch = new IconBatch(targets, generation);
        for (int index = 0; index < targets.size(); index++) {
            startIcon(batch, index);
        }
    }

    private void startIcon(IconBatch batch, int index) {
        IconTarget target = batch.targets.get(index);
        FrontTask<byte[]> task = controller.loadModIcon(target.url(),
                bytes -> settleIcon(batch, index, bytes),
                ignored -> onIconFailed(batch, index));
        batch.tasks.put(index, task);
        iconTasks.add(task);

        PauseTransition timeout = new PauseTransition(
                Duration.millis(ICON_FIRST_ATTEMPT_TIMEOUT_MILLIS));
        timeout.setOnFinished(event -> {
            if (!isCurrent(batch.generation) || batch.settled.contains(index)) {
                return;
            }
            FrontTask<byte[]> running = batch.tasks.remove(index);
            if (running != null) {
                running.cancel();
            }
            onIconFailed(batch, index);
        });
        batch.timeouts.put(index, timeout);
        iconTimeouts.add(timeout);
        timeout.playFromStart();
    }

    private void onIconFailed(IconBatch batch, int index) {
        if (settleIcon(batch, index, EMPTY_ICON)) {
            scheduleIconRetry(batch, index, 1);
        }
    }

    private boolean settleIcon(IconBatch batch, int index, byte[] bytes) {
        if (!isCurrent(batch.generation) || !batch.settled.add(index)) {
            return false;
        }
        PauseTransition timeout = batch.timeouts.remove(index);
        if (timeout != null) {
            timeout.stop();
        }
        batch.tasks.remove(index);
        batch.completed.put(index, bytes);
        displayCompletedIcons(batch);
        return true;
    }

    private void displayCompletedIcons(IconBatch batch) {
        while (isCurrent(batch.generation)
                && batch.completed.containsKey(batch.nextToDisplay[0])) {
            int index = batch.nextToDisplay[0]++;
            byte[] bytes = batch.completed.remove(index);
            applyIcon(batch.targets.get(index), bytes);
        }
    }

    private void scheduleIconRetry(IconBatch batch, int index, int attempt) {
        if (!isCurrent(batch.generation) || attempt > ICON_RETRY_LIMIT) {
            return;
        }
        IconTarget target = batch.targets.get(index);
        FrontTask<byte[]> task = controller.retryModIcon(
                target.url(),
                ICON_RETRY_DELAY_MILLIS,
                bytes -> onIconRetryLoaded(batch, index, bytes),
                ignored -> scheduleIconRetry(batch, index, attempt + 1));
        iconRetryTasks.add(task);
    }

    private void onIconRetryLoaded(IconBatch batch, int index, byte[] bytes) {
        if (!isCurrent(batch.generation)) {
            return;
        }
        if (batch.nextToDisplay[0] <= index) {
            batch.completed.put(index, bytes);
            displayCompletedIcons(batch);
        } else {
            applyIcon(batch.targets.get(index), bytes);
        }
    }

    private void cancelIconTasks() {
        for (FrontTask<byte[]> task : iconTasks) {
            task.cancel();
        }
        iconTasks.clear();
        for (FrontTask<byte[]> task : iconRetryTasks) {
            task.cancel();
        }
        iconRetryTasks.clear();
        for (PauseTransition timeout : iconTimeouts) {
            timeout.stop();
        }
        iconTimeouts.clear();
    }

    private boolean isCurrent(long generation) {
        return !disposed && generation == iconGeneration;
    }

    private static void applyIcon(IconTarget target, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        StackPane frame = target.frame();
        if (frame.getChildren().size() > 1) {
            return;
        }
        int size = target.size();
        Image image = new Image(new ByteArrayInputStream(bytes), size, size, false, true);
        if (image.isError()) {
            return;
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(size * 0.24);
        clip.setArcHeight(size * 0.24);
        view.setClip(clip);
        target.placeholder().setVisible(false);
        target.placeholder().setManaged(false);
        frame.getChildren().add(view);
    }

    private void reset() {
        queryField.clear();
        gameVersionField.clear();
        sourceBox.setValue(sourceBox.getItems().getFirst());
        loaderBox.setValue(loaderBox.getItems().getFirst());
        searchFirstPage();
    }

    private void setControlsDisabled(boolean disabled) {
        searchButton.setDisable(disabled);
        resetButton.setDisable(disabled);
        sourceBox.setDisable(disabled);
        loaderBox.setDisable(disabled);
        previousButton.setDisable(disabled || pageIndex == 0);
        nextButton.setDisable(disabled || !hasNext);
    }

    private void showError(String message) {
        if (disposed) {
            return;
        }
        loading = false;
        setControlsDisabled(false);
        setStatus(UiText.tr("mods.search.failed", message), true);
    }

    private void setStatus(String text, boolean error) {
        statusLabel.getStyleClass().removeAll("field-hint", "field-hint-error");
        statusLabel.getStyleClass().add(error ? "field-hint-error" : "field-hint");
        statusLabel.setText(text);
    }

    @Override
    public void dispose() {
        disposed = true;
        cancelIconTasks();
        if (loadTask != null) {
            loadTask.cancel();
            loadTask = null;
        }
    }

    private record IconTarget(String url, StackPane frame, StackPane placeholder, int size) {
    }

    private static final class IconBatch {
        private final List<IconTarget> targets;
        private final long generation;
        private final Map<Integer, byte[]> completed = new HashMap<>();
        private final Map<Integer, FrontTask<byte[]>> tasks = new HashMap<>();
        private final Map<Integer, PauseTransition> timeouts = new HashMap<>();
        private final Set<Integer> settled = new HashSet<>();
        private final int[] nextToDisplay = {0};

        private IconBatch(List<IconTarget> targets, long generation) {
            this.targets = targets;
            this.generation = generation;
        }
    }
}
