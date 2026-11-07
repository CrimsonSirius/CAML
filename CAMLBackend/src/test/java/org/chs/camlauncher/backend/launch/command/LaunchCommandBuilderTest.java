package org.chs.camlauncher.backend.launch.command;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.launch.LaunchOptions;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.metadata.model.JavaVersion;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchCommandBuilderTest {
    @TempDir
    Path gameDirectory;

    @Test
    void placesMainClassBetweenJvmAndGameArguments() {
        LaunchPlan plan = plan("fake.Main");
        List<Path> classpath = List.of(Path.of("/tmp/a.jar"));

        LaunchCommand command = new LaunchCommandBuilder().build(plan, classpath);

        assertEquals(Path.of("/tmp/java"), command.javaExecutable());
        int cp = command.arguments().indexOf("-cp");
        int main = command.arguments().indexOf("fake.Main");
        assertTrue(cp >= 0 && main > cp);
        assertEquals("/tmp/a.jar", command.arguments().get(cp + 1));
    }

    @Test
    void rejectsMissingMainClass() {
        LaunchPlan plan = plan(null);

        assertThrows(IllegalArgumentException.class,
                () -> new LaunchCommandBuilder().build(plan, List.of()));
    }

    private LaunchPlan plan(String mainClass) {
        VersionInfo manifest = new VersionInfo(
                "1.20.1", "release", null, mainClass, "5", null, null, null,
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
                new JavaRuntime(Path.of("/tmp/java"), "17.0.1", 17, "test", "x86_64", 64, true),
                LaunchOptions.defaults(),
                gameDirectory,
                gameDirectory.resolve("assets"),
                gameDirectory.resolve("versions/1.20.1/natives")
        );
    }
}
