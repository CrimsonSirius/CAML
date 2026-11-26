package org.chs.camlauncher.front.controller;

import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerIsolationTest {
    @Test
    void mapsBooleanChoiceToExplicitMode() {
        assertEquals(VersionIsolationMode.FULL, HomeController.isolationMode(true));
        assertEquals(VersionIsolationMode.NONE, HomeController.isolationMode(false));
    }
}
