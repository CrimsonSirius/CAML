package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceInstallLockTest {
    @TempDir
    Path gameDirectory;

    @Test
    void allowsReentrantAcquisitionInSameThread() throws Exception {
        try (InstanceInstallLock outer = InstanceInstallLock.acquire(gameDirectory, "instance");
             InstanceInstallLock inner = InstanceInstallLock.acquire(gameDirectory, "instance")) {
            assertTrue(outer != inner);
        }
    }

    @Test
    void blocksAnotherThreadUntilReleased() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch acquired = new CountDownLatch(1);
        InstanceInstallLock lock = InstanceInstallLock.acquire(gameDirectory, "instance");
        boolean released = false;
        try {
            Future<?> other = executor.submit(() -> {
                try (InstanceInstallLock second = InstanceInstallLock.acquire(
                        gameDirectory, "instance")) {
                    acquired.countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertFalse(acquired.await(200, TimeUnit.MILLISECONDS));
            lock.close();
            released = true;
            assertTrue(acquired.await(5, TimeUnit.SECONDS));
            other.get(5, TimeUnit.SECONDS);
        } finally {
            if (!released) {
                lock.close();
            }
            executor.shutdownNow();
        }
    }
}
