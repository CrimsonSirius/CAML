package org.chs.camlauncher.backend.auth.microsoft;

import java.util.Objects;
import java.util.UUID;

/** 一次微软登录得到的 Minecraft 会话。 */
public record MicrosoftSession(
        String profileName,
        UUID profileId,
        String accessToken,
        String refreshToken,
        long expiresAt
) {
    public MicrosoftSession {
        Objects.requireNonNull(profileName, "profileName");
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(refreshToken, "refreshToken");
        if (profileName.isBlank()) {
            throw new IllegalArgumentException("profileName must not be blank");
        }
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
    }

    @Override
    public String toString() {
        return "MicrosoftSession[profileName=" + profileName
                + ", profileId=" + profileId
                + ", expiresAt=" + expiresAt
                + "]";
    }
}
