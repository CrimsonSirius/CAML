package org.chs.camlauncher.front.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchUiStateTest {
    @Test
    void exposesStableTextKeysAndDisabledState() {
        assertEquals("home.launch", LaunchUiState.IDLE.textKey());
        assertEquals("home.launching", LaunchUiState.PREPARING.textKey());
        assertEquals("home.running", LaunchUiState.RUNNING.textKey());
        assertFalse(LaunchUiState.IDLE.disablesLaunch());
        assertTrue(LaunchUiState.PREPARING.disablesLaunch());
        assertTrue(LaunchUiState.RUNNING.disablesLaunch());
    }
}
