package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AccountService;
import org.chs.camlauncher.backend.auth.AccountSummary;
import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.InstanceAccountMismatch;
import org.chs.camlauncher.backend.auth.OfflineAccount;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjectorService;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftDeviceCode;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.backend.settings.SettingsService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** 账号、认证服务器和实例账号绑定入口。 */
public final class BackendAccounts {
    private static final Logger LOGGER = LogManager.getLogger(BackendAccounts.class);

    private final AccountService accountService;
    private final AuthlibInjectorService authlibInjectorService;
    private final SettingsService settingsService;

    BackendAccounts(
            AccountService accountService,
            AuthlibInjectorService authlibInjectorService,
            SettingsService settingsService
    ) {
        this.accountService = Objects.requireNonNull(accountService, "accountService");
        this.authlibInjectorService = Objects.requireNonNull(
                authlibInjectorService, "authlibInjectorService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
    }

    void initialize() {
        try {
            authlibInjectorService.setOverride(
                    settingsService.authlibInjectorPath().orElse(null));
        } catch (IOException e) {
            LOGGER.warn("无法读取 authlib-injector 路径设置，使用内置版本", e);
            authlibInjectorService.setOverride(null);
        }
    }

    public AuthSession loginOffline(String username) {
        return new OfflineAccount(username).login();
    }

    public List<AccountSummary> listAccounts() throws IOException {
        return accountService.list();
    }

    public Optional<AccountSummary> defaultAccount() throws IOException {
        String id = settingsService.defaultAccountId().orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        return accountService.find(id);
    }

    public void setDefaultAccountId(String accountId) throws IOException {
        accountService.setDefaultAccountId(accountId);
    }

    public AccountSummary loginYggdrasil(String apiRoot, String username, String password,
                                          boolean remember)
            throws IOException, InterruptedException {
        return accountService.loginYggdrasil(apiRoot, username, password, remember);
    }

    public AccountSummary loginMicrosoft(Consumer<MicrosoftDeviceCode> deviceCodeListener)
            throws IOException, InterruptedException {
        return accountService.loginMicrosoft(deviceCodeListener);
    }

    public AccountSummary addOfflineAccount(String username) throws IOException {
        return accountService.addOfflineAccount(username);
    }

    public void removeAccount(String accountId) throws IOException {
        accountService.remove(accountId);
    }

    public AuthServerInfo fetchAuthServer(String apiRoot) throws IOException, InterruptedException {
        return accountService.fetchServer(apiRoot);
    }

    public List<String> authServerPresets() {
        return accountService.presetApiRoots();
    }

    public Map<String, String> authServerPresetNames() {
        return accountService.presetServerNames();
    }

    public Optional<String> instanceAccountId(Path gameDirectory, String instanceId)
            throws IOException {
        return accountService.instanceAccountId(gameDirectory, instanceId);
    }

    public void setInstanceAccountId(Path gameDirectory, String instanceId, String accountId)
            throws IOException {
        accountService.setInstanceAccountId(gameDirectory, instanceId, accountId);
    }

    public Optional<InstanceAccountMismatch> staleInstanceAccounts(
            Path gameDirectory, List<String> currentInstances) throws IOException {
        return accountService.detectInstanceAccountMismatch(gameDirectory, currentInstances);
    }

    public int reapplyInstanceAccounts(Path gameDirectory, List<String> currentInstances)
            throws IOException {
        return accountService.reapplyInstanceAccounts(gameDirectory, currentInstances);
    }

    public Optional<Path> authlibInjectorPath() throws IOException {
        return settingsService.authlibInjectorPath();
    }

    public void setAuthlibInjectorPath(Path jar) throws IOException {
        settingsService.setAuthlibInjectorPath(jar);
        accountService.setInjectorOverride(jar);
    }

    public AccountSummary switchAccountProfile(String accountId, String profileId)
            throws IOException, InterruptedException {
        return accountService.switchProfile(accountId, profileId);
    }
}
