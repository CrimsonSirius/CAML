package org.chs.camlauncher.backend.download;

import java.util.concurrent.atomic.AtomicBoolean;

/** 下载取消令牌；取消是幂等的，并会传播到文件下载和并发任务。 */
public final class DownloadCancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    /** 已取消时抛出 {@link InterruptedException}，由安装或下载调用方清理临时文件。 */
    public void throwIfCancelled() throws InterruptedException {
        if (cancelled.get()) {
            throw new InterruptedException("下载已取消");
        }
    }
}
