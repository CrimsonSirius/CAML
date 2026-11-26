package org.chs.camlauncher.backend.loader;

import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 运行 Forge/NeoForge 外部 installer 子进程并限制输出与超时。 */
final class ExternalInstallerProcess {
    private static final long TIMEOUT_MINUTES = 20;
    private static final int OUTPUT_LIMIT = 64 * 1024;

    private ExternalInstallerProcess() {
    }

    static String run(
            String loaderName,
            JavaRuntime java,
            Path installerJar,
            Path staging,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(java.executable().toString());
        command.add("-Djava.awt.headless=true");
        command.add("-jar");
        command.add(installerJar.toString());
        command.add("--installClient");
        command.add(staging.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(staging.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> captureOutput(process, output),
                "caml-" + loaderName.toLowerCase() + "-installer-output");
        reader.setDaemon(true);
        reader.start();

        boolean finished = false;
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(TIMEOUT_MINUTES);
        try {
            while (!(finished = process.waitFor(200, TimeUnit.MILLISECONDS))) {
                token.throwIfCancelled();
                if (System.nanoTime() >= deadline) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(loaderName + " 安装器超时");
        }
        reader.join(5000);

        if (process.exitValue() != 0) {
            throw new IOException(loaderName + " 安装器失败，退出码 "
                    + process.exitValue() + "\n" + tail(output));
        }
        return tail(output);
    }

    private static void captureOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    output.append(line).append('\n');
                    if (output.length() > OUTPUT_LIMIT) {
                        output.delete(0, output.length() - OUTPUT_LIMIT);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static String tail(StringBuilder output) {
        synchronized (output) {
            return output.toString();
        }
    }
}
