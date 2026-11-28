package org.chs.camlauncher.backend.api;

import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.instance.InstanceInstallLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceContentLockTest {
    @TempDir
    Path root;

    @Test
    void modToggleWaitsForInstanceLock() throws Exception {
        Path game = root.resolve("game");
        Path mods = game.resolve("versions/1.21.4/mods");
        Files.createDirectories(mods);
        Files.writeString(mods.resolve("demo.jar"), "jar");
        CAMLBackend backend = new CAMLBackend(new BackendConfig(root.resolve("data"), game));

        try (InstanceInstallLock lock = InstanceInstallLock.acquire(game, "1.21.4");
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertBlocks(lock, executor,
                    () -> backend.instances().setModDisabled(
                            game, "1.21.4", "demo.jar", true));
        }

        assertFalse(Files.exists(mods.resolve("demo.jar")));
        assertTrue(Files.isRegularFile(mods.resolve("demo.jar.disabled")));
    }

    @Test
    void resourcePackToggleWaitsForInstanceLock() throws Exception {
        Path game = root.resolve("game");
        Path packs = game.resolve("versions/1.21.4/resourcepacks");
        Files.createDirectories(packs);
        Files.writeString(packs.resolve("demo.zip"), "zip");
        CAMLBackend backend = new CAMLBackend(new BackendConfig(root.resolve("data"), game));

        try (InstanceInstallLock lock = InstanceInstallLock.acquire(game, "1.21.4");
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertBlocks(lock, executor,
                    () -> backend.instances().setResourcePackDisabled(
                            game, "1.21.4", "demo.zip", true));
        }

        assertFalse(Files.exists(packs.resolve("demo.zip")));
        assertTrue(Files.isRegularFile(packs.resolve("demo.zip.disabled")));
    }

    private static void assertBlocks(InstanceInstallLock lock, ExecutorService executor,
                                     Action operation) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Future<?> future = executor.submit(() -> {
            started.countDown();
            try {
                operation.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> future.get(200, TimeUnit.MILLISECONDS));
        lock.close();
        future.get(5, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }
}
