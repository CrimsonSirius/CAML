package org.chs.camlauncher.front.controller;

import javafx.application.Platform;
import org.chs.camlauncher.backend.api.AccountSummary;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftDeviceCode;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.front.text.UiText;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 账号页控制器：账号列表、第三方登录、默认账号与实例账号绑定。
 *
 * <p>网络与磁盘操作全部交给后端；这里只负责选择线程池并把结果切回 JavaFX 线程。</p>
 */
public final class AccountController {
    private final ControllerContext context;
    private List<AccountSummary> cachedAccounts = List.of();

    AccountController(ControllerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /** 最近一次成功加载的账号列表，供下拉框同步渲染。 */
    public List<AccountSummary> cachedAccounts() {
        return cachedAccounts;
    }

    public List<String> presetApiRoots() {
        try {
            return context.backend().authServerPresets();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    public FrontTask<List<AccountSummary>> loadAccounts(
            Consumer<List<AccountSummary>> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.loadAccounts"),
                () -> context.backend().listAccounts(), accounts -> {
                    cachedAccounts = accounts;
                    if (onLoaded != null) {
                        onLoaded.accept(accounts);
                    }
                }, onError);
    }

    public FrontTask<AccountSummary> loginYggdrasil(
            String apiRoot, String username, String password, boolean remember,
            Consumer<AccountSummary> onLoaded, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.loginAccount"),
                () -> context.backend().loginYggdrasil(apiRoot, username, password, remember),
                onLoaded, onError);
    }

    /** 启动微软设备码登录；设备码事件会切回 JavaFX 线程。 */
    public FrontTask<AccountSummary> loginMicrosoft(
            Consumer<MicrosoftDeviceCode> onDeviceCode,
            Consumer<AccountSummary> onLoaded, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.loginMicrosoftAccount"),
                () -> context.backend().loginMicrosoft(code -> {
                    if (onDeviceCode != null) {
                        Platform.runLater(() -> onDeviceCode.accept(code));
                    }
                }), onLoaded, onError);
    }

    public FrontTask<AccountSummary> addOfflineAccount(
            String username, Consumer<AccountSummary> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.addOfflineAccount"),
                () -> context.backend().addOfflineAccount(username), onLoaded, onError);
    }

    public FrontTask<AuthServerInfo> fetchAuthServer(
            String apiRoot, Consumer<AuthServerInfo> onLoaded, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchAuthServer"),
                () -> context.backend().fetchAuthServer(apiRoot), onLoaded, onError);
    }

    public FrontTask<String> loadDefaultAccountId(
            Consumer<String> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.loadAccounts"),
                () -> context.backend().defaultAccount().map(AccountSummary::id).orElse(null),
                onLoaded, onError);
    }

    public FrontTask<Void> saveDefaultAccount(
            String accountId, Runnable onSaved, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.saveDefaultAccount"), () -> {
            context.backend().setDefaultAccountId(accountId);
            return null;
        }, ignored -> run(onSaved), onError);
    }

    public FrontTask<Void> removeAccount(
            String accountId, Runnable onRemoved, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.removeAccount"), () -> {
            context.backend().removeAccount(accountId);
            return null;
        }, ignored -> run(onRemoved), onError);
    }

    public FrontTask<String> loadInstanceAccount(
            Path gameDirectory, String instanceId,
            Consumer<String> onLoaded, Consumer<String> onError) {
        if (gameDirectory == null || instanceId == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.loadInstanceAccount"),
                () -> context.backend().instanceAccountId(gameDirectory, instanceId).orElse(null),
                onLoaded, onError);
    }

    public FrontTask<Void> saveInstanceAccount(
            Path gameDirectory, String instanceId, String accountId,
            Runnable onSaved, Consumer<String> onError) {
        if (gameDirectory == null || instanceId == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.saveInstanceAccount"), () -> {
            context.backend().setInstanceAccountId(gameDirectory, instanceId, accountId);
            return null;
        }, ignored -> run(onSaved), onError);
    }

    public FrontTask<Path> loadInjectorPath(
            Consumer<Path> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.loadAccounts"), () -> {
            try {
                return context.backend().authlibInjectorPath().orElse(null);
            } catch (java.io.IOException e) {
                throw e;
            }
        }, onLoaded, onError);
    }

    public FrontTask<Void> saveInjectorPath(
            Path jar, Runnable onSaved, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.saveInjectorPath"), () -> {
            context.backend().setAuthlibInjectorPath(jar);
            return null;
        }, ignored -> run(onSaved), onError);
    }

    private static void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}

