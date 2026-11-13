package org.chs.camlauncher.front.view;

import javafx.application.Platform;
import javafx.scene.Scene;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界面测试的 JavaFX 工具支持。
 *
 * <p>JavaFX 工具包在每个 JVM 中只能启动一次，因此多个测试类共享这里的启动逻辑；
 * 测试结束时也不退出工具包，避免后续测试类再次 {@code Platform.startup} 失败。</p>
 */
final class FxTestSupport {
    private static boolean started;

    private FxTestSupport() {
    }

    /** 首次调用时启动 JavaFX 工具包，之后复用同一个工具包。 */
    static synchronized void ensureStarted() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            // 其他测试类已经启动过工具包，直接复用。
            started = true;
            return;
        }
        assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX 工具包启动超时");
        started = true;
    }

    /** 在 JavaFX 线程执行布局动作，并把异常带回测试线程。 */
    static <T> T onFxThread(Supplier<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX 布局超时");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }

    /** 挂载 CAML 真实样式表（结构样式由 {@code CamlTheme.install} 负责）。 */
    static void addStylesheet(Scene scene, String path) {
        URL url = FxTestSupport.class.getResource(path);
        assertNotNull(url, "缺少样式表 " + path);
        scene.getStylesheets().add(url.toExternalForm());
    }
}

