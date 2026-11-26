package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.chs.camlauncher.backend.auth.AccountSummary;
import org.chs.camlauncher.backend.auth.AccountType;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftDeviceCode;
import org.chs.camlauncher.front.controller.AccountController;
import org.chs.camlauncher.front.text.AccountChoice;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlComboBox;
import org.chs.camlauncher.ui.control.CamlPasswordField;
import org.chs.camlauncher.ui.control.CamlRadioButton;
import org.chs.camlauncher.ui.control.CamlTextField;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设置页 → 账号：全局账号列表、第三方服务器登录与 authlib-injector 覆盖。
 *
 * <p>账号密码只在本次登录时使用：密码不落盘，accessToken 由后端加密保存。</p>
 */
final class AccountSettingsView extends VBox {
    private final AccountController controller;

    private final VBox accountList = new VBox();
    private final Label statusLabel = ViewSupport.hintLabel();
    private final ToggleGroup defaultGroup = new ToggleGroup();
    private final CamlComboBox<String> serverBox = new CamlComboBox<>();
    private final CamlTextField customServerField = new CamlTextField();
    private final CamlTextField usernameField = new CamlTextField();
    private final CamlPasswordField passwordField = new CamlPasswordField();
    private final CheckBox rememberBox = new CheckBox(UiText.tr("account.login.remember"));
    private final Label injectorLabel = ViewSupport.hintLabel();
    private final CamlButton loginButton;
    private final Label microsoftStatus = ViewSupport.hintLabel();
    private final Label microsoftCode = ViewSupport.hintLabel();
    private final Label microsoftUrl = ViewSupport.hintLabel();
    private final CamlButton microsoftLoginButton = CamlButton.filled(
            UiText.tr("account.microsoft.login"));
    private final CamlButton microsoftCopyButton = CamlButton.outlined(
            UiText.tr("account.microsoft.copyCode"));
    private final CamlButton microsoftCopyUrlButton = CamlButton.outlined(
            UiText.tr("account.microsoft.copyUrl"));

    private List<AccountSummary> accounts = List.of();
    private Map<String, String> presetNames = Map.of();
    private String defaultAccountId;
    private boolean suppressDefaultEvents;
    private String microsoftUserCode = "";
    private String microsoftVerificationUri = "";

    AccountSettingsView(AccountController controller) {
        this.controller = controller;
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("app-settings-content");

        loginButton = CamlButton.filled(UiText.tr("account.login.submit"));

        getChildren().addAll(buildAccountsGroup(), buildMicrosoftGroup(),
                buildLoginGroup(), buildInjectorGroup());
        refresh();
    }

    // ------------------------------------------------------------------ 账号列表

    private VBox buildAccountsGroup() {
        Label heading = new Label(UiText.tr("settings.account.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("settings.account.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        accountList.getStyleClass().add("app-settings-block");

        CamlTextField offlineField = new CamlTextField();
        offlineField.setPromptText(UiText.tr("account.offline.placeholder"));
        CamlButton addOffline = CamlButton.outlined(UiText.tr("account.offline.add"));
        addOffline.setOnAction(event -> {
            String name = offlineField.getText() == null ? "" : offlineField.getText().trim();
            if (name.isEmpty()) {
                showStatus(UiText.tr("account.error.usernameRequired"), true);
                return;
            }
            controller.addOfflineAccount(name, account -> {
                offlineField.clear();
                showStatus(UiText.tr("account.offline.added", account.displayName()), false);
                refresh();
            }, message -> showStatus(message, true));
        });

        HBox offlineRow = new HBox(offlineField, addOffline);
        offlineRow.getStyleClass().add("app-settings-row");
        offlineRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(offlineField, Priority.ALWAYS);

        VBox group = new VBox(heading, description, accountList,
                ViewSupport.buildField(UiText.tr("account.offline.label"), offlineRow, null),
                statusLabel);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);
        return group;
    }

    private void refresh() {
        controller.loadAccounts(this::applyAccounts,
                message -> showStatus(message, true));
    }

    private void applyAccounts(List<AccountSummary> loaded) {
        accounts = loaded == null ? List.of() : loaded;
        rebuildAccountRows();
        rebuildServerChoices();
    }

    private void rebuildAccountRows() {
        accountList.getChildren().clear();
        if (accounts.isEmpty()) {
            Label empty = ViewSupport.hintLabel();
            empty.getStyleClass().add("app-empty-state");
            empty.setText(UiText.tr("account.list.empty"));
            accountList.getChildren().add(empty);
            return;
        }

        suppressDefaultEvents = true;
        defaultGroup.getToggles().clear();
        defaultAccountId = null;
        VBox rows = new VBox();
        rows.getStyleClass().add("app-account-list");
        for (AccountSummary account : accounts) {
            Label initial = new Label(account.displayName().isBlank()
                    ? "?"
                    : account.displayName().substring(0, 1).toUpperCase());
            initial.getStyleClass().add("app-account-avatar-text");
            StackPane avatar = new StackPane(initial);
            avatar.getStyleClass().addAll("app-account-avatar", switch (account.type()) {
                case OFFLINE -> "app-account-avatar-offline";
                case YGGDRASIL -> "app-account-avatar-yggdrasil";
                case MICROSOFT -> "app-account-avatar-microsoft";
            });

            CamlRadioButton isDefault = new CamlRadioButton(UiText.tr("account.list.default"));
            isDefault.setToggleGroup(defaultGroup);
            isDefault.setUserData(account.id());
            isDefault.selectedProperty().addListener((observable, oldValue, selected) -> {
                if (selected && !suppressDefaultEvents) {
                    defaultAccountId = account.id();
                    controller.saveDefaultAccount(account.id(),
                            () -> showStatus(UiText.tr("account.default.saved", AccountChoice.label(account)), false),
                            message -> showStatus(message, true));
                }
            });

            Label name = new Label(AccountChoice.label(account));
            name.getStyleClass().add("app-account-name");
            Label meta = new Label(accountMeta(account));
            meta.getStyleClass().add("app-account-meta");
            if (account.needsLogin()) {
                meta.getStyleClass().add("app-account-meta-error");
            }
            VBox info = new VBox(name, meta);
            info.getStyleClass().add("app-account-info");

            HBox controls = new HBox(isDefault);
            controls.getStyleClass().add("app-account-controls");
            controls.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(avatar, info, controls);
            row.getStyleClass().add("app-account-row");
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(info, Priority.ALWAYS);

            if (account.needsLogin()) {
                CamlButton relogin = CamlButton.outlined(UiText.tr("account.list.relogin"));
                relogin.setOnAction(event -> prepareRelogin(account));
                controls.getChildren().add(relogin);
            }

            if (account.multiProfile()) {
                // 多角色账号：允许在账号内切换角色，切换后账号 id 不变。
                CamlComboBox<String> profileBox = new CamlComboBox<>();
                for (var profile : account.profiles()) {
                    profileBox.getItems().add(profile.id());
                }
                profileBox.setConverter(new javafx.util.StringConverter<>() {
                    @Override
                    public String toString(String value) {
                        return profileLabel(account, value);
                    }

                    @Override
                    public String fromString(String value) {
                        return profileBox.getItems().stream()
                                .filter(item -> profileLabel(account, item).equals(value))
                                .findFirst()
                                .orElse(null);
                    }
                });
                profileBox.setValue(account.uuid());
                profileBox.valueProperty().addListener((observable, oldValue, value) -> {
                    if (value == null || value.equals(oldValue)
                            || value.equalsIgnoreCase(account.uuid())) {
                        return;
                    }
                    profileBox.setDisable(true);
                    controller.switchProfile(account.id(), value, switched -> {
                        profileBox.setDisable(false);
                        showStatus(UiText.tr("account.profile.switched",
                                AccountChoice.label(switched)), false);
                        refresh();
                    }, message -> {
                        profileBox.setDisable(false);
                        profileBox.setValue(oldValue);
                        showStatus(message, true);
                    });
                });

                Label profileLabel = new Label(UiText.tr("account.profile.label"));
                profileLabel.getStyleClass().add("caml-label-small");
                profileBox.getStyleClass().add("app-account-profile");
                controls.getChildren().addAll(profileLabel, profileBox);
            }

            CamlButton remove = CamlButton.outlined(UiText.tr("account.list.remove"));
            remove.getStyleClass().add("app-destructive-action");
            remove.setOnAction(event -> controller.removeAccount(account.id(), () -> {
                showStatus(UiText.tr("account.removed", AccountChoice.label(account)), false);
                refresh();
            }, message -> showStatus(message, true)));
            controls.getChildren().add(remove);

            rows.getChildren().add(row);
        }
        accountList.getChildren().add(rows);
        suppressDefaultEvents = false;

        controller.loadDefaultAccountId(id -> {
            defaultAccountId = id;
            selectDefault(id);
        }, message -> showStatus(message, true));
    }

    private static String profileLabel(AccountSummary account, String profileId) {
        if (profileId == null) {
            return "";
        }
        return account.profiles().stream()
                .filter(profile -> profile.sameId(profileId))
                .map(profile -> profile.name().isBlank() ? profile.id() : profile.name())
                .findFirst()
                .orElse(profileId);
    }

    private static String accountMeta(AccountSummary account) {
        String kind = switch (account.type()) {
            case OFFLINE -> UiText.tr("account.choice.offline");
            case YGGDRASIL -> account.serverName().isBlank()
                    ? UiText.tr("settings.account.login.title")
                    : account.serverName();
            case MICROSOFT -> UiText.tr("settings.account.microsoft.title");
        };
        return account.needsLogin()
                ? kind + " · " + UiText.tr("account.list.needsLogin")
                : kind;
    }

    /**
     * 重新登录：把认证服务器与用户名填回登录表单，只留密码给用户输入。
     *
     * <p>不自动发起请求：密码必须由用户重新输入，启动器从不保存密码。</p>
     */
    private void prepareRelogin(AccountSummary account) {
        if (account.apiRoot().isBlank()) {
            return;
        }
        List<String> presets = controller.presetApiRoots();
        if (presets.contains(account.apiRoot())) {
            serverBox.setValue(account.apiRoot());
        } else {
            serverBox.setValue(customServerRoot());
            customServerField.setText(account.apiRoot());
        }
        usernameField.setText(account.displayName());
        passwordField.clear();
        passwordField.requestFocus();
        showStatus(UiText.tr("account.login.reloginHint", account.displayName()), false);
    }

    private void selectDefault(String accountId) {
        suppressDefaultEvents = true;
        defaultGroup.getToggles().stream()
                .filter(toggle -> java.util.Objects.equals(toggle.getUserData(), accountId))
                .findFirst()
                .ifPresent(toggle -> toggle.setSelected(true));
        suppressDefaultEvents = false;
    }

    // ------------------------------------------------------------------ 微软登录

    private VBox buildMicrosoftGroup() {
        Label heading = new Label(UiText.tr("settings.account.microsoft.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("settings.account.microsoft.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        microsoftCode.setWrapText(true);
        microsoftUrl.setWrapText(true);
        microsoftCode.setVisible(false);
        microsoftCode.setManaged(false);
        microsoftUrl.setVisible(false);
        microsoftUrl.setManaged(false);

        microsoftCopyButton.setOnAction(event -> copyMicrosoftCode());
        microsoftCopyButton.setDisable(true);
        microsoftCopyUrlButton.setOnAction(event -> copyMicrosoftVerificationUri());
        microsoftCopyUrlButton.setDisable(true);
        microsoftLoginButton.setMaxWidth(Double.MAX_VALUE);
        microsoftLoginButton.setOnAction(event -> loginMicrosoft());

        HBox actions = new HBox(microsoftCopyButton, microsoftCopyUrlButton);
        actions.getStyleClass().add("app-settings-row");
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox group = new VBox(heading, description, microsoftStatus,
                microsoftCode, microsoftUrl, actions, microsoftLoginButton);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);

        return group;
    }

    private void loginMicrosoft() {
        microsoftLoginButton.setDisable(true);
        microsoftCopyButton.setDisable(true);
        microsoftCopyUrlButton.setDisable(true);
        microsoftUserCode = "";
        microsoftVerificationUri = "";
        microsoftCode.setVisible(false);
        microsoftCode.setManaged(false);
        microsoftUrl.setVisible(false);
        microsoftUrl.setManaged(false);
        setMicrosoftStatus(UiText.tr("account.microsoft.running"), false);

        controller.loginMicrosoft(this::showMicrosoftDeviceCode,
                account -> {
                    microsoftLoginButton.setDisable(false);
                    microsoftCopyButton.setDisable(true);
                    microsoftCopyUrlButton.setDisable(true);
                    microsoftCode.setVisible(false);
                    microsoftCode.setManaged(false);
                    microsoftUrl.setVisible(false);
                    microsoftUrl.setManaged(false);
                    setMicrosoftStatus(UiText.tr("account.login.success",
                            AccountChoice.label(account)), false);
                    refresh();
                },
                message -> {
                    microsoftLoginButton.setDisable(false);
                    microsoftCopyButton.setDisable(microsoftUserCode.isBlank());
                    microsoftCopyUrlButton.setDisable(microsoftVerificationUri.isBlank());
                    setMicrosoftStatus(message, true);
                });
    }

    private void showMicrosoftDeviceCode(MicrosoftDeviceCode code) {
        microsoftUserCode = code.userCode();
        microsoftVerificationUri = code.verificationUri();
        copyMicrosoftCode();
        microsoftCode.setText(UiText.tr("account.microsoft.userCode", code.userCode()));
        microsoftUrl.setText(UiText.tr("account.microsoft.verificationUri",
                code.verificationUri()));
        microsoftCode.setVisible(true);
        microsoftCode.setManaged(true);
        microsoftUrl.setVisible(true);
        microsoftUrl.setManaged(true);
        microsoftCopyButton.setDisable(false);
        microsoftCopyUrlButton.setDisable(false);
        setMicrosoftStatus(UiText.tr("account.microsoft.codeReady"), false);
    }

    private void copyMicrosoftCode() {
        if (microsoftUserCode.isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(microsoftUserCode);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void copyMicrosoftVerificationUri() {
        if (microsoftVerificationUri.isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(microsoftVerificationUri);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void setMicrosoftStatus(String message, boolean error) {
        microsoftStatus.setText(message == null ? "" : message);
        microsoftStatus.getStyleClass().removeAll("field-hint", "field-hint-error");
        microsoftStatus.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }

    // ------------------------------------------------------------------ 登录

    private VBox buildLoginGroup() {
        Label heading = new Label(UiText.tr("settings.account.login.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("settings.account.login.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        serverBox.setMaxWidth(Double.MAX_VALUE);
        serverBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : serverDisplayName(value);
            }

            @Override
            public String fromString(String value) {
                return serverBox.getItems().stream()
                        .filter(item -> serverDisplayName(item).equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        serverBox.valueProperty().addListener((observable, oldValue, value) ->
                customServerField.setDisable(!customServerRoot().equals(value)));

        customServerField.setPromptText(UiText.tr("account.login.customServerPlaceholder"));
        customServerField.setDisable(true);

        usernameField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        rememberBox.setSelected(true);

        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> login());

        Label hint = ViewSupport.hintLabel();
        hint.setText(UiText.tr("account.login.hint"));

        VBox mode = new VBox(
                ViewSupport.buildField(UiText.tr("account.login.server"), serverBox, null),
                ViewSupport.buildField(UiText.tr("account.login.customServer"), customServerField, null),
                ViewSupport.buildField(UiText.tr("account.login.username"), usernameField, null),
                ViewSupport.buildField(UiText.tr("account.login.password"), passwordField, hint),
                rememberBox
        );
        mode.getStyleClass().add("app-fields");

        VBox group = new VBox(heading, description, mode, loginButton);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);
        return group;
    }

    private void login() {
        String apiRoot = resolveApiRoot();
        if (apiRoot.isEmpty()) {
            showStatus(UiText.tr("account.error.serverRequired"), true);
            return;
        }
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (username.isEmpty()) {
            showStatus(UiText.tr("account.error.usernameRequired"), true);
            return;
        }
        if (password.isEmpty()) {
            showStatus(UiText.tr("account.error.passwordRequired"), true);
            return;
        }

        loginButton.setDisable(true);
        showStatus(UiText.tr("account.login.running"), false);
        controller.loginYggdrasil(apiRoot, username, password, rememberBox.isSelected(),
                account -> {
                    loginButton.setDisable(false);
                    passwordField.clear();
                    showStatus(UiText.tr("account.login.success",
                            AccountChoice.label(account)), false);
                    refresh();
                },
                message -> {
                    loginButton.setDisable(false);
                    passwordField.clear();
                    showStatus(message, true);
                });
    }

    private String resolveApiRoot() {
        String selected = serverBox.getValue();
        if (selected == null) {
            return "";
        }
        if (customServerRoot().equals(selected)) {
            return customServerField.getText() == null ? "" : customServerField.getText().trim();
        }
        return selected;
    }

    private static String customServerRoot() {
        return "@custom";
    }

    private void rebuildServerChoices() {
        presetNames = controller.presetServerNames();
        List<String> items = new ArrayList<>(controller.presetApiRoots());
        items.add(customServerRoot());
        String previous = serverBox.getValue();
        serverBox.getItems().setAll(items);
        if (previous != null && items.contains(previous)) {
            serverBox.setValue(previous);
        } else {
            serverBox.setValue(items.get(0));
        }
    }

    /** 预设服务器显示名称，取不到时退回地址。 */
    private String serverDisplayName(String apiRoot) {
        if (customServerRoot().equals(apiRoot)) {
            return UiText.tr("account.login.customServerOption");
        }
        String name = presetNames.get(apiRoot);
        return name == null || name.isBlank() ? apiRoot : name;
    }

    // ------------------------------------------------------------------ authlib-injector

    private VBox buildInjectorGroup() {
        Label heading = new Label(UiText.tr("settings.account.injector.title"));
        heading.getStyleClass().add("app-settings-group-title");

        Label description = new Label(UiText.tr("settings.account.injector.description"));
        description.getStyleClass().addAll("caml-body-medium", "caml-on-surface-muted");
        description.setWrapText(true);

        injectorLabel.setText(UiText.tr("account.injector.builtin"));

        CamlButton browse = CamlButton.outlined(UiText.tr("common.browse"));
        browse.setOnAction(event -> chooseInjector());
        CamlButton reset = CamlButton.outlined(UiText.tr("account.injector.reset"));
        reset.setOnAction(event -> controller.saveInjectorPath(null, () -> {
            injectorLabel.setText(UiText.tr("account.injector.builtin"));
            showStatus(UiText.tr("account.injector.saved"), false);
        }, message -> showStatus(message, true)));

        HBox actions = new HBox(browse, reset);
        actions.getStyleClass().add("app-settings-row");
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox group = new VBox(heading, description, injectorLabel, actions);
        group.getStyleClass().add("app-settings-group");
        group.setMaxWidth(Double.MAX_VALUE);

        controller.loadInjectorPath(path -> injectorLabel.setText(path == null
                ? UiText.tr("account.injector.builtin")
                : UiText.tr("account.injector.custom", path)),
                message -> showStatus(message, true));
        return group;
    }

    private void chooseInjector() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(UiText.tr("account.injector.chooserTitle"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JAR", "*.jar"));
        Window window = getScene() == null ? null : getScene().getWindow();
        java.io.File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        Path path = file.toPath().toAbsolutePath().normalize();
        controller.saveInjectorPath(path, () -> {
            injectorLabel.setText(UiText.tr("account.injector.custom", path));
            showStatus(UiText.tr("account.injector.saved"), false);
        }, message -> showStatus(message, true));
    }

    // ------------------------------------------------------------------ 工具

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.getStyleClass().removeAll("field-hint", "field-hint-error");
        statusLabel.getStyleClass().add(error ? "field-hint-error" : "field-hint");
    }

    /** 重新加载账号列表，供设置页重新显示该子页时调用。 */
    void reload() {
        refresh();
    }
}
