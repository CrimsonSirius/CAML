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
        Path gameDirectory = target.toAbsolutePath().getParent();
        Path attemptsFile = gameDirectory.resolve(".fake-installer-attempts");
        int attempt = Files.isRegularFile(attemptsFile)
                ? Integer.parseInt(Files.readString(attemptsFile).trim()) + 1
                : 1;
        Files.writeString(attemptsFile, Integer.toString(attempt), StandardCharsets.UTF_8);
        Path failOnce = gameDirectory.resolve(".fake-installer-fail-once");
        if (Files.isRegularFile(failOnce)) {
            Files.delete(failOnce);
            System.exit(17);
        }

        Path versionDirectory = target.resolve("versions").resolve(versionId);
        Files.createDirectories(versionDirectory);
        String missingLibrary = Files.isRegularFile(
                gameDirectory.resolve(".fake-installer-missing-library"))
                ? """
                  ,"libraries":[{
                    "name":"org.example:missing:1.0",
                    "downloads":{"artifact":{"path":"org/example/missing/1.0/missing-1.0.jar"}}
                  }]
                  """
                : "";
        Files.writeString(versionDirectory.resolve(versionId + ".json"), """
                {
                  "id": "test-forge-id",
                  "inheritsFrom": "1.20.1",
                  "mainClass": "test.ForgeMain",
                  "type": "release"%s
                }
                """.formatted(missingLibrary), StandardCharsets.UTF_8);

        Path library = target.resolve(
                "libraries/org/example/loader-lib/1.0/loader-lib-1.0.jar");
        Files.createDirectories(library.getParent());
        Files.writeString(library, "loader-library", StandardCharsets.UTF_8);

        Files.writeString(target.resolve("launcher_profiles.json"),
                "{\"profiles\":{},\"settings\":{}}\n", StandardCharsets.UTF_8);
    }
}
