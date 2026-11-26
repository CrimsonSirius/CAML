package org.chs.camlauncher.backend.mod.service;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.io.ArchiveExtractionGuard;
import org.chs.camlauncher.backend.io.SafePaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 将地图 ZIP 解压为 Minecraft 世界目录，并安装到实例的 saves 目录。 */
final class WorldInstaller {
    private final ArchiveExtractionGuard extractionGuard;

    WorldInstaller() {
        this(new ArchiveExtractionGuard());
    }

    WorldInstaller(ArchiveExtractionGuard extractionGuard) {
        this.extractionGuard = extractionGuard;
    }

    Path install(Path archive, Path savesDirectory, String fallbackName) throws IOException {
        Files.createDirectories(savesDirectory);
        Path temporary = Files.createTempDirectory(savesDirectory, ".caml-world-");
        String displayName = fallbackName == null || fallbackName.isBlank() ? "world" : fallbackName;
        try {
            try {
                extract(archive, temporary, displayName);
            } catch (BackendException e) {
                throw e;
            } catch (IOException | RuntimeException e) {
                throw invalidArchive(displayName, e);
            }
            Path worldRoot = findWorldRoot(temporary);
            if (worldRoot == null) {
                throw invalidArchive(displayName);
            }
            String name = worldRoot.equals(temporary)
                    ? sanitize(baseName(fallbackName))
                    : sanitize(worldRoot.getFileName().toString());
            Path target = uniqueTarget(savesDirectory, name);
            Files.move(worldRoot, target);
            return target;
        } finally {
            deleteRecursively(temporary);
        }
    }

    private void extract(Path archive, Path output, String displayName) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                extractionGuard.beginEntry();
                String name = entry.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }
                Path target = safeTarget(output, name, displayName);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream stream = Files.newOutputStream(target,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE)) {
                        extractionGuard.copy(input, stream);
                    }
                }
                input.closeEntry();
            }
        }
    }

    private static Path findWorldRoot(Path temporary) throws IOException {
        if (Files.isRegularFile(temporary.resolve("level.dat"))) {
            return temporary;
        }
        try (Stream<Path> children = Files.list(temporary)) {
            List<Path> candidates = children
                    .filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve("level.dat")))
                    .toList();
            return candidates.size() == 1 ? candidates.getFirst() : null;
        }
    }

    private static Path safeTarget(Path root, String name, String displayName) throws IOException {
        String normalized = name.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        Path target = root.resolve(normalized).normalize();
        if (target.equals(root) || !target.startsWith(root)) {
            throw invalidArchive(displayName);
        }
        return target;
    }

    private static Path uniqueTarget(Path savesDirectory, String name) {
        String candidate = name;
        int suffix = 2;
        while (true) {
            Path target = SafePaths.resolveChild(savesDirectory, candidate, "world name");
            if (!Files.exists(target)) {
                return target;
            }
            candidate = name + " (" + suffix++ + ")";
        }
    }

    private static String baseName(String fileName) {
        String name = fileName == null ? "" : fileName;
        int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (separator >= 0) {
            name = name.substring(separator + 1);
        }
        int extension = name.lastIndexOf('.');
        if (extension > 0) {
            name = name.substring(0, extension);
        }
        return name;
    }

    private static String sanitize(String name) {
        String cleaned = name == null ? "" : name
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .trim()
                .replaceAll("[. ]+$", "");
        return cleaned.isBlank() ? "world" : cleaned;
    }

    private static BackendException invalidArchive(String displayName) {
        return new BackendException(BackendError.of("mod.error.worldArchiveInvalid", displayName));
    }

    private static BackendException invalidArchive(String displayName, Throwable cause) {
        return new BackendException(
                BackendError.of("mod.error.worldArchiveInvalid", displayName), cause);
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 临时目录清理失败不覆盖安装结果。
                }
            });
        } catch (IOException ignored) {
            // 临时目录清理失败不覆盖安装结果。
        }
    }
}
