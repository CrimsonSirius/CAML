package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.language.LanguageService;
import org.chs.camlauncher.backend.settings.SettingsService;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlTextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadSettingsViewArchiveTest {
    @TempDir
    Path dataDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void resetsAndSavesArchiveLimits() throws Exception {
        SettingsService settings = new SettingsService(dataDirectory);
        LanguageService language = new LanguageService(dataDirectory, settings);
        language.initialize();
        UiText.initialize(language);
        AtomicReference<long[]> saved = new AtomicReference<>();

        onFxThread(() -> {
            DownloadSettingsView view = new DownloadSettingsView(
                    8, value -> {
                    },
                    DownloadSourceMode.MIRROR, "",
                    (mode, api) -> {
                    },
                    123, 64, 512,
                    (entries, entrySize, totalSize) -> saved.set(
                            new long[]{entries, entrySize, totalSize})
            );
            Scene scene = new Scene(view, 800, 600);
            view.applyCss();
            view.layout();

            CamlTextField entries = (CamlTextField) view.lookup("#archive-max-entries");
            CamlTextField entrySize = (CamlTextField) view.lookup("#archive-max-entry-mib");
            CamlTextField totalSize = (CamlTextField) view.lookup("#archive-max-total-mib");
            assertEquals("123", entries.getText());

            entries.setText("1");
            ((CamlButton) view.lookup("#archive-max-entries-reset")).fire();
            assertEquals(Integer.toString(SettingsService.DEFAULT_ARCHIVE_MAX_ENTRIES),
                    entries.getText());

            entries.setText("100");
            entrySize.setText("8");
            totalSize.setText("16");
            ((CamlButton) view.lookup("#archive-limits-save")).fire();
            assertTrue(view.lookup("#archive-limits-status").isVisible());
            return null;
        });

        assertEquals(100, saved.get()[0]);
        assertEquals(8, saved.get()[1]);
        assertEquals(16, saved.get()[2]);
    }

    @Test
    void invalidLimitsDoNotCallSave() throws Exception {
        SettingsService settings = new SettingsService(dataDirectory);
        LanguageService language = new LanguageService(dataDirectory, settings);
        language.initialize();
        UiText.initialize(language);
        AtomicReference<long[]> saved = new AtomicReference<>();

        onFxThread(() -> {
            DownloadSettingsView view = new DownloadSettingsView(
                    8, value -> {
                    },
                    DownloadSourceMode.MIRROR, "",
                    (mode, api) -> {
                    },
                    100, 64, 512,
                    (entries, entrySize, totalSize) -> saved.set(
                            new long[]{entries, entrySize, totalSize})
            );
            new Scene(view, 800, 600);
            view.applyCss();
            view.layout();
            ((CamlTextField) view.lookup("#archive-max-entries")).setText("100");
            ((CamlTextField) view.lookup("#archive-max-entry-mib")).setText("64");
            ((CamlTextField) view.lookup("#archive-max-total-mib")).setText("32");
            ((CamlButton) view.lookup("#archive-limits-save")).fire();
            assertTrue(view.lookup("#archive-limits-status").isVisible());
            return null;
        });

        assertNull(saved.get());
    }
}
