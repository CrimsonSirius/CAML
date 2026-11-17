package org.chs.camlauncher.backend.auth.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.AccountType;
import org.chs.camlauncher.backend.json.JsonService;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 账号索引的持久化：{@code ~/.caml/accounts/accounts.json}。
 *
 * <p>写入遵循项目既有约定：临时文件 + 原子替换，读失败时返回空列表而不是让启动失败。
 * accessToken 由 {@link SecretCipher} 加密后落盘，文件损坏时只丢弃该 token，
 * 账号条目仍保留（用户重新登录即可），不会静默删除用户配置。</p>
 */
public final class AccountStore {
    private static final Logger LOGGER = LogManager.getLogger(AccountStore.class);
    private static final int FILE_VERSION = 1;

    private final Path accountsFile;
    private final SecretCipher cipher;
    private final JsonService json;
    private final Object writeLock = new Object();

    public AccountStore(Path dataDirectory, JsonService json) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Path directory = dataDirectory.resolve("accounts");
        this.accountsFile = directory.resolve("accounts.json");
        this.cipher = new SecretCipher(directory.resolve("secret.key"));
        this.json = Objects.requireNonNull(json, "json");
    }

    public Path accountsFile() {
        return accountsFile;
    }

    public List<StoredAccount> load() throws IOException {
        synchronized (writeLock) {
            return read();
        }
    }

    public Optional<StoredAccount> find(String id) throws IOException {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return load().stream().filter(account -> account.id().equals(id)).findFirst();
    }

    /** 新增或按 id 覆盖已有账号。 */
    public void upsert(StoredAccount account) throws IOException {
        Objects.requireNonNull(account, "account");
        synchronized (writeLock) {
            List<StoredAccount> accounts = new ArrayList<>(read());
            accounts.removeIf(existing -> existing.id().equals(account.id()));
            accounts.add(account);
            write(accounts);
        }
    }

    public void remove(String id) throws IOException {
        if (id == null || id.isBlank()) {
            return;
        }
        synchronized (writeLock) {
            List<StoredAccount> accounts = new ArrayList<>(read());
            if (accounts.removeIf(existing -> existing.id().equals(id))) {
                write(accounts);
            }
        }
    }

    private List<StoredAccount> read() throws IOException {
        if (!Files.isRegularFile(accountsFile)) {
            return List.of();
        }

        AccountsFile file;
        try {
            file = json.read(accountsFile, AccountsFile.class);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("账号索引损坏，按空列表处理: {}", accountsFile, e);
            return List.of();
        }
        if (file == null || file.accounts == null) {
            return List.of();
        }

        List<StoredAccount> accounts = new ArrayList<>(file.accounts.size());
        for (AccountEntry entry : file.accounts) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            accounts.add(new StoredAccount(
                    entry.id,
                    AccountType.fromStored(entry.type),
                    entry.displayName,
                    entry.uuid,
                    entry.apiRoot,
                    entry.serverName,
                    cipher.decrypt(entry.accessToken),
                    entry.clientToken));
        }
        return List.copyOf(accounts);
    }

    private void write(List<StoredAccount> accounts) throws IOException {
        AccountsFile file = new AccountsFile();
        file.version = FILE_VERSION;
        file.accounts = new ArrayList<>(accounts.size());
        for (StoredAccount account : accounts) {
            AccountEntry entry = new AccountEntry();
            entry.id = account.id();
            entry.type = account.type().storedName();
            entry.displayName = account.displayName();
            entry.uuid = account.uuid();
            entry.apiRoot = account.apiRoot();
            entry.serverName = account.serverName();
            entry.accessToken = cipher.encrypt(account.accessToken());
            entry.clientToken = account.clientToken();
            file.accounts.add(entry);
        }

        Path parent = accountsFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "caml-accounts-", ".tmp");
        try {
            json.write(temporary, file);
            try {
                Files.move(temporary, accountsFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, accountsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class AccountsFile {
        private int version = FILE_VERSION;
        private List<AccountEntry> accounts = new ArrayList<>();
    }

    private static final class AccountEntry {
        private String id;
        private String type;
        private String displayName;
        private String uuid;
        private String apiRoot;
        private String serverName;
        private String accessToken;
        private String clientToken;
    }
}

