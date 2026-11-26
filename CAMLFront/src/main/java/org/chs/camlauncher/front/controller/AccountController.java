package org.chs.camlauncher.front.controller;

import org.chs.camlauncher.front.task.AsyncTaskHandle;

import javafx.application.Platform;
import org.chs.camlauncher.backend.auth.AccountSummary;
import org.chs.camlauncher.backend.auth.InstanceAccountMismatch;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftDeviceCode;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.front.text.UiText;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    /** 预设服务器地址 → 展示名；失败时退化为空表（下拉框仍显示地址）。 */
    public Map<String, String> presetServerNames() {
        try {
            return context.backend().authServerPresetNames();
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    public AsyncTaskHandle<List<AccountSummary>> loadAccounts(
            Consumer<List<AccountSummary>> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.loadAccounts"),
                () -> context.backend().listAccounts(), accounts -> {
                    cachedAccounts = accounts;
                    if (onLoaded != null) {
                        onLoaded.accept(accounts);
                    }
                }, onError);
    }

    public AsyncTaskHandle<AccountSummary> loginYggdrasil(
            String apiRoot, String username, String password, boolean remember,
            Consumer<AccountSummary> onLoaded, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.loginAccount"),
                () -> context.backend().loginYggdrasil(apiRoot, username, password, remember),
                onLoaded, onError);
    }

    /** 启动微软设备码登录；设备码事件会切回 JavaFX 线程。 */
    public AsyncTaskHandle<AccountSummary> loginMicrosoft(
            Consumer<MicrosoftDeviceCode> onDeviceCode,
            Consumer<AccountSummary> onLoaded, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.loginMicrosoftAccount"),
                () -> context.backend().loginMicrosoft(code -> {
                    if (onDeviceCode != null) {
                        Platform.runLater(() -> onDeviceCode.accept(code));
                    }
                }), onLoaded, onError);
    }

    public AsyncTaskHandle<AccountSummary> addOfflineAccount(
            String username, Consumer<AccountSummary> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.addOfflineAccount"),
                () -> context.backend().addOfflineAccount(username), onLoaded, onError);
    }

    public AsyncTaskHandle<AuthServerInfo> fetchAuthServer(
            String apiRoot, Consumer<AuthServerInfo> onLoaded, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.fetchAuthServer"),
                () -> context.backend().fetchAuthServer(apiRoot), onLoaded, onError);
    }

    public AsyncTaskHandle<String> loadDefaultAccountId(
            Consumer<String> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.loadAccounts"),
                () -> context.backend().defaultAccount().map(AccountSummary::id).orElse(null),
                onLoaded, onError);
    }

    public AsyncTaskHandle<Void> saveDefaultAccount(
            String accountId, Runnable onSaved, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.saveDefaultAccount"), () -> {
            context.backend().setDefaultAccountId(accountId);
            return null;
        }, ignored -> run(onSaved), onError);
    }

    public AsyncTaskHandle<Void> removeAccount(
            String accountId, Runnable onRemoved, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.removeAccount"), () -> {
            context.backend().removeAccount(accountId);
            return null;
        }, ignored -> run(onRemoved), onError);
    }

    /** 切换账号使用的角色；成功后回调新的账号摘要。 */
    public AsyncTaskHandle<AccountSummary> switchProfile(
            String accountId, String profileId,
            Consumer<AccountSummary> onSwitched, Consumer<String> onError) {
        return context.runMetadata(UiText.tr("operation.switchProfile"),
                () -> context.backend().switchAccountProfile(accountId, profileId),
                onSwitched, onError);
    }

    public AsyncTaskHandle<String> loadInstanceAccount(
            Path gameDirectory, String instanceId,
            Consumer<String> onLoaded, Consumer<String> onError) {
        if (gameDirectory == null || instanceId == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.loadInstanceAccount"),
                () -> context.backend().instanceAccountId(gameDirectory, instanceId).orElse(null),
                onLoaded, onError);
    }

    public AsyncTaskHandle<Void> saveInstanceAccount(
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

    /** 检测“实例账号绑定留在旧游戏位置”的情况；没有问题时回调 null。 */
    public AsyncTaskHandle<InstanceAccountMismatch> detectInstanceAccountMismatch(
            Path gameDirectory, List<String> instances,
            Consumer<InstanceAccountMismatch> onLoaded, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.loadInstanceAccount"),
                () -> context.backend().staleInstanceAccounts(gameDirectory, instances).orElse(null),
                onLoaded, onError);
    }

    /** 把旧游戏位置下的实例账号绑定套用到当前目录。 */
    public AsyncTaskHandle<Integer> reapplyInstanceAccounts(
            Path gameDirectory, List<String> instances,
            Consumer<Integer> onApplied, Consumer<String> onError) {
        if (gameDirectory == null) {
            return null;
        }
        return context.runSettings(UiText.tr("operation.saveInstanceAccount"),
                () -> context.backend().reapplyInstanceAccounts(gameDirectory, instances),
                onApplied, onError);
    }

    public AsyncTaskHandle<Path> loadInjectorPath(
            Consumer<Path> onLoaded, Consumer<String> onError) {
        return context.runSettings(UiText.tr("operation.loadAccounts"),
                () -> context.backend().authlibInjectorPath().orElse(null),
                onLoaded, onError);
    }

    public AsyncTaskHandle<Void> saveInjectorPath(
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
