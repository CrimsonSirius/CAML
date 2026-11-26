package org.chs.camlauncher.front.view;

import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainViewOverlayTest {
    @Test
    void blockingOverlayCannotBeDismissedByScrimClick() {
        VBox warning = new VBox();
        warning.getStyleClass().add("app-overlay-blocking");
        VBox nonBlocking = new VBox();

        assertTrue(MainView.blocksOverlayDismissal(warning));
        assertFalse(MainView.blocksOverlayDismissal(nonBlocking));
    }
}
