package org.chs.camlauncher.front;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;

import java.util.Objects;

public final class CAMLApplication extends Application {
    private CAMLBackend backend;
    private Logger logger;

    @Override
    public void init() throws Exception {
        BackendConfig config = BackendConfig.defaults();
        BackendPaths.ensure(config);

        backend = new CAMLBackend(config);
        backend.start();

        logger = LogManager.getLogger(CAMLApplication.class);
        logger.info("CAML frontend initialized");
    }

    @Override
    public void start(Stage stage) {
        MainView view = new MainView(backend.status());

        Scene scene = new Scene(view, 960, 600);
        scene.getStylesheets().add(Objects.requireNonNull(
                CAMLApplication.class.getResource("/styles/app.css"),
                "Missing /styles/app.css"
        ).toExternalForm());

        stage.setTitle("CAML");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (backend != null) {
            backend.close();
        }
        if (logger != null) {
            logger.info("CAML frontend stopped");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
