package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.DownloadInfo;
import org.chs.camlauncher.backend.metadata.JavaVersion;
import org.chs.camlauncher.backend.metadata.Library;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeExtractorTest {
    @TempDir
    Path gameDirectory;

    @Test
    void cleansNativesAndExtractsOnlyAllowedEntries() throws Exception {
        Path libraries = gameDirectory.resolve("libraries");
        Path nativeJar = libraries.resolve("org/example/native/1.0/native-1.0-natives-linux.jar");
        Files.createDirectories(nativeJar.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(nativeJar))) {
            addEntry(zip, "libtest.so", "binary");
            addEntry(zip, "META-INF/MANIFEST.MF", "manifest");
            addEntry(zip, "excluded/file.txt", "excluded");
        }

        Library library = new Library(
                "org.example:native:1.0",
                null,
                new Library.Downloads(
                        null,
                        Map.of("natives-linux", new DownloadInfo(
                                "org/example/native/1.0/native-1.0-natives-linux.jar",
                                "sha1", 1L, "https://example.com/native.jar"))
                ),
                Map.of("linux", "natives-linux"),
                new Library.Extract(List.of("excluded/")),
                List.of()
        );
        VersionInfo manifest = new VersionInfo(
                "1.20.1", "release", null, "net.minecraft.client.main.Main", "5", null, null, null,
                null, new JavaVersion("java-runtime-gamma", 17), List.of(library), null,
                null, null, null, null
        );
        InstalledVersion instance = new InstalledVersion(
                "1.20.1",
                manifest,
                gameDirectory,
                gameDirectory.resolve("versions/1.20.1"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.json"),
                gameDirectory.resolve("versions/1.20.1/1.20.1.jar"),
                libraries,
                gameDirectory.resolve("assets"),
                null
        );
        Path nativesDirectory = gameDirectory.resolve("versions/1.20.1/natives");
        Files.createDirectories(nativesDirectory);
        Files.writeString(nativesDirectory.resolve("stale.txt"), "stale");

        LaunchPlan plan = new LaunchPlan(
                instance,
                manifest,
                new AuthSession("Steve", UUID.randomUUID(), "token", "legacy", "{}"),
                new JavaRuntime(Path.of("/tmp/java"), "17.0.1", 17, "test", "x86_64", 64, true),
                LaunchOptions.defaults(),
                gameDirectory,
                gameDirectory.resolve("assets"),
                nativesDirectory
        );

        new NativeExtractor(
                new RuleEvaluator("linux", "x86_64", "6.0", Map.of()),
                "linux",
                64
        ).extract(plan);

        assertTrue(Files.isRegularFile(nativesDirectory.resolve("libtest.so")));
        assertFalse(Files.exists(nativesDirectory.resolve("stale.txt")));
        assertFalse(Files.exists(nativesDirectory.resolve("META-INF")));
        assertFalse(Files.exists(nativesDirectory.resolve("excluded")));
    }

    private static void addEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
