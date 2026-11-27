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

    /** 接管调用方已创建（或即将创建）的版本 staging 目录。 */
    public static VersionDirectoryStage adopt(
            Path gameDirectory, String instanceId, Path stagedDirectory) throws IOException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(stagedDirectory, "stagedDirectory");
        String instance = InstanceNameRules.validate(instanceId);
        Path versionsDirectory = gameDirectory.resolve("versions").toAbsolutePath().normalize();
        Path target = SafePaths.resolveChild(versionsDirectory, instance, "version id");
        Path staged = stagedDirectory.toAbsolutePath().normalize();
        if (!versionsDirectory.equals(staged.getParent()) || staged.equals(target)) {
            throw new IOException("版本 staging 目录无效: " + stagedDirectory);
        }
        return new VersionDirectoryStage(staged, target);
    }

    public Path directory() {
        return directory;
    }

    public InstalledVersion commit(InstalledVersion installed) throws IOException {
        Path target = commitDirectory();
        return installed.relocateVersionDirectory(target);
    }

    /** 原子发布 staging 目录并返回最终目录。 */
    public Path commitDirectory() throws IOException {
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
        return target;
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
