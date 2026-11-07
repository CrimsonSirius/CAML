package org.chs.camlauncher.front.controller;

import org.chs.camlauncher.backend.api.BackendStatus;
import org.chs.camlauncher.backend.api.CAMLBackend;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 前端控制器组合入口。
 *
 * <p>与 {@code MainView} 的壳层思路一致：只负责组合主页、下载、设置三个页面控制器，
 * 并提供应用级状态和关闭流程。具体交互由各页面控制器承担。</p>
 */
public final class MainController {
    private final ControllerContext context;
    private final HomeController homeController;
    private final DownloadsController downloadsController;
    private final SettingsController settingsController;

    public MainController(CAMLBackend backend) {
        context = new ControllerContext(Objects.requireNonNull(backend, "backend"));
        homeController = new HomeController(context);
        downloadsController = new DownloadsController(context);
        settingsController = new SettingsController(context);
    }

    public HomeController homeController() {
        return homeController;
    }

    public DownloadsController downloadsController() {
        return downloadsController;
    }

    public SettingsController settingsController() {
        return settingsController;
    }

    public Path defaultGameDirectory() {
        return settingsController.defaultGameDirectory();
    }

    public BackendStatus status() {
        return context.backend().status();
    }

    public int downloadConcurrency() {
        return settingsController.downloadConcurrency();
    }

    public void shutdown() {
        context.shutdown();
        settingsController.shutdown();
    }
}
