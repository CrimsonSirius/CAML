package org.chs.camlauncher.front.view;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.backend.system.SystemMemoryInfo;
import org.chs.camlauncher.language.contract.LanguageSelectionMode;
import org.chs.camlauncher.language.contract.LanguageFallbackMode;
import org.chs.camlauncher.front.controller.AccountController;
import org.chs.camlauncher.front.controller.SettingsController;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlIcon;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;
import org.chs.camlauncher.ui.theme.CamlMotion;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** 设置页：左侧子导航，右侧游戏/下载/账号/外观/关于内容。 */
final class SettingsView extends VBox implements SidebarPage, PageLifecycle {
    enum Section {
        GAME,
        DOWNLOAD,
        ACCOUNT,
        APPEARANCE,
        ABOUT
    }

    private final GameSettingsView gameSettings;
    private final DownloadSettingsView downloadSettings;
    private final LauncherSettingsView launcherSettings;
    private final AccountSettingsView accountSettings;
    private final AboutSettingsView aboutSettings;
    private final SettingsController controller;
    private final CamlSideNav sideNav;
    private final CamlSideNavItem gameItem;
    private final CamlSideNavItem downloadItem;
    private final CamlSideNavItem launcherItem;
    private final CamlSideNavItem accountItem;
    private final CamlSideNavItem aboutItem;
    private final ScrollPane contentScroll = new ScrollPane();
    private final Label saveErrorLabel = new Label();
    private final StackPane contentHost;
    private Node currentContent;
    private Section currentSection = Section.GAME;
    private FadeTransition contentTransition;

    SettingsView(LaunchSettings launchSettings, IntConsumer onMemoryChanged,
                 Consumer<Boolean> onVersionIsolationChanged,
                 SystemMemoryInfo systemMemory, SettingsController controller,
                 AccountController accountController,
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
        accountSettings = new AccountSettingsView(accountController);
        launcherSettings = new LauncherSettingsView(
                initialLanguageMode, initialLanguage,
                initialFallbackMode, initialFallbackLocale,
                onLanguageChanged, onLanguageFallbackChanged);
        aboutSettings = new AboutSettingsView(
                controller.backendStatus(), controller.systemInfo(), controller.deviceInfo(),
                controller.projectDependencies(),
                launchSettings.gameDirectoryProperty());

        sideNav = new CamlSideNav();
        sideNav.getStyleClass().add("app-settings-sidebar");
        sideNav.setMaxHeight(Double.MAX_VALUE);
        gameItem = new CamlSideNavItem(
                new CamlIcon(CamlIcon.Glyph.SETTINGS, 20),
                UiText.tr("settings.section.game"));
        downloadItem = new CamlSideNavItem(
                new CamlIcon(CamlIcon.Glyph.DOWNLOAD, 20),
                UiText.tr("settings.section.download"));
        accountItem = new CamlSideNavItem(
                new CamlIcon(CamlIcon.Glyph.ACCOUNT, 20),
                UiText.tr("settings.section.account"));
        launcherItem = new CamlSideNavItem(
                new CamlIcon(CamlIcon.Glyph.APPEARANCE, 20),
                UiText.tr("settings.section.appearance"));
        aboutItem = new CamlSideNavItem(
                new CamlIcon(CamlIcon.Glyph.INFO, 20),
                UiText.tr("settings.section.about"));
        sideNav.addItem(gameItem);
        sideNav.addItem(downloadItem);
        sideNav.addItem(accountItem);
        sideNav.addItem(launcherItem);
        sideNav.addItem(aboutItem);
        sideNav.select(gameItem);

        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.getStyleClass().add("app-settings-scroll");
        contentScroll.setContent(gameSettings);
        currentContent = gameSettings;

        saveErrorLabel.getStyleClass().addAll("caml-body-small", "app-settings-save-error");
        saveErrorLabel.setWrapText(true);
        saveErrorLabel.setMaxWidth(Double.MAX_VALUE);
        saveErrorLabel.setVisible(false);
        saveErrorLabel.setManaged(false);
        VBox contentColumn = new VBox(saveErrorLabel, contentScroll);
        contentColumn.getStyleClass().add("app-settings-content-column");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        contentHost = new StackPane(contentColumn);
        contentHost.getStyleClass().add("app-settings-content-host");
        contentHost.setAlignment(Pos.TOP_LEFT);
        contentHost.setMaxHeight(Double.MAX_VALUE);
        // 内容列宽度跟随滚动视口并铺满可用宽度；各设置视图显式允许横向拉伸。
        contentScroll.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
            double innerWidth = Math.max(0, newValue.getWidth());
            gameSettings.setPrefWidth(innerWidth);
            downloadSettings.setPrefWidth(innerWidth);
            accountSettings.setPrefWidth(innerWidth);
            launcherSettings.setPrefWidth(innerWidth);
            aboutSettings.setPrefWidth(innerWidth);
        });
        sideNav.selectedItemProperty().addListener((observable, oldValue, selected) -> {
            if (selected == aboutItem) {
                currentSection = Section.ABOUT;
                showContent(aboutSettings);
            } else if (selected == launcherItem) {
                currentSection = Section.APPEARANCE;
                showContent(launcherSettings);
            } else if (selected == downloadItem) {
                currentSection = Section.DOWNLOAD;
                showContent(downloadSettings);
            } else if (selected == accountItem) {
                currentSection = Section.ACCOUNT;
                showContent(accountSettings);
            } else {
                currentSection = Section.GAME;
                showContent(gameSettings);
            }
        });

        setAlignment(Pos.TOP_LEFT);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getStyleClass().add("app-settings-page");
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        getChildren().add(contentHost);
        controller.setOnSaveFailed(this::showSaveError);
        controller.setOnSaveSucceeded(this::clearSaveError);
    }

    void configureThemes(List<ThemeInfo> themes, ThemeInfo active, Consumer<ThemeInfo> onSelected) {
        launcherSettings.configureThemes(themes, active, onSelected);
    }

    void flushPendingSettings() {
        gameSettings.flushPendingSave();
        downloadSettings.flushPendingSaves();
    }

    void showSection(Section section) {
        Section target = section == null ? Section.GAME : section;
        sideNav.select(switch (target) {
            case GAME -> gameItem;
            case DOWNLOAD -> downloadItem;
            case ACCOUNT -> accountItem;
            case APPEARANCE -> launcherItem;
            case ABOUT -> aboutItem;
        });
    }

    Section currentSection() {
        return currentSection;
    }

    @Override
    public CamlSideNav sidebar() {
        return sideNav;
    }

    @Override
    public void onShow() {
        controller.startMemoryMonitor();
        if (currentContent == accountSettings) {
            accountSettings.reload();
        }
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
        if (CamlMotion.reduceMotion()) {
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

    private void showSaveError(String message) {
        saveErrorLabel.setText(message == null ? "" : message);
        saveErrorLabel.setVisible(true);
        saveErrorLabel.setManaged(true);
    }

    private void clearSaveError() {
        saveErrorLabel.setVisible(false);
        saveErrorLabel.setManaged(false);
    }
}
