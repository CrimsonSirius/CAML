package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.io.SafePaths;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Stream;

/** 版本目录的临时 staging，成功后可原子移动到最终实例目录。 */
public final class VersionDirectoryStage implements AutoCloseable {
    private final Path directory;
    private final Path target;
    private boolean committed;

    private VersionDirectoryStage(Path directory, Path target) {
        this.directory = directory;
        this.target = target;
    }

    public static VersionDirectoryStage create(Path gameDirectory, String instanceId)
            throws IOException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String instance = InstanceNameRules.validate(instanceId);
        Path versionsDirectory = gameDirectory.resolve("versions");
        Files.createDirectories(versionsDirectory);
        Path target = SafePaths.resolveChild(versionsDirectory, instance, "version id");
        Path staged = Files.createTempDirectory(versionsDirectory, ".caml-version-install-");
        return new VersionDirectoryStage(staged, target);
    }

    public Path directory() {
        return directory;
    }

    public InstalledVersion commit(InstalledVersion installed) throws IOException {
        if (committed) {
            throw new IllegalStateException("版本 staging 已提交");
        }
        if (Files.exists(target)) {
            throw new IOException("版本目录已存在: " + target);
        }
        Files.createDirectories(target.getParent());
        try {
            Files.move(directory, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(directory, target);
        }
        committed = true;
        return installed.relocateVersionDirectory(target);
    }

    @Override
    public void close() {
        if (!committed) {
            deleteRecursively(directory);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 保留原始安装异常。
                }
            }
        } catch (IOException ignored) {
            // 保留原始安装异常。
        }
    }
}
