package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.ui.control.CamlPageHost;
import org.chs.camlauncher.ui.theme.CamlMotion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamlPageHostSidebarTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void sidebarIsAddedToThePageSceneGraph() throws Exception {
        boolean attached = onFxThread(() -> {
            boolean previousReduceMotion = CamlMotion.reduceMotion();
            CamlMotion.setReduceMotion(true);
            try {
                CamlPageHost host = new CamlPageHost(new StackPane());
                VBox root = new VBox(host);
                Scene scene = new Scene(root, 800, 600);
                StackPane page = new StackPane();
                StackPane sidebar = new StackPane();

                host.setPage(page, sidebar);
                boolean first = sidebar.getScene() == scene && page.getScene() == scene;
                host.setPage(new StackPane());
                host.setPage(page, sidebar);
                return first && sidebar.getScene() == scene && page.getScene() == scene;
            } finally {
                CamlMotion.setReduceMotion(previousReduceMotion);
            }
        });

        assertTrue(attached);
    }
}
