package org.chs.camlauncher.front.view;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.translation.Language;
import org.chs.camlauncher.ui.control.CamlRadioButton;

import java.util.function.Consumer;

/** 设置页中的“版本隔离”分组。 */
final class VersionIsolationSettingsView extends VBox {
    VersionIsolationSettingsView(LaunchSettings launchSettings,
                                 Consumer<Boolean> onVersionIsolationChanged) {
        Label heading = new Label(Language.tr("settings.game.isolation.title"));
        heading.getStyleClass().add("app-settings-group-title");

        ToggleGroup isolationGroup = new ToggleGroup();
        CamlRadioButton fullyIsolated = new CamlRadioButton(Language.tr("settings.game.isolation.full"));
        CamlRadioButton notIsolated = new CamlRadioButton(Language.tr("settings.game.isolation.none"));
        fullyIsolated.setToggleGroup(isolationGroup);
        notIsolated.setToggleGroup(isolationGroup);
        fullyIsolated.setSelected(launchSettings.fullVersionIsolation());
        notIsolated.setSelected(!launchSettings.fullVersionIsolation());

        isolationGroup.selectedToggleProperty().addListener((observable, oldValue, selected) -> {
            if (selected == fullyIsolated || selected == notIsolated) {
                boolean full = selected == fullyIsolated;
                launchSettings.setFullVersionIsolation(full);
                onVersionIsolationChanged.accept(full);
            }
        });
        launchSettings.fullVersionIsolationProperty().addListener((observable, oldValue, full) -> {
            if (full && !fullyIsolated.isSelected()) {
                fullyIsolated.setSelected(true);
            } else if (!full && !notIsolated.isSelected()) {
                notIsolated.setSelected(true);
            }
        });

        VBox options = new VBox(12, fullyIsolated, notIsolated);

        setSpacing(16);
        setPadding(new javafx.geometry.Insets(24));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-group");
        getChildren().addAll(heading, options);
    }
}