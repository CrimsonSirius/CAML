package org.chs.camlauncher.backend.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilProfile;
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilSession;
import org.chs.camlauncher.backend.auth.yggdrasil.YggdrasilUnavailableException;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.settings.SettingsService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 账号编排：账号列表、Yggdrasil 登录、角色切换，以及“实例账号 → 全局账号”的解析。
 *
 * <p>所有网络与磁盘操作都在后端完成；前端只拿到 {@link AccountSummary} 和
 * {@link ResolvedAccount}，永远不接触 token。</p>
 *
 * <p><b>不记住登录</b>：勾选“记住登录”时才把 accessToken 加密落盘；否则 token 只保存在
 * {@link #transientSessions}（进程内），本次运行期间可以正常启动游戏，重启后要求重新登录。</p>
 */
public final class AccountService {
    private static final Logger LOGGER = LogManager.getLogger(AccountService.class);

    private final AccountStore store;
    private final AuthServerService servers;
    private final AuthlibInjectorService injectors;
    private final SettingsService settings;
    private final YggdrasilClient client;
    private final MicrosoftAuthService microsoft;
    /** 未持久化凭据的会话：账号 id → 带 token 的账号。 */
    private final Map<String, StoredAccount> transientSessions = new ConcurrentHashMap<>();

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
        return store.find(accountId).map(this::summary);
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

    /**
     * 找出“绑定留在旧游戏目录”的实例。
     *
     * <p>实例账号绑定以“游戏目录 + 实例名”为键，修改“游戏位置”后旧绑定不会自动生效。
     * 这里只报告<b>当前目录里也存在</b>的实例，避免把已经不需要的旧条目都算进来；
     * 返回空表示没有需要提示的情况。</p>
     *
     * @param gameDirectory     当前游戏目录
     * @param currentInstances  当前游戏目录里已安装的实例
     */
    public Optional<InstanceAccountMismatch> detectInstanceAccountMismatch(
            Path gameDirectory, List<String> currentInstances) throws IOException {
        if (gameDirectory == null || currentInstances == null || currentInstances.isEmpty()) {
            return Optional.empty();
        }
        Path current = gameDirectory.toAbsolutePath().normalize();
        List<SettingsService.InstanceAccountBinding> bindings = settings.instanceAccountBindings();

        List<String> affected = new ArrayList<>();
        Path previous = null;
        for (String instanceId : currentInstances) {
            if (instanceId == null || instanceId.isBlank()) {
                continue;
            }
            for (SettingsService.InstanceAccountBinding binding : bindings) {
                if (!instanceId.equals(binding.instanceId())) {
                    continue;
                }
                if (binding.gameDirectory().equals(current)) {
                    continue;
                }
                affected.add(instanceId);
                // 多个旧目录时展示最近匹配到的那个；一键套用会对每个实例分别回溯。
                previous = binding.gameDirectory();
                break;
            }
        }
        if (affected.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new InstanceAccountMismatch(previous, affected));
    }

    /**
     * 把旧游戏目录下仍可用的实例绑定套用到当前游戏目录。
     *
     * @return 实际套用的绑定数量
     */
    public int reapplyInstanceAccounts(Path gameDirectory, List<String> currentInstances)
            throws IOException {
        if (gameDirectory == null || currentInstances == null || currentInstances.isEmpty()) {
            return 0;
        }
        Path current = gameDirectory.toAbsolutePath().normalize();
        Map<Path, List<String>> byDirectory = new LinkedHashMap<>();
        for (SettingsService.InstanceAccountBinding binding : settings.instanceAccountBindings()) {
            if (binding.gameDirectory().equals(current)) {
                continue;
            }
            if (!currentInstances.contains(binding.instanceId())) {
                continue;
            }
            byDirectory.computeIfAbsent(binding.gameDirectory(), key -> new ArrayList<>())
                    .add(binding.instanceId());
        }
        int moved = 0;
        for (Map.Entry<Path, List<String>> entry : byDirectory.entrySet()) {
            moved += settings.moveInstanceAccountBindings(entry.getKey(), current, entry.getValue());
        }
        return moved;
    }

    public AuthServerInfo fetchServer(String apiRoot) throws IOException, InterruptedException {
        return servers.fetch(apiRoot);
    }

    public List<String> presetApiRoots() {
        return List.copyOf(servers.presetNames().keySet());
    }

    /** 预设服务器地址 → 展示名，供下拉框显示服务器名称而不是裸地址。 */
    public Map<String, String> presetServerNames() {
        return servers.presetNames();
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
     * @param remember 是否持久化 accessToken；为 {@code false} 时本次运行可用，
     *                 重启后需要重新登录
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

        // metadata 只是扩展信息：拿不到时用主机名兜底，凭据登录本身不受影响。
        AuthServerInfo info = servers.fetchOrRemember(normalized);
        String clientToken = settings.clientToken();
        YggdrasilSession session = client.authenticate(normalized, username.trim(), password, clientToken);

        StoredAccount account = new StoredAccount(
                yggdrasilAccountId(session.profileId()),
                AccountType.YGGDRASIL,
                session.username(),
                normalizeUuid(session.profileId()),
                normalized,
                info == null ? hostOf(normalized) : info.serverName(),
                remember ? session.accessToken() : "",
                blankToEmpty(session.clientToken()),
                session.profiles());

        if (remember) {
            transientSessions.remove(account.id());
        } else {
            transientSessions.put(account.id(),
                    account.withToken(session.accessToken(), blankToEmpty(session.clientToken())));
        }
        store.upsert(account);
        LOGGER.info("已登录第三方账号 {} @ {}{}", session.username(),
                account.serverName(), remember ? "" : "（未保存凭据）");
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
        transientSessions.remove(account.id());
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
        transientSessions.remove(accountId);
        store.remove(accountId);
        if (settings.defaultAccountId().filter(accountId::equals).isPresent()) {
            settings.setDefaultAccountId(null);
        }
        settings.clearInstanceAccount(accountId);
    }

    /**
     * 把账号的 accessToken 重新绑定到另一个角色。
     *
     * <p>账号 id 保持不变，只更新显示名、UUID 与角色列表；切换后立即把新 token 写回，
     * 避免下次启动再绑定一次。</p>
     */
    public AccountSummary switchProfile(String accountId, String profileId)
            throws IOException, InterruptedException {
        StoredAccount account = store.find(accountId).orElseThrow(() -> new BackendException(
                BackendError.of("auth.error.accountMissing", accountId)));
        if (account.type() != AccountType.YGGDRASIL) {
            throw new BackendException(BackendError.of("auth.error.accountRequired"));
        }
        YggdrasilProfile target = account.profilesOrDefault().stream()
                .filter(profile -> profile.sameId(profileId))
                .findFirst()
                .orElseThrow(() -> new BackendException(BackendError.of(
                        "auth.error.profileMissing", profileId)));

        StoredAccount effective = transientSessions.getOrDefault(account.id(), account);
        if (!effective.hasToken()) {
            throw new BackendException(BackendError.of(
                    "auth.error.loginRequired", account.displayName(), account.serverName()));
        }
        Optional<YggdrasilSession> refreshed = client.refresh(account.apiRoot(),
                effective.accessToken(), effective.clientToken(), target.id(), target.name());
        if (refreshed.isEmpty()) {
            throw new BackendException(BackendError.of(
                    "auth.error.loginExpired", account.displayName(), account.serverName()));
        }
        YggdrasilSession session = refreshed.get();
        StoredAccount updated = persistSession(account, session);
        LOGGER.info("已为账号 {} 切换角色: {}", account.id(), updated.displayName());
        return summary(updated);
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
        return resolveById(accountId);
    }

    /** 按账号 id 直接解析；找不到账号时报结构化错误。 */
    public ResolvedAccount resolveById(String accountId) throws IOException, InterruptedException {
        if (accountId == null || accountId.isBlank()) {
            throw new BackendException(BackendError.of("auth.error.accountRequired"));
        }
        StoredAccount account = store.find(accountId).orElseThrow(() -> new BackendException(
                BackendError.of("auth.error.accountMissing", accountId)));
        // 未记住登录的账号只在内存里有 token，这里取出完整会话。
        return resolveStored(transientSessions.getOrDefault(account.id(), account));
    }

    private ResolvedAccount resolveStored(StoredAccount account)
            throws IOException, InterruptedException {
        return switch (account.type()) {
            case OFFLINE -> new ResolvedAccount(
                    new OfflineAccount(account.displayName()).login(), Optional.empty(),
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
        String profileName = account.displayName();
        try {
            // 直接 refresh 而不是先 validate：refresh 会带上 selectedProfile，
            // 这样“token 未绑定角色”（LittleSkin 多角色账号）的旧凭据也能在启动时自愈，
            // 否则 token 虽然有效，进服务器时仍会被 session 服务拒绝。
            Optional<YggdrasilSession> refreshed = client.refresh(
                    account.apiRoot(), accessToken, clientToken, profileId, profileName);
            if (refreshed.isPresent()) {
                YggdrasilSession session = refreshed.get();
                accessToken = session.accessToken();
                clientToken = blankToEmpty(session.clientToken());
                profileId = session.profileId();
                profileName = session.username();
                // 角色名可能在服务器侧被修改：把最新角色信息一并写回，
                // 否则下次启动仍会用旧名字拼接启动参数。
                account = persistSession(account, session);
                LOGGER.info("已刷新第三方账号 {} 的凭据", session.username());
            } else if (!client.validate(account.apiRoot(), accessToken, clientToken)) {
                // 服务器拒绝刷新且现有 token 也无效：只能重新登录。
                throw new BackendException(BackendError.of(
                        "auth.error.loginExpired", account.displayName(), account.serverName()));
            }
        } catch (YggdrasilUnavailableException e) {
            // 认证服务器暂时不可用（5xx、限流、网络失败）时继续使用现有 token：
            // 单人生存不依赖它，多人服务器的校验由游戏自己完成。
            LOGGER.warn("无法刷新第三方账号凭据，继续使用现有 token: {}", account.apiRoot(), e);
        } catch (BackendException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("无法校验第三方账号凭据，继续使用现有 token: {}", account.apiRoot(), e);
        }

        AuthServerInfo info = servers.fetchOrRemember(account.apiRoot());
        String prefetched = info == null ? "" : info.prefetchedBase64();
        AuthlibInjector injector = injectors.ensure(account.apiRoot(), prefetched);

        AuthSession session = new AuthSession(
                profileName,
                YggdrasilSession.profileUuid(profileId),
                accessToken,
                "mojang",
                "{}");
        return new ResolvedAccount(session, Optional.of(injector),
                summary(transientSessions.getOrDefault(account.id(), account)));
    }

    /**
     * 把刷新结果（token + 最新角色信息）写回账号。
     *
     * <p>未记住登录的账号只更新内存会话，绝不把 token 落盘。</p>
     */
    private StoredAccount persistSession(StoredAccount account, YggdrasilSession session)
            throws IOException {
        StoredAccount updated = account.withProfile(
                session.selectedProfile(), session.profiles(),
                session.accessToken(), blankToEmpty(session.clientToken()));
        if (transientSessions.containsKey(account.id())) {
            transientSessions.put(account.id(), updated);
        } else {
            store.upsert(updated);
        }
        return updated;
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

    private AccountSummary summary(StoredAccount account) {
        boolean hasToken = account.hasToken() || transientSessions.containsKey(account.id());
        return new AccountSummary(
                account.id(),
                account.displayName(),
                account.type(),
                account.uuid(),
                account.apiRoot(),
                account.serverName(),
                account.profilesOrDefault(),
                account.type() == AccountType.YGGDRASIL && !hasToken);
    }

    private static String yggdrasilAccountId(String profileId) {
        return "yggdrasil:" + normalizeUuid(profileId);
    }

    private static String normalizeUuid(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String hostOf(String apiRoot) {
        String host = URI.create(apiRoot).getHost();
        return host == null ? apiRoot : host;
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
