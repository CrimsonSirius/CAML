package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.DownloadInfo;
import org.chs.camlauncher.backend.metadata.JavaVersion;
import org.chs.camlauncher.backend.metadata.Library;
import org.chs.camlauncher.backend.metadata.Rule;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathBuilderTest {
    @TempDir
    Path gameDirectory;

    @Test
    void buildsOrderedDeduplicatedClasspathWithClientJarLast() {
        Library base = library("org.example:base:1.0", "org/example/base/1.0/base-1.0.jar", List.of(), Map.of());
        Library nativeMain = new Library(
                "org.example:main:1.0",
                null,
                new Library.Downloads(
                        new DownloadInfo("org/example/main/1.0/main-1.0.jar", "main", 1L, "https://example.com/main.jar"),
                        Map.of("natives-linux", new DownloadInfo(
                                "org/example/main/1.0/main-1.0-natives-linux.jar",
                                "native", 1L, "https://example.com/native.jar"))
                ),
                Map.of("linux", "natives-linux"),
                null,
                List.of()
        );
        Library nativeOnly = library(
                "org.example:native:1.0:natives-linux",
                "org/example/native/1.0/native-1.0-natives-linux.jar",
                List.of(),
                Map.of()
        );
        Library disallowed = library(
                "org.example:windows-only:1.0",
                "org/example/windows-only/1.0/windows-only-1.0.jar",
                List.of(new Rule(
                        Rule.Action.ALLOW,
                        new Rule.Os("windows", null, null),
                        Map.of()
                )),
                Map.of()
        );

        VersionInfo manifest = new VersionInfo(
                "1.20.1", "release", null, "net.minecraft.client.main.Main", "5", null, null, null,
                null, new JavaVersion("java-runtime-gamma", 17),
                List.of(base, nativeMain, base, nativeOnly, disallowed), null,
                null, null, null, null
        );

        InstalledVersion instance = new InstalledVersion(
                "1.20.1",
                manifest,
                gameDirectory,
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
                new JavaRuntime(Path.of("/tmp/java"), "17.0.1", 17, "test", "x86_64", 64, true),
                LaunchOptions.defaults(),
                gameDirectory,
                gameDirectory.resolve("assets"),
                gameDirectory.resolve("versions/1.20.1/natives")
        );

        ClasspathBuilder builder = new ClasspathBuilder(
                new RuleEvaluator("linux", "x86_64", "6.0", Map.of())
        );
        List<Path> classpath = builder.build(plan);

        assertEquals(3, classpath.size());
        assertEquals(
                gameDirectory.resolve("libraries/org/example/base/1.0/base-1.0.jar").toAbsolutePath().normalize(),
                classpath.get(0)
        );
        assertEquals(
                gameDirectory.resolve("libraries/org/example/main/1.0/main-1.0.jar").toAbsolutePath().normalize(),
                classpath.get(1)
        );
        assertEquals(
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar").toAbsolutePath().normalize(),
                classpath.get(2)
        );
        assertTrue(builder.join(classpath).contains("base-1.0.jar"));
    }

    private static Library library(
            String name,
            String path,
            List<Rule> rules,
            Map<String, String> natives
    ) {
        return new Library(
                name,
                null,
                new Library.Downloads(
                        new DownloadInfo(path, "sha1", 1L, "https://example.com/library.jar"),
                        Map.of()
                ),
                natives,
                null,
                rules
        );
    }
}
