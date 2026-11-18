package org.chs.camlauncher.backend.auth.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.auth.yggdrasil.AuthServerInfo;
import org.chs.camlauncher.backend.json.JsonService;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 认证服务器 metadata 的磁盘缓存：{@code ~/.caml/accounts/servers.json}。
 *
 * <p>{@code AuthServerService} 的内存缓存只在进程内有效，冷启动时皮肤站不可用就会
 * 失去 {@code prefetched}。把 metadata 落到磁盘后，回退顺序变成
 * “网络 → 内存 → 磁盘”，皮肤站维护期间仍能正常启动并加载皮肤。</p>
 *
 * <p>写入遵循项目既有约定：临时文件 + 原子替换；读取失败按“没有缓存”处理，
 * 不会因为缓存损坏而阻塞启动。</p>
 */
public final class AuthServerStore {
    private static final Logger LOGGER = LogManager.getLogger(AuthServerStore.class);
    private static final int FILE_VERSION = 1;

    private final Path file;
    private final JsonService json;
    private final Object writeLock = new Object();

    public AuthServerStore(Path dataDirectory, JsonService json) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.file = dataDirectory.resolve("accounts").resolve("servers.json");
        this.json = Objects.requireNonNull(json, "json");
    }

    public Path file() {
        return file;
    }

    public Optional<AuthServerInfo> find(String apiRoot) {
        String normalized;
        try {
            normalized = AuthServerInfo.normalizeApiRoot(apiRoot);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        synchronized (writeLock) {
            ServerEntry entry = read().servers.get(normalized);
            if (entry == null || entry.rawMetadata == null || entry.rawMetadata.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AuthServerInfo(
                    normalized,
                    entry.serverName,
                    entry.skinDomains,
                    entry.rawMetadata));
        }
    }

    public void save(AuthServerInfo info) {
        if (info == null) {
            return;
        }
        synchronized (writeLock) {
            ServersFile data = read();
            ServerEntry entry = new ServerEntry();
            entry.serverName = info.serverName();
            entry.skinDomains = info.skinDomains();
            entry.rawMetadata = info.rawMetadata();
            data.servers.put(info.apiRoot(), entry);
            try {
                write(data);
            } catch (IOException e) {
                // 缓存写入失败不影响本次登录/启动，只是下次要重新联网。
                LOGGER.warn("无法写入认证服务器缓存: {}", file, e);
            }
        }
    }

    private ServersFile read() {
        ServersFile data = null;
        if (Files.isRegularFile(file)) {
            try {
                data = json.read(file, ServersFile.class);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("认证服务器缓存损坏，按空缓存处理: {}", file, e);
            }
        }
        if (data == null) {
            data = new ServersFile();
        }
        if (data.servers == null) {
            data.servers = new LinkedHashMap<>();
        }
        return data;
    }

    private void write(ServersFile data) throws IOException {
        data.version = FILE_VERSION;
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "caml-servers-", ".tmp");
        try {
            json.write(temporary, data);
            try {
                Files.move(temporary, file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class ServersFile {
        private int version = FILE_VERSION;
        private Map<String, ServerEntry> servers = new LinkedHashMap<>();
    }

    private static final class ServerEntry {
        private String serverName;
        private List<String> skinDomains;
        private String rawMetadata;
    }
}

