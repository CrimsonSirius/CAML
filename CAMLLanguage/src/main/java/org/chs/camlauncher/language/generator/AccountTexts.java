package org.chs.camlauncher.language.generator;

/**
 * 账号相关文案的共享英文默认值。
 *
 * <p>只有 {@code TranslationTable} 里不存在的键才会写入，因此任何语言只要在
 * 自己的生成器里登记同名键就能覆盖。这样新增账号功能时不必在十一种语言里
 * 重复维护同一批键，同时仍满足"各语言键集合一致"的校验。</p>
 */
final class AccountTexts {
    private AccountTexts() {
    }

    static void populateDefaults(TranslationTable t) {
        // 账号选择
        put(t, "account.choice.default", "Follow default account");
        put(t, "account.choice.offline", "Offline username");
        put(t, "home.account", "Account");
        put(t, "home.manageAccounts", "Instance accounts");
        put(t, "home.error.loadAccounts", "Failed to load accounts: %s");

        // 设置页账号分组
        put(t, "settings.section.account", "Accounts");
        put(t, "settings.account.title", "Accounts");
        put(t, "settings.account.description",
                "Accounts saved by the launcher. Access tokens are encrypted before being "
                        + "written to disk; passwords are never stored.");
        put(t, "account.list.empty",
                "No accounts yet. Add an offline account or sign in to a third-party server.");
        put(t, "account.list.default", "Default");
        put(t, "account.list.remove", "Remove");
        put(t, "account.default.saved", "%s is now the default account");
        put(t, "account.removed", "Removed %s");

        put(t, "account.offline.label", "Add offline account");
        put(t, "account.offline.placeholder", "Offline username");
        put(t, "account.offline.add", "Add");
        put(t, "account.offline.added", "Offline account %s added");

        // 第三方登录
        put(t, "settings.account.login.title", "Third-party sign-in");
        put(t, "settings.account.login.description",
                "Sign in with an account from a Yggdrasil authentication server "
                        + "(for example LittleSkin).");
        put(t, "account.login.server", "Authentication server");
        put(t, "account.login.customServer", "Custom server address");
        put(t, "account.login.customServerPlaceholder", "https://example.com/api/yggdrasil");
        put(t, "account.login.customServerOption", "Custom address");
        put(t, "account.login.username", "Email or username");
        put(t, "account.login.password", "Password");
        put(t, "account.login.hint",
                "The password is used only for this sign-in and is never saved.");
        put(t, "account.login.remember", "Keep me signed in");
        put(t, "account.login.submit", "Sign in");
        put(t, "account.login.running", "Signing in...");
        put(t, "account.login.success", "Signed in as %s");
        put(t, "account.error.serverRequired",
                "Please enter the authentication server address");
        put(t, "account.error.usernameRequired", "Please enter the username");
        put(t, "account.error.passwordRequired", "Please enter the password");

        // authlib-injector
        put(t, "settings.account.injector.title", "authlib-injector");
        put(t, "settings.account.injector.description",
                "The launcher downloads authlib-injector once and reuses it; the file is "
                        + "verified with SHA-256 on every start.");
        put(t, "account.injector.builtin", "Using the bundled version (downloaded on demand)");
        put(t, "account.injector.custom", "Custom file: %s");
        put(t, "account.injector.reset", "Use bundled version");
        put(t, "account.injector.saved", "authlib-injector setting saved");
        put(t, "account.injector.chooserTitle", "Select authlib-injector jar");

        // 实例账号
        put(t, "account.instance.title", "Instance accounts");
        put(t, "account.instance.loading", "Loading accounts...");
        put(t, "account.instance.count",
                "%d instances; choose the account used to launch each one");
        put(t, "account.instance.saved", "Account saved for instance %s");

        // 后台操作名
        put(t, "operation.loadAccounts", "Load accounts");
        put(t, "operation.loginAccount", "Sign in to account");
        put(t, "operation.addOfflineAccount", "Add offline account");
        put(t, "operation.fetchAuthServer", "Fetch authentication server");
        put(t, "operation.saveDefaultAccount", "Save default account");
        put(t, "operation.removeAccount", "Remove account");
        put(t, "operation.loadInstanceAccount", "Load instance account");
        put(t, "operation.saveInstanceAccount", "Save instance account");
        put(t, "operation.saveInjectorPath", "Save authlib-injector path");

        // 认证错误
        put(t, "auth.error.usernameRequired", "Username is required");
        put(t, "auth.error.passwordRequired", "Password is required");
        put(t, "auth.error.metadataUnavailable",
                "Cannot read the authentication server metadata: %s (HTTP %s)");
        put(t, "auth.error.invalidCredentials",
                "Incorrect username or password for %s %s");
        put(t, "auth.error.requestFailed",
                "Authentication request failed: %s (HTTP %s) %s");
        put(t, "auth.error.responseInvalid",
                "The authentication server returned an unexpected response: %s");
        put(t, "auth.error.noSelectedProfile",
                "This account has no Minecraft profile: %s");
        put(t, "auth.error.accountMissing", "Account not found: %s");
        put(t, "auth.error.accountRequired", "Please select an account first");
        put(t, "auth.error.loginRequired",
                "Account %s (%s) has no saved credentials, please sign in again");
        put(t, "auth.error.loginExpired",
                "The sign-in of %s (%s) has expired, please sign in again");
        put(t, "auth.error.noAccount",
                "No account is configured. Add one in Settings, or use an offline username.");
        put(t, "auth.error.injectorNotFound", "authlib-injector not found: %s");
        put(t, "auth.error.injectorChecksum", "authlib-injector verification failed: %s");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}

