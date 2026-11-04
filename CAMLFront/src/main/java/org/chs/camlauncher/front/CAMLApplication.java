package org.chs.camlauncher.front;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.front.view.MainView;

import java.util.Objects;

/**
 * JavaFX 应用入口。
 *
 * <p>CAMLFront 只负责界面展示和交互。配置、目录创建、网络和进程操作全部由
 * {@link CAMLBackend} 负责，前端不直接调用文件系统 API。</p>
 */
public final class CAMLApplication extends Application {
    private CAMLBackend backend;
    private MainController controller;
    private Logger logger;

    @Override
    public void init() throws Exception {
        logger = LogManager.getLogger(CAMLApplication.class);

        // 1. 初始化后端。
        // 注意：目录创建由 backend.start() 内部的 BackendPaths.ensure(...) 完成,
        // 前端不再直接读写文件，符合前后端职责分离要求。
        BackendConfig config = BackendConfig.defaults();
        backend = new CAMLBackend(config);
        backend.start();

        logger.info("CAML frontend initialized");
    }

    @Override
    public void start(Stage stage) {
        // 2. 组装 Controller 和 View
        controller = new MainController(backend);
        MainView view = new MainView(controller);

        // 3. 设置场景
        Scene scene = new Scene(view, 960, 720);

        // 加载全局样式；资源缺失时退化为内联样式，避免影响启动。
        try {
            scene.getStylesheets().add(Objects.requireNonNull(
                    CAMLApplication.class.getResource("/styles/app.css")
            ).toExternalForm());
        } catch (Exception ignored) {
            logger.warn("未找到 /styles/app.css，使用纯内联样式。");
        }

        stage.setTitle("CAML");
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // 4. 清理资源：先停前端后台线程，再关闭后端
        if (controller != null) {
            controller.shutdown();
        }
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