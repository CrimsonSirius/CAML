package org.chs.camlauncher.front.controller;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 前端异步任务句柄。
 *
 * <p>取消会停止后续 UI 回调，并尽力中断后台线程；但后端已经开始且不响应中断的操作
 * 仍可能继续执行。页面只需要关心句柄状态，不再自行保存散落的 Future。</p>
 */
public final class FrontTask<T> {
    private final CompletableFuture<T> completion;
    private final Future<?> running;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    FrontTask(CompletableFuture<T> completion, Future<?> running) {
        this.completion = Objects.requireNonNull(completion, "completion");
        this.running = Objects.requireNonNull(running, "running");
    }

    /**
     * 请求取消任务。
     *
     * @return 是否成功取消；任务已完成或已取消时返回 {@code false}
     */
    public boolean cancel() {
        if (!completion.cancel(true)) {
            return false;
        }
        cancelled.set(true);
        running.cancel(true);
        return true;
    }

    public boolean isCancelled() {
        return cancelled.get() || completion.isCancelled();
    }

    public boolean isDone() {
        return completion.isDone();
    }

}
