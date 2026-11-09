package org.chs.camlauncher.backend.runtime;

import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class JavaLocator {
    private static final long INSPECT_TIMEOUT_SECONDS = 5;
    private static final String JAVA_EXECUTABLE = isWindows() ? "java.exe" : "java";
    private static final String JAVAC_EXECUTABLE = isWindows() ? "javac.exe" : "javac";

    public List<JavaRuntime> discover() {
        Map<String, JavaRuntime> runtimes = new LinkedHashMap<>();
        for (Path candidate : candidates()) {
            inspect(candidate).ifPresent(runtime -> runtimes.putIfAbsent(
                    runtime.executable().toString(),
                    runtime
            ));
        }

        List<JavaRuntime> result = new ArrayList<>(runtimes.values());
        result.sort(Comparator
                .comparingInt(JavaRuntime::majorVersion).reversed()
                .thenComparing(runtime -> runtime.executable().toString()));
        return List.copyOf(result);
    }

    /**
     * 扫描 Java，并合并持久化的手动 Java；同一真实路径只保留一项。
     *
     * @param manualPaths 用户手动添加的 Java
     * @param selectedPath 当前选中的 Java，即使已经不可用也保留在结果中
     */
    public List<JavaCandidate> discoverCandidates(List<Path> manualPaths, Path selectedPath) {
        Map<String, JavaCandidate> candidates = new LinkedHashMap<>();
        Set<String> manualKeys = new LinkedHashSet<>();
        for (Path path : manualPaths) {
            if (path != null) {
                manualKeys.add(canonicalKey(path));
            }
        }

        for (JavaRuntime runtime : discover()) {
            String key = canonicalKey(runtime.executable());
            candidates.put(key, JavaCandidate.from(runtime, manualKeys.contains(key)));
        }

        for (Path path : manualPaths) {
            if (path == null) {
                continue;
            }
            String key = canonicalKey(path);
            JavaCandidate existing = candidates.get(key);
            if (existing != null) {
                candidates.put(key, existing.withManual(true));
                continue;
            }
            inspect(path).ifPresentOrElse(
                    runtime -> candidates.put(key, JavaCandidate.from(runtime, true)),
                    () -> candidates.put(key, JavaCandidate.unavailable(
                            path.toAbsolutePath().normalize(), true))
            );
        }

        if (selectedPath != null) {
            String selectedKey = canonicalKey(selectedPath);
            if (!candidates.containsKey(selectedKey)) {
                inspect(selectedPath).ifPresentOrElse(
                        runtime -> candidates.put(selectedKey, JavaCandidate.from(runtime, false)),
                        () -> candidates.put(selectedKey, JavaCandidate.unavailable(
                                selectedPath.toAbsolutePath().normalize(), false))
                );
            }
        }

        List<JavaCandidate> result = new ArrayList<>(candidates.values());
        result.sort(Comparator
                .comparing(JavaCandidate::available).reversed()
                .thenComparing(Comparator.comparingInt(JavaCandidate::majorVersion).reversed())
                .thenComparing(candidate -> candidate.executable().toString()));
        return List.copyOf(result);
    }

    public Optional<JavaRuntime> findSuitable(JavaRequirement requirement) {
        return discover().stream()
                .filter(requirement::isSatisfiedBy)
                .min(Comparator
                        .comparingInt((JavaRuntime runtime) ->
                                runtime.majorVersion() == requirement.majorVersion() ? 0 : 1)
                        .thenComparingInt(runtime ->
                                Math.abs(runtime.majorVersion() - requirement.majorVersion()))
                        .thenComparing(runtime -> runtime.jdk() ? 0 : 1)
                        .thenComparing(runtime -> runtime.executable().toString()));
    }

    public Optional<JavaRuntime> inspect(Path executable) {
        if (executable == null || !Files.isRegularFile(executable)) {
            return Optional.empty();
        }

        Process process;
        try {
            process = new ProcessBuilder(
                    executable.toString(),
                    "-XshowSettings:properties",
                    "-version"
            ).redirectErrorStream(true).start();
        } catch (IOException e) {
            return Optional.empty();
        }

        try {
            if (!process.waitFor(INSPECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return Optional.empty();
            }

            Map<String, String> properties = JavaRuntimeParser.parseProperties(output);
            String version = properties.get("java.version");
            String architecture = JavaRuntimeParser.normalizeArchitecture(
                    properties.getOrDefault("os.arch", ""));
            int majorVersion = JavaRuntimeParser.parseMajorVersion(version);
            if (version == null || majorVersion <= 0 || architecture.isBlank()) {
                return Optional.empty();
            }

            Path home = properties.containsKey("java.home")
                    ? Path.of(properties.get("java.home"))
                    : executable.toAbsolutePath().getParent().getParent();
            int bits = JavaRuntimeParser.parseBits(properties.get("sun.arch.data.model"), architecture);
            boolean jdk = Files.isRegularFile(home.resolve("bin").resolve(JAVAC_EXECUTABLE));

            return Optional.of(new JavaRuntime(
                    executable.toAbsolutePath().normalize(),
                    version,
                    majorVersion,
                    properties.getOrDefault("java.vendor", "unknown"),
                    architecture,
                    bits,
                    jdk
            ));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        } finally {
            process.destroy();
        }
    }

    public Path launchExecutable(JavaRuntime runtime) {
        Path executable = runtime.executable();
        if (isWindows() && executable.getFileName().toString().equalsIgnoreCase("java.exe")) {
            Path javaw = executable.resolveSibling("javaw.exe");
            if (Files.isRegularFile(javaw)) {
                return javaw;
            }
        }
        return executable;
    }

    private static List<Path> candidates() {
        List<Path> candidates = new ArrayList<>();
        addJavaHome(candidates, System.getProperty("java.home"));
        addJavaHome(candidates, System.getenv("JAVA_HOME"));
        addPathCandidates(candidates);

        for (Path root : commonRoots()) {
            addChildren(candidates, root);
        }
        return candidates;
    }

    private static void addJavaHome(List<Path> candidates, String home) {
        if (home == null || home.isBlank()) {
            return;
        }
        candidates.add(Path.of(home, "bin", JAVA_EXECUTABLE));
    }

    private static void addPathCandidates(List<Path> candidates) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return;
        }
        for (String entry : path.split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                candidates.add(Path.of(entry, JAVA_EXECUTABLE));
            }
        }
    }

    private static void addChildren(List<Path> candidates, Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> children = Files.list(root)) {
            children.forEach(child -> candidates.add(child.resolve("bin").resolve(JAVA_EXECUTABLE)));
        } catch (IOException ignored) {
        }
    }

    private static List<Path> commonRoots() {
        String userHome = System.getProperty("user.home");
        List<Path> roots = new ArrayList<>(List.of(
                Path.of(userHome, ".jdks"),
                Path.of(userHome, ".gradle", "jdks"),
                Path.of(userHome, ".sdkman", "candidates", "java"),
                Path.of(userHome, ".caml", "java")
        ));

        if (isWindows()) {
            String programFiles = System.getenv("ProgramFiles");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (programFiles != null) {
                roots.add(Path.of(programFiles, "Java"));
                roots.add(Path.of(programFiles, "Eclipse Adoptium"));
                roots.add(Path.of(programFiles, "Zulu"));
                roots.add(Path.of(programFiles, "Microsoft"));
            }
            if (localAppData != null) {
                roots.add(Path.of(localAppData, "Programs"));
            }
        } else {
            roots.add(Path.of("/usr/lib/jvm"));
            roots.add(Path.of("/usr/java"));
            roots.add(Path.of("/opt"));
        }
        return roots;
    }

    private static boolean isWindows() {
        return "windows".equals(RuleEvaluator.currentOsName());
    }

    private static String canonicalKey(Path path) {
        try {
            return path.toRealPath().toString();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize().toString();
        }
    }
}
