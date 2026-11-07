package org.chs.camlauncher.backend.launch.command;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderResolverTest {
    @Test
    void replacesKnownPlaceholdersAndKeepsUnknownOnes() {
        String result = PlaceholderResolver.resolve(
                "${auth_player_name}-${version_name}-${unknown}",
                Map.of("auth_player_name", "Steve", "version_name", "1.20.1")
        );

        assertEquals("Steve-1.20.1-${unknown}", result);
    }
}
