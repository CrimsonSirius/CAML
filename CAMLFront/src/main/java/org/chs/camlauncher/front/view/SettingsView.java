package org.chs.camlauncher.front.view;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** 设置页：左侧子导航，右侧游戏/下载/外观设置内容。 */
final class SettingsView extends VBox implements SidebarPage, PageLifecycle {
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("caml.reduceMotion");

    private final GameSettingsView gameSettings;
    private final DownloadSettingsView downloadSettings;
    private final LauncherSettingsView launcherSettings;
    private final SettingsController controller;
    private final CamlSideNav sideNav;
    private final CamlSideNavItem launcherItem;
    private final ScrollPane contentScroll = new ScrollPane();
    private final StackPane contentHost;
    private Node currentContent;
    private FadeTransition contentTransition;

    SettingsView(LaunchSettings launchSettings, IntConsumer onMemoryChanged,
                 Consumer<Boolean> onVersionIsolationChanged,
                 SystemMemoryInfo systemMemory, SettingsController controller,
                 LanguageSelectionMode initialLanguageMode, String initialLanguage,
                 LanguageFallbackMode initialFallbackMode, String initialFallbackLocale,
                 BiConsumer<LanguageSelectionMode, String> onLanguageChanged,
                 BiConsumer<LanguageFallbackMode, String> onLanguageFallbackChanged) {
        this.controller = controller;
        gameSettings = new GameSettingsView(
                launchSettings, onMemoryChanged, onVersionIsolationChanged, systemMemory, controller);
        controller.setOnSystemMemoryUpdated(gameSettings::updateSystemMemory);
        downloadSettings = new DownloadSettingsView(
                controller.downloadConcurrency(),
                controller::saveDownloadConcurrencyAsync,
                controller.downloadSourceMode(),
                controller.customDownloadApi(),
                controller::saveDownloadSourceAsync
        );
        launcherSettings = new LauncherSettingsView(
                initialLanguageMode, initialLanguage,
                initialFallbackMode, initialFallbackLocale,
                onLanguageChanged, onLanguageFallbackChanged);

        sideNav = new CamlSideNav();
        sideNav.getStyleClass().add("app-settings-sidebar");
        sideNav.setMaxHeight(Double.MAX_VALUE);
        CamlSideNavItem gameItem = new CamlSideNavItem(UiText.tr("settings.section.game"));
        CamlSideNavItem downloadItem = new CamlSideNavItem(UiText.tr("settings.section.download"));
        launcherItem = new CamlSideNavItem(UiText.tr("settings.section.appearance"));
        sideNav.addItem(gameItem);
        sideNav.addItem(downloadItem);
        sideNav.addItem(launcherItem);
        sideNav.select(gameItem);

        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.getStyleClass().add("app-settings-scroll");
        contentScroll.setContent(gameSettings);
        currentContent = gameSettings;

        contentHost = new StackPane(contentScroll);
        contentHost.setAlignment(Pos.TOP_LEFT);
        contentHost.setMaxHeight(Double.MAX_VALUE);
        contentHost.setPadding(new Insets(32, 40, 32, 40));
        HBox.setHgrow(contentHost, Priority.ALWAYS);
        contentScroll.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
            double innerWidth = Math.max(0, newValue.getWidth());
            gameSettings.setPrefWidth(innerWidth);
            gameSettings.setMaxWidth(innerWidth);
            downloadSettings.setPrefWidth(innerWidth);
            downloadSettings.setMaxWidth(innerWidth);
            launcherSettings.setPrefWidth(innerWidth);
            launcherSettings.setMaxWidth(innerWidth);
        });
        sideNav.selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected == launcherItem) {
                showContent(launcherSettings);
            } else if (selected == downloadItem) {
                showContent(downloadSettings);
            } else {
                showContent(gameSettings);
            }
        });

        HBox body = new HBox(24, sideNav, contentHost);
        body.setAlignment(Pos.TOP_LEFT);
        body.setMaxHeight(Double.MAX_VALUE);
        body.getStyleClass().add("app-settings-body");
        VBox.setVgrow(body, Priority.ALWAYS);

        setPadding(Insets.EMPTY);
        setAlignment(Pos.TOP_LEFT);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getStyleClass().add("app-settings-page");
        getChildren().add(body);
    }

    void configureThemes(List<ThemeInfo> themes, ThemeInfo active, Consumer<ThemeInfo> onSelected) {
        launcherSettings.configureThemes(themes, active, onSelected);
    }

    void flushPendingSettings() {
        gameSettings.flushPendingSave();
        downloadSettings.flushPendingSaves();
    }

    void showAppearanceSection() {
        sideNav.select(launcherItem);
    }

    @Override
    public CamlSideNav sidebar() {
        return sideNav;
    }

    @Override
    public void onShow() {
        controller.startMemoryMonitor();
    }

    @Override
    public void onHide() {
        controller.stopMemoryMonitor();
    }

    @Override
    public void dispose() {
        controller.stopMemoryMonitor();
        if (contentTransition != null) {
            contentTransition.stop();
            contentTransition = null;
        }
    }

    private void showContent(Node next) {
        if (currentContent == next) {
            return;
        }
        if (REDUCE_MOTION) {
            setContent(next);
            return;
        }

        if (contentTransition != null) {
            contentTransition.stop();
        }
        contentScroll.setOpacity(1);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(80), contentScroll);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            setContent(next);
            contentScroll.setOpacity(0);
            contentTransition = new FadeTransition(Duration.millis(180), contentScroll);
            contentTransition.setFromValue(0);
            contentTransition.setToValue(1);
            contentTransition.play();
        });
        contentTransition = fadeOut;
        fadeOut.play();
    }

    private void setContent(Node next) {
        currentContent = next;
        next.setOpacity(1);
        contentScroll.setContent(next);
        contentScroll.setVvalue(0);
    }
}
