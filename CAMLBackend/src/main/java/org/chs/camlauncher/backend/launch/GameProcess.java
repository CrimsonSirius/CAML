package org.chs.camlauncher.backend.launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GameProcess implements AutoCloseable {
    private final Process process;
    private final List<String> command;
    private final Thread stdoutPump;
    private final Thread stderrPump;
    private final Thread exitWatcher;

    GameProcess(Process process, List<String> command, ProcessListener listener, Path cleanupFile) {
        this.process = process;
        this.command = List.copyOf(command);
        ProcessListener actualListener = listener == null ? ProcessListener.NONE : listener;
        this.stdoutPump = pump(process.getInputStream(), false, actualListener, "caml-game-stdout");
        this.stderrPump = pump(process.getErrorStream(), true, actualListener, "caml-game-stderr");
        this.exitWatcher = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                stdoutPump.join();
                stderrPump.join();
                actualListener.onExit(exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (cleanupFile != null) {
                    try {
                        Files.deleteIfExists(cleanupFile);
                    } catch (IOException ignored) {
                    }
                }
            }
        }, "caml-game-exit");
        exitWatcher.setDaemon(true);
        exitWatcher.start();
    }

    public List<String> command() {
        return command;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    public int exitValue() {
        return process.exitValue();
    }

    public void stop() {
        process.destroy();
    }

    @Override
    public void close() {
        stop();
    }

    private static Thread pump(InputStream input, boolean error, ProcessListener listener, String name) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    listener.onLog(line, error);
                }
            } catch (IOException e) {
                listener.onLog("Failed to read process output: " + e.getMessage(), true);
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
