package org.chs.camlauncher.front.task;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncTaskHandleTest {
    @Test
    void cancelsRunningTaskAndCompletion() {
        CompletableFuture<String> completion = new CompletableFuture<>();
        FutureTask<Void> running = new FutureTask<>(() -> null);
        AsyncTaskHandle<String> task = new AsyncTaskHandle<>(completion, running);

        assertFalse(task.isDone());
        assertTrue(task.cancel());
        assertTrue(task.isCancelled());
        assertTrue(running.isCancelled());
        assertTrue(completion.isCancelled());
    }

    @Test
    void cannotCancelCompletedTask() {
        CompletableFuture<String> completion = new CompletableFuture<>();
        FutureTask<Void> running = new FutureTask<>(() -> null);
        AsyncTaskHandle<String> task = new AsyncTaskHandle<>(completion, running);
        completion.complete("done");

        assertTrue(task.isDone());
        assertFalse(task.cancel());
        assertFalse(task.isCancelled());
    }
}
