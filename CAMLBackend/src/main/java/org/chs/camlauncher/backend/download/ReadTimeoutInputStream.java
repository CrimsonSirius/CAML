package org.chs.camlauncher.backend.download;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** 通过 watchdog 关闭长时间没有数据的响应流，避免 read 永久阻塞。 */
final class ReadTimeoutInputStream extends FilterInputStream {
    private static final ScheduledExecutorService WATCHDOG =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "caml-read-timeout");
                thread.setDaemon(true);
                return thread;
            });

    private final Duration timeout;
    private final ScheduledFuture<?> watchdog;
    private volatile long lastActivityNanos = System.nanoTime();
    private volatile boolean timedOut;
    private volatile boolean closed;

    ReadTimeoutInputStream(InputStream input, Duration timeout) {
        super(Objects.requireNonNull(input, "input"));
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long intervalMillis = Math.max(100, Math.min(1000, timeout.toMillis() / 4));
        watchdog = WATCHDOG.scheduleWithFixedDelay(
                this::checkTimeout, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int read() throws IOException {
        try {
            int value = super.read();
            touch(value);
            return value;
        } catch (IOException e) {
            throw translate(e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        try {
            int count = super.read(buffer, offset, length);
            touch(count);
            return count;
        } catch (IOException e) {
            throw translate(e);
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        watchdog.cancel(false);
        super.close();
    }

    private void touch(int count) {
        if (count > 0) {
            lastActivityNanos = System.nanoTime();
        }
    }

    private void checkTimeout() {
        if (closed || timedOut) {
            return;
        }
        if (System.nanoTime() - lastActivityNanos < timeout.toNanos()) {
            return;
        }
        timedOut = true;
        try {
            in.close();
        } catch (IOException ignored) {
            // 关闭失败仍会在下一次 read 时由底层流报告。
        }
    }

    private IOException translate(IOException cause) {
        if (!timedOut) {
            return cause;
        }
        SocketTimeoutException timeoutException = new SocketTimeoutException(
                "读取超时（" + timeout.toMillis() + "ms 没有收到数据）");
        timeoutException.initCause(cause);
        return timeoutException;
    }
}
