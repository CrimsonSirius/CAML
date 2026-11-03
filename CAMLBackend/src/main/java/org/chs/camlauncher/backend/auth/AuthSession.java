package org.chs.camlauncher.backend.auth;

import java.util.Objects;
import java.util.UUID;

public record AuthSession(
        String username,
        UUID uuid,
        String accessToken,
        String userType,
        String userProperties
) {
    public AuthSession {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(userType, "userType");
        Objects.requireNonNull(userProperties, "userProperties");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
    }
}
