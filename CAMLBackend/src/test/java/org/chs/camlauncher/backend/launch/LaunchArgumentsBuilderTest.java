package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.Argument;
import org.chs.camlauncher.backend.metadata.Arguments;
import org.chs.camlauncher.backend.metadata.DownloadInfo;
import org.chs.camlauncher.backend.metadata.JavaVersion;
import org.chs.camlauncher.backend.metadata.LoggingInfo;
import org.chs.camlauncher.backend.metadata.Rule;
import org.chs.camlauncher.backend.metadata.RuledArgument;
import org.chs.camlauncher.backend.metadata.StringArgument;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchArgumentsBuilderTest {
    @TempDir
    Path gameDirectory;

    @Test
    void buildsJvmArgumentsWithMemoryDefaultsAndClasspath() {
        LaunchPlan plan = plan(new Arguments(
                List.of(),
                List.of(
                        new StringArgument("-Dfoo=${auth_player_name}"),
                        new RuledArgument(
                                List.of(new Rule(Rule.Action.ALLOW, null,
                                        Map.of("has_custom_resolution", true))),
                                List.of("-Dcustom")
                        )
                )
        ), new LaunchOptions(
                1024, 2048, 854, 480, false,
                List.of("-XX:+UseG1GC"), List.of(), Map.of()
        ));
        List<Path> classpath = List.of(Path.of("/tmp/a.jar"), Path.of("/tmp/b.jar"));

        List<String> result = new JvmArgumentsBuilder().build(plan, classpath);

        assertTrue(result.contains("-Xmx2048m"));
        assertTrue(result.contains("-Xms1024m"));
        assertTrue(result.contains("-Dfoo=Steve"));
        assertTrue(result.contains("-Dcustom"));
        assertTrue(result.contains("-XX:+UseG1GC"));
        assertTrue(result.contains("-Djava.library.path=" + plan.nativesDirectory()));
        assertTrue(result.contains("-cp"));
        assertTrue(result.contains("/tmp/a.jar" + java.io.File.pathSeparator + "/tmp/b.jar"));
    }

    @Test
    void buildsGameArgumentsWithRulesAndPlaceholders() {
        LaunchPlan plan = plan(new Arguments(
                List.of(
                        new StringArgument("--username"),
                        new StringArgument("${auth_player_name}"),
                        new StringArgument("--uuid"),
                        new StringArgument("${auth_uuid}"),
                        new RuledArgument(
                                List.of(new Rule(Rule.Action.ALLOW, null,
                                        Map.of("has_custom_resolution", true))),
                                List.of("--width", "${resolution_width}", "--height", "${resolution_height}")
                        ),
                        new RuledArgument(
                                List.of(new Rule(Rule.Action.ALLOW, null,
                                        Map.of("is_demo_user", true))),
                                List.of("--demo")
                        )
                ),
                List.of()
        ), new LaunchOptions(
                null, null, 854, 480, true,
                List.of(), List.of("--extra"), Map.of()
        ));

        List<String> result = new GameArgumentsBuilder().build(plan, List.of());

        assertEquals("Steve", result.get(result.indexOf("--username") + 1));
        assertEquals(
                plan.auth().uuid().toString().replace("-", ""),
                result.get(result.indexOf("--uuid") + 1)
        );
        assertEquals("854", result.get(result.indexOf("--width") + 1));
        assertEquals("480", result.get(result.indexOf("--height") + 1));
        assertTrue(result.contains("--fullscreen"));
        assertTrue(result.contains("--extra"));
        assertFalse(result.contains("--demo"));
    }

    @Test
    void fallsBackToLegacyMinecraftArguments() {
        VersionInfo manifest = manifest(null, "--username ${auth_player_name} --gameDir ${game_directory}");
        LaunchPlan plan = plan(manifest, LaunchOptions.defaults());

        List<String> result = new GameArgumentsBuilder().build(plan, List.of());

        assertTrue(result.contains("--username"));
        assertTrue(result.contains("Steve"));
        assertTrue(result.contains("--gameDir"));
        assertTrue(result.contains(gameDirectory.toString()));
    }

    @Test
    void addsLog4jConfigurationArgument() {
        LoggingInfo logging = new LoggingInfo(
                "-Dlog4j.configurationFile=${path}",
                new DownloadInfo("client-1.12.xml", null, "sha1", 1L, "https://example.com/log.xml"),
                "log4j2-xml"
        );
        VersionInfo manifest = new VersionInfo(
                "1.20.1", "release", null, "net.minecraft.client.main.Main", "5", null,
                null, new Arguments(List.of(), List.of()), null,
                new JavaVersion("java-runtime-gamma", 17), List.of(), Map.of("client", logging),
                null, null, null, null
        );
        LaunchPlan plan = plan(manifest, LaunchOptions.defaults());

        List<String> result = new JvmArgumentsBuilder().build(plan, List.of());

        assertTrue(result.contains(
                "-Dlog4j.configurationFile="
                        + gameDirectory.resolve("assets/log_configs/client-1.12.xml")
        ));
    }

    private LaunchPlan plan(Arguments arguments, LaunchOptions options) {
        return plan(manifest(arguments, null), options);
    }

    private LaunchPlan plan(VersionInfo manifest, LaunchOptions options) {
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
        return new LaunchPlan(
                instance,
                manifest,
                new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}"),
                new JavaRuntime(Path.of("/tmp/java"), "17.0.1", 17, "test", "x86_64", 64, true),
                options,
                gameDirectory,
                gameDirectory.resolve("assets"),
                gameDirectory.resolve("versions/1.20.1/natives")
        );
    }

    private static VersionInfo manifest(Arguments arguments, String minecraftArguments) {
        return new VersionInfo(
                "1.20.1", "release", null, "net.minecraft.client.main.Main", "5", null,
                minecraftArguments, arguments, null,
                new JavaVersion("java-runtime-gamma", 17), List.of(), null,
                null, null, null, null
        );
    }
}
