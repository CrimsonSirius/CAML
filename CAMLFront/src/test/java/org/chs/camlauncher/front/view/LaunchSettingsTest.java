package org.chs.camlauncher.front.view;

import org.chs.camlauncher.backend.runtime.JavaSelectionMode;
import org.chs.camlauncher.front.text.AccountChoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchSettingsTest {
    @Test
    void copyPreservesOfflineUsernameAndAccountChoice() {
        LaunchSettings settings = new LaunchSettings(
                8192,
                Path.of("/games/.minecraft"),
                true,
                "1.21.4",
                JavaSelectionMode.AUTO,
                null,
                true,
                "-Dfoo=bar"
        );
        settings.setOfflineUsername("Steve");
        settings.setSelectedAccountChoice("account:123");

        LaunchSettings copy = settings.copy();

        assertEquals("Steve", copy.offlineUsername());
        assertEquals("account:123", copy.selectedAccountChoice());
        assertTrue(copy.fullVersionIsolation());
        assertEquals(8192, copy.maxMemoryMb());
        assertEquals("-Dfoo=bar", copy.jvmArguments());
    }

    @Test
    void copyKeepsDefaultAccountChoice() {
        LaunchSettings settings = new LaunchSettings(
                4096, Path.of("/game"), false, "1.20.1",
                JavaSelectionMode.AUTO, null, true, "");

        assertEquals(AccountChoice.DEFAULT, settings.copy().selectedAccountChoice());
    }
}
