package org.chs.camlauncher.front;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.theme.ThemeInfo;
import org.chs.camlauncher.front.controller.MainController;
import org.chs.camlauncher.front.view.MainView;
import org.chs.camlauncher.ui.theme.CamlTheme;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    private List<ThemeInfo> themes = List.of();
    private ThemeInfo activeTheme;

    @Override
    public void init() throws Exception {
        logger = LogManager.getLogger(CAMLApplication.class);

        // 1. 初始化后端。
        // 注意：目录创建由 backend.start() 内部的 BackendPaths.ensure(...) 完成,
        // 前端不再直接读写文件，符合前后端职责分离要求。
        BackendConfig config = BackendConfig.defaults();
        backend = new CAMLBackend(config);
        backend.start();

        try {
            themes = backend.listThemes();
            activeTheme = backend.activeTheme();
        } catch (IOException e) {
            logger.warn("无法加载主题，将使用组件默认结构样式", e);
        }

        logger.info("CAML frontend initialized");
    }

    @Override
    public void start(Stage stage) {
        // 2. 组装 Controller 和 View
        controller = new MainController(backend);
        MainView view = new MainView(controller);

        // 3. 按主屏幕可用区域决定窗口尺寸：小屏不溢出，大屏不过小，默认居中
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WindowSizePlanner.WindowMetrics metrics = WindowSizePlanner.compute(
                screen.getMinX(), screen.getMinY(), screen.getWidth(), screen.getHeight());
        WindowSizePlanner.WindowGeometry window = metrics.window();

        // 4. 设置场景：结构样式内置，配色来自 <data>/themes/*.css
        Scene scene = new Scene(view, window.width(), window.height());
        CamlTheme.install(scene);
        if (activeTheme != null) {
            try {
                CamlTheme.applyTheme(scene, activeTheme.file());
            } catch (RuntimeException e) {
                logger.warn("无法应用主题: {}", activeTheme.file(), e);
            }
        }
        view.configureThemes(themes, activeTheme, theme -> switchTheme(scene, theme));

        // 加载全局样式；资源缺失时退化为内联样式，避免影响启动。
        try {
            scene.getStylesheets().add(Objects.requireNonNull(
                    CAMLApplication.class.getResource("/styles/app.css")
            ).toExternalForm());
        } catch (Exception ignored) {
            logger.warn("未找到 /styles/app.css，使用纯内联样式。");
        }

        stage.setTitle("CAML");
        stage.setMinWidth(metrics.minWidth());
        stage.setMinHeight(metrics.minHeight());
        stage.setScene(scene);
        stage.setWidth(window.width());
        stage.setHeight(window.height());
        stage.setX(window.x());
        stage.setY(window.y());
        stage.show();

        logger.info("窗口尺寸 {}x{}（屏幕可用区域 {}x{}）",
                (long) window.width(), (long) window.height(),
                (long) screen.getWidth(), (long) screen.getHeight());
    }

    /** 立即应用主题，并把选择写入 ~/.caml/theme.txt。 */
    private void switchTheme(Scene scene, ThemeInfo theme) {
        try {
            CamlTheme.applyTheme(scene, theme.file());
        } catch (RuntimeException e) {
            logger.warn("无法应用主题: {}", theme.file(), e);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                backend.selectTheme(theme.id());
            } catch (IOException e) {
                logger.warn("保存主题选择失败: {}", theme.id(), e);
            }
        });
    }

    @Override
    public void stop() {
        // 5. 清理资源：先停前端后台线程，再关闭后端
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