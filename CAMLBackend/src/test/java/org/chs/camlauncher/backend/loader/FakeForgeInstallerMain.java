package org.chs.camlauncher.backend.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FakeForgeInstallerMain {
    private FakeForgeInstallerMain() {
    }

    public static void main(String[] args) throws Exception {
        Path target = Path.of(args[args.length - 1]);
        String versionId = "test-forge-id";

        Path versionDirectory = target.resolve("versions").resolve(versionId);
        Files.createDirectories(versionDirectory);
        Files.writeString(versionDirectory.resolve(versionId + ".json"), """
                {
                  "id": "test-forge-id",
                  "inheritsFrom": "1.20.1",
                  "mainClass": "test.ForgeMain",
                  "type": "release"
                }
                """, StandardCharsets.UTF_8);

        Path library = target.resolve(
                "libraries/org/example/loader-lib/1.0/loader-lib-1.0.jar");
        Files.createDirectories(library.getParent());
        Files.writeString(library, "loader-library", StandardCharsets.UTF_8);

        Files.writeString(target.resolve("launcher_profiles.json"),
                "{\"profiles\":{},\"settings\":{}}\n", StandardCharsets.UTF_8);
    }
}
