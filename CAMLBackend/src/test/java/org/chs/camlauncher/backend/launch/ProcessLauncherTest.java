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
        Path javaHome = Path.of(System.getProperty("java.home"));
        String javaName = System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        Path javaExecutable = javaHome.resolve("bin").resolve(javaName);

        VersionInfo manifest = new VersionInfo(
                "1.20.1", "release", null, FakeMain.class.getName(), "5", null, null, null,
                null, new JavaVersion("java-runtime-gamma", 17), List.of(), null,
                null, null, null, null
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
        LaunchPlan plan = new LaunchPlan(
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
        List<Path> classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Path::of)
                .toList();

        StringBuffer output = new StringBuffer();
        CountDownLatch exited = new CountDownLatch(1);
        ProcessListener listener = new ProcessListener() {
            @Override
            public void onLog(String line, boolean error) {
                output.append(line).append('\n');
            }

            @Override
            public void onExit(int exitCode) {
                exited.countDown();
            }
        };

        try (GameProcess process = new ProcessLauncher().launch(plan, classpath, listener)) {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            assertEquals(7, process.exitValue());
            assertTrue(exited.await(5, TimeUnit.SECONDS));
            assertTrue(output.toString().contains("fake-main-hello"));
            assertTrue(output.toString().contains("fake-main-error"));
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
