package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.front.UiScale;
import org.chs.camlauncher.front.controller.DownloadsController;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.control.CamlTopBar;
import org.chs.camlauncher.ui.control.CamlTopNav;
import org.chs.camlauncher.ui.control.CamlTopNavItem;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 应用壳层：顶部导航、页面宿主和底栏。
 *
 * <p>具体页面拆分到 {@link HomeView}、{@link DownloadsView} 和
 * {@link SettingsView}，本类不持有启动表单、日志或设置控件。</p>
 */
public final class MainView extends BorderPane {
    private final MainController controller;
    private final LaunchSettings launchSettings;
    private final SystemMemoryInfo systemMemory;
    private final BiConsumer<LanguageSelectionMode, String> onLanguageChanged;
    private final BiConsumer<LanguageFallbackMode, String> onLanguageFallbackChanged;
    private final HomeView homeView;
    private final DownloadsView downloadsView;
    private final SettingsView settingsView;
    private final CamlPageHost pageHost;
    private CamlTopNav navigation;
    private CamlTopNavItem settingsNavItem;
    private PageLifecycle activePage;
    private LanguageSelectionMode languageMode;
    private String language;
    private LanguageFallbackMode languageFallbackMode;
    private String languageFallbackLocale;
    private double appliedFontPx = -1;

    public MainView(MainController controller, int initialMaxMemoryMb,
                    SystemMemoryInfo systemMemory, Path initialGameDirectory,
                    boolean fullVersionIsolation, String initialSelectedInstance,
                    JavaSelectionMode initialJavaSelectionMode, Path initialJavaPath,
                    LanguageSelectionMode initialLanguageMode, String initialLanguage,
                    LanguageFallbackMode initialLanguageFallbackMode,
                    String initialLanguageFallbackLocale,
                    BiConsumer<LanguageSelectionMode, String> onLanguageChanged,
                    BiConsumer<LanguageFallbackMode, String> onLanguageFallbackChanged) {
        this(controller, new LaunchSettings(
                initialMaxMemoryMb,
                initialGameDirectory,
                fullVersionIsolation,
                initialSelectedInstance,
                initialJavaSelectionMode,
                initialJavaPath
        ), systemMemory, initialLanguageMode, initialLanguage,
                initialLanguageFallbackMode, initialLanguageFallbackLocale,
                onLanguageChanged, onLanguageFallbackChanged);
    }

    private MainView(MainController controller, LaunchSettings launchSettings,
                     SystemMemoryInfo systemMemory, LanguageSelectionMode initialLanguageMode,
                     String initialLanguage,
                     LanguageFallbackMode initialLanguageFallbackMode,
                     String initialLanguageFallbackLocale,
                     BiConsumer<LanguageSelectionMode, String> onLanguageChanged,
                     BiConsumer<LanguageFallbackMode, String> onLanguageFallbackChanged) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.launchSettings = Objects.requireNonNull(launchSettings, "launchSettings");
        this.systemMemory = Objects.requireNonNull(systemMemory, "systemMemory");
        this.onLanguageChanged = Objects.requireNonNull(onLanguageChanged, "onLanguageChanged");
        this.onLanguageFallbackChanged = Objects.requireNonNull(
                onLanguageFallbackChanged, "onLanguageFallbackChanged");
        this.languageMode = initialLanguageMode == null
                ? LanguageSelectionMode.AUTO
                : initialLanguageMode;
        this.language = initialLanguage == null ? UiText.DEFAULT_LOCALE : initialLanguage;
        this.languageFallbackMode = initialLanguageFallbackMode == null
                ? LanguageFallbackMode.KEY
                : initialLanguageFallbackMode;
        this.languageFallbackLocale = initialLanguageFallbackLocale == null
                ? UiText.DEFAULT_LOCALE
                : initialLanguageFallbackLocale;
        getStyleClass().add("app-root");

        HomeController homeController = controller.homeController();
        DownloadsController downloadsController = controller.downloadsController();
        SettingsController settingsController = controller.settingsController();

        settingsView = new SettingsView(
                launchSettings,
                settingsController::saveMaxMemoryMbAsync,
                settingsController::saveVersionIsolationAsync,
                systemMemory,
                settingsController,
                languageMode,
                language,
                languageFallbackMode,
                languageFallbackLocale,
                (mode, locale) -> {
                    this.languageMode = mode;
                    this.language = locale == null ? UiText.DEFAULT_LOCALE : locale;
                    this.onLanguageChanged.accept(mode, this.language);
                },
                (mode, locale) -> {
                    this.languageFallbackMode = mode;
                    this.languageFallbackLocale = locale == null
                            ? UiText.DEFAULT_LOCALE
                            : locale;
                    this.onLanguageFallbackChanged.accept(mode, this.languageFallbackLocale);
                }
        );
        homeView = new HomeView(homeController, launchSettings);
        downloadsView = new DownloadsView(downloadsController, launchSettings);
        settingsController.setOnDownloadSourceSaved(downloadsView::refreshVersions);
        pageHost = new CamlPageHost(homeView);
        activePage = homeView;
        homeView.onShow();

        setTop(buildTopBar());
        setCenter(pageHost);
        setBottom(buildFooter());

        // 根节点字号决定整个界面的缩放：CSS 中的 em 全部按根字号换算，
        // 需要代码计算的几何（模组页、工作区分栏）读取同一个 UiScale 因子。
        ViewSupport.onLayoutMetricsChanged(this, this::updateUiScale);
    }

    /** 使用当前共享状态重建视图，用于语言切换后立即刷新全部文案。 */
    public MainView recreate() {
        LaunchSettings copiedSettings = new LaunchSettings(
                launchSettings.maxMemoryMb(),
                launchSettings.gameDirectory(),
                launchSettings.fullVersionIsolation(),
                launchSettings.selectedVersion(),
                launchSettings.javaSelectionMode(),
                launchSettings.selectedJavaPath()
        );
        return new MainView(controller, copiedSettings, systemMemory,
                languageMode, language, languageFallbackMode, languageFallbackLocale,
                onLanguageChanged, onLanguageFallbackChanged);
    }

    private CamlTopBar buildTopBar() {
        CamlTopBar bar = new CamlTopBar("CAML", UiText.tr("app.subtitle"));

        navigation = new CamlTopNav();
        CamlTopNavItem home = navigation.addItem(UiText.tr("nav.home"));
        CamlTopNavItem downloads = navigation.addItem(UiText.tr("nav.downloads"));
        settingsNavItem = navigation.addItem(UiText.tr("nav.settings"));
        navigation.select(home);
        navigation.selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected == home) {
                showPage(homeView);
            } else if (selected == downloads) {
                showPage(downloadsView);
            } else if (selected == settingsNavItem) {
                showPage(settingsView);
            }
        });
        bar.addNavigation(navigation);
        return bar;
    }

    private void showPage(Node page) {
        if (activePage != null) {
            activePage.onHide();
        }
        Node sidebar = page instanceof SidebarPage sidebarPage ? sidebarPage.sidebar() : null;
        pageHost.setPage(page, sidebar);
        activePage = page instanceof PageLifecycle lifecycle ? lifecycle : null;
        if (activePage != null) {
            activePage.onShow();
        }
    }

    private Label buildFooter() {
        BackendStatus backendStatus = controller.status();
        Label footer = new Label(UiText.tr("footer.status",
                backendStatus.version(),
                backendStatus.ready()
                        ? UiText.tr("footer.backend.ready")
                        : UiText.tr("footer.backend.notReady"),
                backendStatus.dataDirectory()));
        footer.getStyleClass().addAll("caml-body-small", "app-footer-bar");
        footer.setWrapText(true);
        footer.setMaxWidth(Double.MAX_VALUE);
        return footer;
    }

    /** 填充设置页的主题选择器；应用入口负责实际加载 CSS 并持久化。 */
    public void configureThemes(List<ThemeInfo> themes, ThemeInfo active, Consumer<ThemeInfo> onSelected) {
        settingsView.configureThemes(themes, active, onSelected);
    }

    /** 提交所有仍处于防抖窗口内的设置修改。 */
    public void flushPendingSettings() {
        settingsView.flushPendingSettings();
    }

    /** 语言切换重建后回到设置页的外观分组。 */
    public void showSettingsPage() {
        navigation.select(settingsNavItem);
        settingsView.showAppearanceSection();
    }

    public void dispose() {
        if (activePage != null) {
            activePage.onHide();
            activePage = null;
        }
        homeView.dispose();
        downloadsView.dispose();
        settingsView.dispose();
    }

    /**
     * 按窗口尺寸更新根字号。
     *
     * <p>只在字号真正变化时写入内联样式，避免每次窗口拖动都触发整棵场景的 CSS 重算。</p>
     */
    private void updateUiScale() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        double fontPx = UiScale.baseFontPx(UiScale.factor(width, height));
        if (fontPx == appliedFontPx) {
            return;
        }
        appliedFontPx = fontPx;
        setStyle("-fx-font-size: " + (int) fontPx + "px;");
    }

}