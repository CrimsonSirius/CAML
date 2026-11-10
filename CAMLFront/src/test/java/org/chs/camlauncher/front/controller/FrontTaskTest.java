package org.chs.camlauncher.front.controller;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontTaskTest {
    @Test
    void cancelsRunningTaskAndCompletion() {
        CompletableFuture<String> completion = new CompletableFuture<>();
        FutureTask<Void> running = new FutureTask<>(() -> null);
        FrontTask<String> task = new FrontTask<>(completion, running);

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
        FrontTask<String> task = new FrontTask<>(completion, running);
        completion.complete("done");

        assertTrue(task.isDone());
        assertFalse(task.cancel());
        assertFalse(task.isCancelled());
    }
}
