package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.JavaVersion;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LaunchPlannerTest {
    @TempDir
    Path gameDirectory;

    @Test
    void createsImmutableLaunchPlan() {
        VersionInfo manifest = manifestWithJava(17);
        InstalledVersion instance = new InstalledVersion(
                "1.20.1",
                manifest,
                gameDirectory,
                gameDirectory.resolve("versions/1.20.1"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.json"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar"),
                gameDirectory.resolve("libraries"),
                gameDirectory.resolve("assets"),
                gameDirectory.resolve("assets/indexes/5.json")
        );
        AuthSession auth = new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}");
        LaunchOptions options = new LaunchOptions(
                1024,
                2048,
                854,
                480,
                true,
                List.of("-XX:+UseG1GC"),
                List.of("--demo"),
                Map.of("LANG", "zh_CN.UTF-8")
        );

        LaunchPlan plan = new LaunchPlanner().create(
                instance,
                auth,
                new JavaRuntime(Path.of("/tmp/java"), "17.0.1", 17, "test", "x86_64", 64, true),
                options
        );

        assertEquals(instance, plan.instance());
        assertEquals(auth, plan.auth());
        assertEquals(options, plan.options());
        assertEquals(gameDirectory, plan.gameDirectory());
        assertEquals(gameDirectory.resolve("assets"), plan.assetsDirectory());
        assertEquals(gameDirectory.resolve("versions/1.20.1/natives"), plan.nativesDirectory());
    }

    @Test
    void rejectsUnsuitableJava() {
        InstalledVersion instance = new InstalledVersion(
                "1.20.1",
                manifestWithJava(17),
                gameDirectory,
                gameDirectory.resolve("versions/1.20.1"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.json"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar"),
                gameDirectory.resolve("libraries"),
                gameDirectory.resolve("assets"),
                null
        );
        AuthSession auth = new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}");

        assertThrows(IllegalArgumentException.class, () -> new LaunchPlanner().create(
                instance,
                auth,
                new JavaRuntime(Path.of("/tmp/java"), "8.0", 8, "test", "x86_64", 64, true),
                LaunchOptions.defaults()
        ));
    }

    private static VersionInfo manifestWithJava(int majorVersion) {
        return new VersionInfo(
                "1.20.1", "release", null, "net.minecraft.client.main.Main", "5", null, null, null,
                null, new JavaVersion("java-runtime-gamma", majorVersion), List.of(), null,
                null, null, null, null, null
        );
    }
}
