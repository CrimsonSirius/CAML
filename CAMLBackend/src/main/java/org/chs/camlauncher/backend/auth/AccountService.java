package org.chs.camlauncher.backend.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.AccountSummary;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjector;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjectorService;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftAuthService;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftDeviceCode;
import org.chs.camlauncher.backend.auth.microsoft.MicrosoftSession;
import org.chs.camlauncher.backend.auth.store.AccountStore;
import org.chs.camlauncher.backend.auth.store.StoredAccount;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerService;
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilClient;
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilSession;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.settings.SettingsService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 账号编排：账号列表、Yggdrasil 登录、以及“实例账号 → 全局账号”的解析。
 *
 * <p>所有网络与磁盘操作都在后端完成；前端只拿到 {@link AccountSummary} 和
 * {@link ResolvedAccount}，永远不接触 token。</p>
 */
public final class AccountService {
    private static final Logger LOGGER = LogManager.getLogger(AccountService.class);

    private final AccountStore store;
    private final AuthServerService servers;
    private final AuthlibInjectorService injectors;
    private final SettingsService settings;
    private final YggdrasilClient client;
    private final MicrosoftAuthService microsoft;

    public AccountService(AccountStore store,
                         AuthServerService servers,
                         AuthlibInjectorService injectors,
                         SettingsService settings) {
        this(store, servers, injectors, settings, new YggdrasilClient(),
                new MicrosoftAuthService());
    }

    public AccountService(AccountStore store,
                          AuthServerService servers,
                          AuthlibInjectorService injectors,
                          SettingsService settings,
                          YggdrasilClient client) {
        this(store, servers, injectors, settings, client, new MicrosoftAuthService());
    }

    public AccountService(AccountStore store,
                          AuthServerService servers,
                          AuthlibInjectorService injectors,
                          SettingsService settings,
                          YggdrasilClient client,
                          MicrosoftAuthService microsoft) {
        this.store = Objects.requireNonNull(store, "store");
        this.servers = Objects.requireNonNull(servers, "servers");
        this.injectors = Objects.requireNonNull(injectors, "injectors");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.client = Objects.requireNonNull(client, "client");
        this.microsoft = Objects.requireNonNull(microsoft, "microsoft");
    }

    // ------------------------------------------------------------------ 查询

    public List<AccountSummary> list() throws IOException {
        List<AccountSummary> result = new ArrayList<>();
        for (StoredAccount account : store.load()) {
            result.add(summary(account));
        }
        return List.copyOf(result);
    }

    public Optional<AccountSummary> find(String accountId) throws IOException {
        return store.find(accountId).map(AccountService::summary);
    }

    public Optional<String> defaultAccountId() throws IOException {
        return settings.defaultAccountId();
    }

    public void setDefaultAccountId(String accountId) throws IOException {
        settings.setDefaultAccountId(accountId);
    }

    public Optional<String> instanceAccountId(Path gameDirectory, String instanceId) throws IOException {
        return settings.instanceAccountId(gameDirectory, instanceId);
    }

    public void setInstanceAccountId(Path gameDirectory, String instanceId, String accountId)
            throws IOException {
        settings.setInstanceAccountId(gameDirectory, instanceId, accountId);
    }

    public AuthServerInfo fetchServer(String apiRoot) throws IOException, InterruptedException {
        return servers.fetch(apiRoot);
    }

    public List<String> presetApiRoots() {
        return servers.presetNames().keySet().stream().toList();
    }

    public String serverDisplayName(String apiRoot) {
        AuthServerInfo info = servers.fetchOrRemember(apiRoot);
        return info == null ? apiRoot : info.serverName();
    }

    public void setInjectorOverride(Path jar) {
        injectors.setOverride(jar);
    }

    public Optional<Path> injectorOverride() {
        return Optional.ofNullable(injectors.override());
    }

    // ------------------------------------------------------------------ 登录

    /**
     * 使用用户名 + 密码登录第三方认证服务器。
     *
     * @param remember 是否持久化 accessToken；为 {@code false} 时下次启动需要重新登录
     */
    public AccountSummary loginYggdrasil(String apiRoot, String username, String password,
                                         boolean remember)
            throws IOException, InterruptedException {
        String normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        if (username == null || username.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.usernameRequired"));
        }
        if (password == null || password.isEmpty()) {
            throw new BackendException(BackendError.of("auth.error.passwordRequired"));
        }

        AuthServerInfo info = servers.fetch(normalized);
        String clientToken = settings.clientToken();
        YggdrasilSession session = client.authenticate(normalized, username.trim(), password, clientToken);

        StoredAccount account = new StoredAccount(
                yggdrasilAccountId(session.profileId()),
                AccountType.YGGDRASIL,
                session.username(),
                normalizeUuid(session.profileId()),
                normalized,
                info.serverName(),
                remember ? session.accessToken() : "",
                blankToEmpty(session.clientToken()));

        store.upsert(account);
        if (remember) {
            LOGGER.info("已登录第三方账号 {} @ {}", session.username(), info.serverName());
        } else {
            LOGGER.info("已登录第三方账号 {} @ {}（未保存凭据）", session.username(), info.serverName());
        }
        return summary(account);
    }

    /** 使用微软设备码登录，并把 refresh token 加密保存到账号索引。 */
    public AccountSummary loginMicrosoft(Consumer<MicrosoftDeviceCode> deviceCodeListener)
            throws IOException, InterruptedException {
        MicrosoftSession session = microsoft.login(deviceCodeListener);
        String uuid = normalizeUuid(session.profileId().toString());
        StoredAccount account = new StoredAccount(
                "microsoft:" + uuid,
                AccountType.MICROSOFT,
                session.profileName(),
                uuid,
                "",
                "Microsoft",
                session.accessToken(),
                "",
                session.refreshToken(),
                session.expiresAt());
        store.upsert(account);
        LOGGER.info("已登录微软账号 {}", session.profileName());
        return summary(account);
    }

    /** 新增离线账号（仅名称，不访问网络）。 */
    public AccountSummary addOfflineAccount(String username) throws IOException {
        if (username == null || username.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.usernameRequired"));
        }
        String name = username.trim();
        String uuid = normalizeUuid(OfflineAccount.offlineUuid(name).toString());
        StoredAccount account = new StoredAccount(
                "offline:" + uuid, AccountType.OFFLINE, name, uuid, "", "", "", "");
        store.upsert(account);
        return summary(account);
    }

    /** 退出登录并删除账号；如果它是全局默认账号，同时清空默认设置。 */
    public void remove(String accountId) throws IOException {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        store.remove(accountId);
        if (settings.defaultAccountId().filter(accountId::equals).isPresent()) {
            settings.setDefaultAccountId(null);
        }
        settings.clearInstanceAccount(accountId);
    }

    // ------------------------------------------------------------------ 启动解析

    /**
     * 解析本次启动使用的账号：实例账号优先，其次全局默认账号。
     *
     * @return 没有可用账号时返回 {@code null}，由调用方回退到主页输入的离线用户名
     */
    public ResolvedAccount resolve(Path gameDirectory, String instanceId)
            throws IOException, InterruptedException {
        Optional<String> instanceAccount = gameDirectory == null || instanceId == null
                ? Optional.empty()
                : settings.instanceAccountId(gameDirectory, instanceId);
        String accountId = instanceAccount
                .or(() -> {
                    try {
                        return settings.defaultAccountId();
                    } catch (IOException e) {
                        return Optional.<String>empty();
                    }
                })
                .orElse(null);
        if (accountId == null || accountId.isBlank()) {
            return null;
        }

        StoredAccount account = store.find(accountId).orElseThrow(() -> new BackendException(
                BackendError.of("auth.error.accountMissing", accountId)));

        return resolveStored(account);
    }

    /** 按账号 id 直接解析；找不到账号时报结构化错误。 */
    public ResolvedAccount resolveById(String accountId) throws IOException, InterruptedException {
        if (accountId == null || accountId.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.accountRequired"));
        }
        StoredAccount account = store.find(accountId).orElseThrow(() -> new BackendException(
                BackendError.of("auth.error.accountMissing", accountId)));
        return resolveStored(account);
    }

    private ResolvedAccount resolveStored(StoredAccount account)
            throws IOException, InterruptedException {
        return switch (account.type()) {
            case OFFLINE -> new ResolvedAccount(
                    new OfflineAccount(account.displayName()).login(),
                    Optional.empty(),
                    summary(account));
            case YGGDRASIL -> resolveYggdrasil(account);
            case MICROSOFT -> resolveMicrosoft(account);
        };
    }

    private ResolvedAccount resolveYggdrasil(StoredAccount account)
            throws IOException, InterruptedException {
        if (!account.hasToken()) {
            throw new BackendException(BackendError.of(
                    "auth.error.loginRequired", account.displayName(), account.serverName()));
        }

        String accessToken = account.accessToken();
        String clientToken = account.clientToken();
        String profileId = account.uuid();
        try {
            // 直接 refresh 而不是先 validate：refresh 会带上 selectedProfile，
            // 这样“token 未绑定角色”（LittleSkin 多角色账号）的旧凭据也能在启动时自愈，
            // 否则 token 虽然有效，进服务器时仍会被 session 服务拒绝。
            Optional<YggdrasilSession> refreshed = client.refresh(
                    account.apiRoot(), accessToken, clientToken, profileId, account.displayName());
            if (refreshed.isPresent()) {
                YggdrasilSession session = refreshed.get();
                accessToken = session.accessToken();
                clientToken = blankToEmpty(session.clientToken());
                profileId = session.profileId();
                account = account.withToken(accessToken, clientToken);
                store.upsert(account);
                LOGGER.info("已刷新第三方账号 {} 的凭据", session.username());
            } else if (!client.validate(account.apiRoot(), accessToken, clientToken)) {
                // 服务器拒绝刷新且现有 token 也无效：只能重新登录。
                throw new BackendException(BackendError.of(
                        "auth.error.loginExpired", account.displayName(), account.serverName()));
            }
        } catch (BackendException e) {
            throw e;
        } catch (IOException e) {
            // 认证服务器暂时不可达时继续使用现有 token：
            // 单人生存不依赖它，多人服务器的校验由游戏自己完成。
            LOGGER.warn("无法校验第三方账号凭据，继续使用现有 token: {}", account.apiRoot(), e);
        }

        AuthServerInfo info = servers.fetchOrRemember(account.apiRoot());
        String prefetched = info == null ? "" : info.prefetchedBase64();
        AuthlibInjector injector = injectors.ensure(account.apiRoot(), prefetched);

        AuthSession session = new AuthSession(
                account.displayName(),
                YggdrasilSession.profileUuid(profileId),
                accessToken,
                "mojang",
                "{}");
        return new ResolvedAccount(session, Optional.of(injector), summary(account));
    }

    private ResolvedAccount resolveMicrosoft(StoredAccount account)
            throws IOException, InterruptedException {
        String accessToken = account.accessToken();
        boolean expired = accessToken.isBlank()
                || System.currentTimeMillis() >= account.expiresAt();
        if (expired) {
            if (account.refreshToken().isBlank()) {
                throw new BackendException(BackendError.of(
                        "auth.error.loginExpired", account.displayName(), account.serverName()));
            }
            MicrosoftSession refreshed;
            try {
                refreshed = microsoft.refresh(account.refreshToken());
            } catch (BackendException e) {
                if ("auth.microsoft.loginExpired".equals(e.error().code())) {
                    throw new BackendException(BackendError.of(
                            "auth.error.loginExpired", account.displayName(), account.serverName()), e);
                }
                throw e;
            }
            if (!normalizeUuid(refreshed.profileId().toString()).equals(account.uuid())) {
                throw new BackendException(BackendError.of("auth.microsoft.profileChanged"));
            }
            account = account.withMicrosoftTokens(
                    refreshed.profileName(), refreshed.accessToken(),
                    refreshed.refreshToken(), refreshed.expiresAt());
            store.upsert(account);
            accessToken = refreshed.accessToken();
            LOGGER.info("已刷新微软账号 {} 的凭据", refreshed.profileName());
        }

        AuthSession session = new AuthSession(
                account.displayName(),
                YggdrasilSession.profileUuid(account.uuid()),
                accessToken,
                "msa",
                "{}");
        return new ResolvedAccount(session, Optional.empty(), summary(account));
    }

    // ------------------------------------------------------------------ 工具

    private static AccountSummary summary(StoredAccount account) {
        return new AccountSummary(
                account.id(),
                account.displayName(),
                account.type(),
                account.uuid(),
                account.apiRoot(),
                account.serverName());
    }

    private static String yggdrasilAccountId(String profileId) {
        return "yggdrasil:" + normalizeUuid(profileId);
    }

    private static String normalizeUuid(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("-", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 一次启动解析出的账号：会话、需要注入的 agent（如果有）以及展示信息。 */
    public record ResolvedAccount(
            AuthSession session,
            Optional<AuthlibInjector> injector,
            AccountSummary account
    ) {
        public ResolvedAccount {
            Objects.requireNonNull(session, "session");
            injector = injector == null ? Optional.empty() : injector;
            Objects.requireNonNull(account, "account");
        }
    }
}
