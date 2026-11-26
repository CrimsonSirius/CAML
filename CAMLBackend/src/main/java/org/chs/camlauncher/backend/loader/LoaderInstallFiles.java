package org.chs.camlauncher.backend.loader;

import org.chs.camlauncher.backend.instance.InstalledVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/** loader 安装使用的 staging、目录复制和原子移动工具。 */
final class LoaderInstallFiles {
    private LoaderInstallFiles() {
    }

    static void prepareStaging(Path staging, String gameVersion, InstalledVersion base)
            throws IOException {
        Path stagedVersionDirectory = staging.resolve("versions").resolve(gameVersion);
        Files.createDirectories(stagedVersionDirectory);
        Files.copy(base.versionJson(),
                stagedVersionDirectory.resolve(gameVersion + ".json"),
                StandardCopyOption.REPLACE_EXISTING);
        if (Files.isRegularFile(base.clientJar())) {
            Files.copy(base.clientJar(),
                    stagedVersionDirectory.resolve(gameVersion + ".jar"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(
                staging.resolve("launcher_profiles.json"),
                "{\"profiles\":{},\"settings\":{}}\n",
                StandardCharsets.UTF_8
        );
    }

    static void copyTree(Path source, Path target) throws IOException {
        Path sourceRoot = source.toAbsolutePath().normalize();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(sourceRoot.relativize(path)).normalize();
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else if (Files.isRegularFile(path)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static void moveVersionDirectory(
            Path source,
            Path target,
            String installedVersionId,
            String instanceId
    ) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }

        if (!installedVersionId.equals(instanceId)) {
            Path json = target.resolve(installedVersionId + ".json");
            if (!Files.isRegularFile(json)) {
                throw new IOException("安装器生成的版本 JSON 不存在: " + json);
            }
            Files.move(json, target.resolve(instanceId + ".json"),
                    StandardCopyOption.REPLACE_EXISTING);

            Path jar = target.resolve(installedVersionId + ".jar");
            if (Files.isRegularFile(jar)) {
                Files.move(jar, target.resolve(instanceId + ".jar"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static void moveFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }
}
