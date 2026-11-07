package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.JavaVersion;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessLauncherTest {
    @TempDir
    Path gameDirectory;

    @Test
    void launchesProcessAndPumpsOutputAndExitCode() throws Exception {
        RecordingListener listener = new RecordingListener();

        try (GameProcess process = new ProcessLauncher().launch(
                plan(javaExecutable()), realClasspath(), listener)) {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            assertEquals(7, process.exitValue());
            assertTrue(listener.exited.await(5, TimeUnit.SECONDS));
            assertTrue(listener.output.toString().contains("fake-main-hello"));
            assertTrue(listener.output.toString().contains("fake-main-error"));
        }
    }

    @Test
    void fallsBackToArgumentFileForVeryLongClasspath() throws Exception {
        RecordingListener listener = new RecordingListener();
        List<Path> classpath = new ArrayList<>(realClasspath());
        for (int i = 0; i < 10_000; i++) {
            classpath.add(Path.of("/tmp/caml-long-classpath/library-" + i + ".jar"));
        }

        try (GameProcess process = new ProcessLauncher().launch(
                plan(javaExecutable()), List.copyOf(classpath), listener)) {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS));
            assertEquals(7, process.exitValue());
            assertTrue(listener.exited.await(5, TimeUnit.SECONDS));
            assertTrue(listener.output.toString().contains("fake-main-hello"));
        }
    }

    private LaunchPlan plan(Path javaExecutable) {
        VersionInfo manifest = new VersionInfo(
                "1.20.1", "release", null, FakeMain.class.getName(), "5", null, null, null,
                null, new JavaVersion("java-runtime-gamma", 17), List.of(), null,
                null, null, null, null, null
        );
        InstalledVersion instance = new InstalledVersion(
                "1.20.1", manifest, gameDirectory,
                gameDirectory.resolve("versions/1.20.1"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.json"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar"),
                gameDirectory.resolve("libraries"),
                gameDirectory.resolve("assets"),
                null
        );
        return new LaunchPlan(
                instance,
                manifest,
                new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}"),
                new JavaRuntime(
                        javaExecutable,
                        Runtime.version().toString(),
                        Runtime.version().feature(),
                        "test",
                        System.getProperty("os.arch").contains("64") ? "x86_64" : "x86",
                        64,
                        true
                ),
                LaunchOptions.defaults(),
                gameDirectory,
                gameDirectory.resolve("assets"),
                gameDirectory.resolve("versions/1.20.1/natives")
        );
    }

    private static Path javaExecutable() {
        String javaName = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return Path.of(System.getProperty("java.home")).resolve("bin").resolve(javaName);
    }

    private static List<Path> realClasspath() {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Path::of)
                .toList();
    }

    private static final class RecordingListener implements ProcessListener {
        private final StringBuffer output = new StringBuffer();
        private final CountDownLatch exited = new CountDownLatch(1);

        @Override
        public void onLog(String line, boolean error) {
            output.append(line).append('\n');
        }

        @Override
        public void onExit(int exitCode) {
            exited.countDown();
        }
    }

    public static final class FakeMain {
        private FakeMain() {
        }

        public static void main(String[] args) {
            System.out.println("fake-main-hello");
            System.err.println("fake-main-error");
            System.exit(7);
        }
    }
}
