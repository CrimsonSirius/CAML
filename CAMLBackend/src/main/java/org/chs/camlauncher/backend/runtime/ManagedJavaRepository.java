package org.chs.camlauncher.backend.runtime;

import org.chs.camlauncher.backend.json.JsonService;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** CAML 托管的 Java 仓库：<data>/java/<platform>/<name>。 */
public final class ManagedJavaRepository {
    private static final String MANIFEST_SUFFIX = ".json";

    private final Path root;
    private final JavaLocator javaLocator;
    private final JsonService json = new JsonService();

    public ManagedJavaRepository(Path dataDirectory, JavaLocator javaLocator) {
        this.root = dataDirectory.toAbsolutePath().normalize().resolve("java");
        this.javaLocator = javaLocator;
    }

    public Path root() {
        return root;
    }

    public Path platformRoot() {
        return root.resolve(JavaPlatform.current().key());
    }

    public Path tempArchivePath(String name, String fileName) throws IOException {
        Path temp = platformRoot().resolve(".tmp");
        Files.createDirectories(temp);
        String safeFile = fileName == null || fileName.isBlank() ? "java-archive" : fileName;
        return temp.resolve(sanitize(name) + "-" + safeFile.replace('/', '_').replace('\\', '_'));
    }

    public JavaCandidate install(JavaDownloadOption option, Path archive, String requestedName,
                                 String checksum) throws IOException {
        String name = uniqueName(requestedName == null || requestedName.isBlank()
                ? option.distributionId() + "-" + option.majorVersion()
                + "-" + option.packageType().apiValue()
                : requestedName);
        Path platformRoot = platformRoot();
        Path target = platformRoot.resolve(name);
        Path extractRoot = platformRoot.resolve(".tmp").resolve(name + "-extract");
        Files.createDirectories(platformRoot.resolve(".tmp"));
        deleteTree(extractRoot);
        try {
            JavaArchiveExtractor.extract(archive, extractRoot, option.fileName());
            Path contentRoot = singleChildDirectory(extractRoot);
            if (contentRoot != null) {
                move(contentRoot, target);
            } else {
                Files.createDirectories(target);
                moveChildren(extractRoot, target);
            }

            Path executable = findExecutable(target);
            ensureExecutable(target);
            JavaRuntime runtime = javaLocator.inspect(executable)
                    .orElseThrow(() -> new IOException("无法识别已解压的 Java: " + executable));
            json.write(platformRoot.resolve(name + MANIFEST_SUFFIX),
                    ManagedJavaManifest.from(option, name, checksum));
            return JavaCandidate.from(runtime, false);
        } catch (IOException | RuntimeException e) {
            deleteTree(target);
            throw e;
        } finally {
            deleteTree(extractRoot);
        }
    }

    public List<JavaCandidate> listCandidates() {
        Path platformRoot = platformRoot();
        if (!Files.isDirectory(platformRoot)) {
            return List.of();
        }
        List<JavaCandidate> result = new ArrayList<>();
        try (Stream<Path> entries = Files.list(platformRoot)) {
            entries.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().equals(".tmp"))
                    .forEach(path -> {
                        try {
                            Path executable = findExecutable(path);
                            javaLocator.inspect(executable).ifPresent(runtime ->
                                    result.add(JavaCandidate.from(runtime, false)));
                        } catch (IOException | RuntimeException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
        result.sort(Comparator
                .comparing(JavaCandidate::available).reversed()
                .thenComparing(Comparator.comparingInt(JavaCandidate::majorVersion).reversed())
                .thenComparing(candidate -> candidate.executable().toString()));
        return List.copyOf(result);
    }

    private String uniqueName(String requestedName) {
        String base = sanitize(requestedName);
        Path platformRoot = platformRoot();
        String candidate = base;
        int index = 2;
        while (Files.exists(platformRoot.resolve(candidate))
                || Files.exists(platformRoot.resolve(candidate + MANIFEST_SUFFIX))) {
            candidate = base + "-" + index++;
        }
        return candidate;
    }

    private static String sanitize(String value) {
        String sanitized = value == null ? "" : value.trim()
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "java" : sanitized;
    }

    private static Path findExecutable(Path javaHome) throws IOException {
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win");
        Path executable = javaHome.resolve("bin").resolve(windows ? "java.exe" : "java");
        if (!Files.isRegularFile(executable)) {
            Path macExecutable = javaHome.resolve("Contents/Home/bin/java");
            if (Files.isRegularFile(macExecutable)) {
                return macExecutable;
            }
            throw new IOException("Java 归档中缺少 bin/java: " + javaHome);
        }
        return executable;
    }

    private static void ensureExecutable(Path javaHome) throws IOException {
        Path bin = javaHome.resolve("bin");
        if (!Files.isDirectory(bin)) {
            bin = javaHome.resolve("Contents/Home/bin");
        }
        if (!Files.isDirectory(bin)) {
            return;
        }
        try (Stream<Path> files = Files.walk(bin)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    path.toFile().setExecutable(true, false);
                } catch (RuntimeException ignored) {
                }
            });
        }
    }

    private static Path singleChildDirectory(Path directory) throws IOException {
        try (Stream<Path> children = Files.list(directory)) {
            List<Path> entries = children.toList();
            if (entries.size() == 1 && Files.isDirectory(entries.get(0))) {
                return entries.get(0);
            }
        }
        return null;
    }

    private static void moveChildren(Path source, Path target) throws IOException {
        try (Stream<Path> children = Files.list(source)) {
            for (Path child : children.toList()) {
                move(child, target.resolve(child.getFileName().toString()));
            }
        }
    }

    private static void move(Path source, Path target) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTree(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
