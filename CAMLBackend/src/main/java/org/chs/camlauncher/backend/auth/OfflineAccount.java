package org.chs.camlauncher.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record OfflineAccount(String username) implements Account {
    public OfflineAccount {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }

    @Override
    public AuthSession login() {
        return new AuthSession(
                username,
                offlineUuid(username),
                UUID.randomUUID().toString().replace("-", ""),
                "legacy",
                "{}"
        );
    }

    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        );
    }
}
