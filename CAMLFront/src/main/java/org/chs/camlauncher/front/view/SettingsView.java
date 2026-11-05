package org.chs.camlauncher.front.view;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.ui.control.CamlSideNav;
import org.chs.camlauncher.ui.control.CamlSideNavItem;

import java.util.List;
import java.util.function.Consumer;

/** 设置页：左侧子导航，右侧游戏/启动器设置内容。 */
final class SettingsView extends VBox implements SidebarPage {
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("caml.reduceMotion");

    private final LauncherSettingsView launcherSettings;
    private final CamlSideNav sideNav;
    private final StackPane contentHost;
    private FadeTransition contentTransition;

    SettingsView(LaunchSettings launchSettings) {
        GameSettingsView gameSettings = new GameSettingsView(launchSettings);
        launcherSettings = new LauncherSettingsView();

        sideNav = new CamlSideNav();
        sideNav.getStyleClass().add("app-settings-sidebar");
        sideNav.setMaxHeight(Double.MAX_VALUE);
        CamlSideNavItem gameItem = new CamlSideNavItem("游戏设置");
        CamlSideNavItem launcherItem = new CamlSideNavItem("启动器设置");
        sideNav.addItem(gameItem);
        sideNav.addItem(launcherItem);
        sideNav.select(gameItem);

        contentHost = new StackPane(gameSettings);
        contentHost.setAlignment(Pos.TOP_LEFT);
        contentHost.setMaxHeight(Double.MAX_VALUE);
        contentHost.setPadding(new Insets(32, 40, 32, 40));
        HBox.setHgrow(contentHost, Priority.ALWAYS);
        sideNav.selectedItemProperty().addListener((observable, oldValue, selected) ->
                showContent(selected == launcherItem ? launcherSettings : gameSettings));

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

    @Override
    public CamlSideNav sidebar() {
        return sideNav;
    }

    private void showContent(Node next) {
        if (contentHost.getChildren().size() == 1 && contentHost.getChildren().get(0) == next) {
            return;
        }
        if (REDUCE_MOTION) {
            contentHost.getChildren().setAll(next);
            next.setOpacity(1);
            return;
        }

        if (contentTransition != null) {
            contentTransition.stop();
        }
        Node current = contentHost.getChildren().isEmpty() ? null : contentHost.getChildren().get(0);
        if (current == null) {
            contentHost.getChildren().setAll(next);
            next.setOpacity(1);
            return;
        }

        current.setOpacity(1);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(80), current);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            contentHost.getChildren().setAll(next);
            next.setOpacity(0);
            contentTransition = new FadeTransition(Duration.millis(180), next);
            contentTransition.setFromValue(0);
            contentTransition.setToValue(1);
            contentTransition.play();
        });
        contentTransition = fadeOut;
        fadeOut.play();
    }
}
