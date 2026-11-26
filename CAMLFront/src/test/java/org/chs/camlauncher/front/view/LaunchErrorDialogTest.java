package org.chs.camlauncher.front.view;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.front.text.UiText;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchErrorDialogTest {
    @TempDir
    Path dataDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void offersRetryAndClose() throws Exception {
        SettingsService settings = new SettingsService(dataDirectory);
        LanguageService language = new LanguageService(dataDirectory, settings);
        language.initialize();
        UiText.initialize(language);

        Dialog<ButtonType> dialog = onFxThread(() -> LaunchErrorDialog.create("boom"));

        assertEquals(2, dialog.getDialogPane().getButtonTypes().size());
        assertTrue(dialog.getDialogPane().getButtonTypes().stream()
                .anyMatch(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE));
        assertTrue(dialog.getDialogPane().getButtonTypes().stream()
                .anyMatch(type -> type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE));
    }
}
