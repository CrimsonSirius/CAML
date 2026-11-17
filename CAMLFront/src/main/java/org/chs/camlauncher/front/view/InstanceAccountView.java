package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.backend.api.AccountSummary;
import org.chs.camlauncher.front.controller.AccountController;
import org.chs.camlauncher.front.controller.FrontTask;
import org.chs.camlauncher.front.controller.HomeController;
import org.chs.camlauncher.front.text.AccountChoice;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 主页二级页：为每个实例单独指定账号。
 *
 * <p>没有为实例指定账号时回退到设置页里的全局默认账号；账号解析顺序由后端
 * {@code AccountService.resolve} 保证，界面只负责写入绑定。</p>
 */
final class InstanceAccountView extends VBox implements PageLifecycle {
    private final AccountController accountController;
    private final Path gameDirectory;
    private final Runnable onBack;
    private final Label statusLabel = ViewSupport.hintLabel();
    private final ScrollPane scroll = new ScrollPane();
    private final List<FrontTask<?>> tasks = new ArrayList<>();

    private List<AccountSummary> accounts = List.of();
    private boolean disposed;

    InstanceAccountView(AccountController accountController, HomeController homeController,
                        Path gameDirectory, Runnable onBack) {
        this.accountController = accountController;
        this.gameDirectory = gameDirectory;
        this.onBack = onBack;

        getStyleClass().add("app-instance-picker");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        CamlButton backButton = ViewSupport.backButton(onBack);

        Label title = new Label(UiText.tr("account.instance.title"));
        title.getStyleClass().add("caml-headline-small");

        Label pathLabel = new Label(gameDirectory == null
                ? UiText.tr("instances.picker.noGameDirectory")
                : UiText.tr("instances.picker.gameDirectory", gameDirectory));
        pathLabel.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");
        pathLabel.setWrapText(true);

        VBox titles = new VBox(title, pathLabel);
        titles.getStyleClass().add("app-instance-picker-titles");
        HBox header = new HBox(backButton, titles);
        header.getStyleClass().add("app-instance-picker-header");
        header.setAlignment(Pos.CENTER_LEFT);

        statusLabel.setText(UiText.tr("account.instance.loading"));

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("app-instance-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, statusLabel, scroll);

        if (gameDirectory == null) {
            statusLabel.setText(UiText.tr("instances.picker.noGameDirectoryHint"));
            return;
        }

        tasks.add(accountController.loadAccounts(this::onAccountsLoaded,
                message -> statusLabel.setText(message)));
        tasks.add(homeController.inspectGameDirectory(gameDirectory, info -> {
            if (disposed) {
                return;
            }
            if (!info.directoryExists()) {
                statusLabel.setText(UiText.tr("instances.picker.missing", info.directory()));
                return;
            }
            if (!info.hasInstalledVersions()) {
                statusLabel.setText(UiText.tr("instances.picker.empty"));
                return;
            }
            buildRows(info.installedVersions());
        }, message -> statusLabel.setText(UiText.tr("instances.picker.scanFailed", message))));
    }

    private void onAccountsLoaded(List<AccountSummary> loaded) {
        if (disposed) {
            return;
        }
        accounts = loaded == null ? List.of() : loaded;
    }

    private void buildRows(List<String> instances) {
        VBox list = new VBox();
        list.getStyleClass().add("app-instance-list");

        for (String instanceId : instances) {
            Label name = new Label(instanceId);
            name.getStyleClass().add("caml-label-medium");
            HBox.setHgrow(name, Priority.ALWAYS);

            CamlComboBox<String> choiceBox = new CamlComboBox<>();
            choiceBox.setMaxWidth(Double.MAX_VALUE);
            choiceBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(String value) {
                    return AccountChoice.label(value == null ? AccountChoice.DEFAULT : value, accounts);
                }

                @Override
                public String fromString(String value) {
                    return choiceBox.getItems().stream()
                            .filter(item -> toString(item).equals(value))
                            .findFirst()
                            .orElse(AccountChoice.DEFAULT);
                }
            });

            List<String> choices = new ArrayList<>();
            choices.add(AccountChoice.DEFAULT);
            for (AccountSummary account : accounts) {
                choices.add(account.id());
            }
            choiceBox.getItems().setAll(choices);
            choiceBox.setValue(AccountChoice.DEFAULT);

            choiceBox.valueProperty().addListener((observable, oldValue, value) -> {
                if (disposed || value == null || value.equals(oldValue)) {
                    return;
                }
                String accountId = AccountChoice.DEFAULT.equals(value) ? null : value;
                tasks.add(accountController.saveInstanceAccount(gameDirectory, instanceId,
                        accountId,
                        () -> statusLabel.setText(UiText.tr("account.instance.saved", instanceId)),
                        message -> statusLabel.setText(message)));
            });

            tasks.add(accountController.loadInstanceAccount(gameDirectory, instanceId, accountId -> {
                if (disposed) {
                    return;
                }
                choiceBox.setValue(accountId == null ? AccountChoice.DEFAULT : accountId);
            }, message -> statusLabel.setText(message)));

            HBox row = new HBox(name, choiceBox);
            row.getStyleClass().add("app-settings-row");
            row.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(row);
        }

        scroll.setContent(list);
        statusLabel.setText(UiText.tr("account.instance.count", instances.size()));
    }

    @Override
    public void dispose() {
        disposed = true;
        for (FrontTask<?> task : tasks) {
            if (task != null) {
                task.cancel();
            }
        }
        tasks.clear();
    }
}

