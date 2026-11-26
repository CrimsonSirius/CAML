package org.chs.camlauncher.backend.runtime;

import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    /**
     * 探测超时。CI 上冷启动 JVM 可能远慢于本机（Windows runner 常见数秒），
     * 因此这里给足余量；真正的兜底见 {@link #fromReleaseFile(Path)}。
     */
    private static final long INSPECT_TIMEOUT_SECONDS = 20;
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

    /** 找到架构/位数匹配且主版本最接近的 Java，允许版本不满足要求。 */
    public Optional<JavaRuntime> findClosest(JavaRequirement requirement) {
        return discover().stream()
                .filter(runtime -> runtime.architecture().equals(requirement.architecture()))
                .filter(runtime -> requirement.bits() <= 0 || runtime.bits() == requirement.bits())
                .min(Comparator
                        .comparingInt((JavaRuntime runtime) ->
                                Math.abs(runtime.majorVersion() - requirement.majorVersion()))
                        .thenComparing(Comparator.comparingInt(JavaRuntime::majorVersion).reversed())
                        .thenComparing(runtime -> runtime.jdk() ? 0 : 1)
                        .thenComparing(runtime -> runtime.executable().toString()));
    }

    /**
     * 读取一个 Java 可执行文件的信息。
     *
     * <p>先尝试执行 `java -XshowSettings:properties -version`；失败（超时、退出码非 0、
     * 输出无法解析、或被安全软件拦截）时回退到读取同目录上层的 `release` 文件。
     *
     * <p>回退是必要的：进程探测在受限环境里可能整体不可用，而 `release` 是 JDK 自带的
     * 纯文本元数据，读取不受子进程、编码和超时影响。
     */
    public Optional<JavaRuntime> inspect(Path executable) {
        if (executable == null || !Files.isRegularFile(executable)) {
            return Optional.empty();
        }
        Optional<JavaRuntime> probed = probe(executable);
        return probed.isPresent() ? probed : fromReleaseFile(executable);
    }

    private Optional<JavaRuntime> probe(Path executable) {
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

        // 必须先把输出读干净再等退出：子进程写满管道缓冲区后会阻塞在 write 上，
        // 若先 waitFor 再 readAllBytes，只能等到超时（classpath 越长越容易触发）。
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream input = process.getInputStream()) {
                input.transferTo(buffer);
            } catch (IOException ignored) {
                // 进程被 destroy 时读取会中断，按“没有输出”处理。
            }
        }, "caml-java-inspect");
        reader.setDaemon(true);
        reader.start();

        try {
            if (!process.waitFor(INSPECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            reader.join(TimeUnit.SECONDS.toMillis(INSPECT_TIMEOUT_SECONDS));
            if (process.exitValue() != 0) {
                return Optional.empty();
            }

            Map<String, String> properties = JavaRuntimeParser.parseProperties(
                    buffer.toString(StandardCharsets.UTF_8));
            String version = properties.get("java.version");
            String architecture = JavaRuntimeParser.normalizeArchitecture(
                    properties.getOrDefault("os.arch", ""));
            int majorVersion = JavaRuntimeParser.parseMajorVersion(version);
            if (version == null || majorVersion <= 0 || architecture.isBlank()) {
                return Optional.empty();
            }

            Path home = properties.containsKey("java.home")
                    ? Path.of(properties.get("java.home"))
                    : parentOf(executable, 2);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            process.destroy();
        }
    }

    /**
     * 从 <code>&lt;jdk&gt;/release</code> 读取版本信息，作为进程探测的兜底。
     *
     * <p>`release` 是 JDK 安装目录下的固定文件，含 `JAVA_VERSION`、`OS_ARCH`、`IMPLEMENTOR`。
     * 缺少该文件（例如只拷贝了 `bin`）或版本无法解析时返回空，保持原有语义。
     */
    private Optional<JavaRuntime> fromReleaseFile(Path executable) {
        Path home = parentOf(executable, 2);
        if (home == null) {
            return Optional.empty();
        }
        Path release = home.resolve("release");
        if (!Files.isRegularFile(release)) {
            return Optional.empty();
        }

        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(release, StandardCharsets.UTF_8)) {
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            return Optional.empty();
        }

        String version = values.get("JAVA_VERSION");
        int majorVersion = JavaRuntimeParser.parseMajorVersion(version);
        String architecture = JavaRuntimeParser.normalizeArchitecture(values.getOrDefault("OS_ARCH", ""));
        if (version == null || majorVersion <= 0 || architecture.isBlank()) {
            return Optional.empty();
        }

        String vendor = values.getOrDefault("IMPLEMENTOR", "unknown");
        if (vendor.isBlank()) {
            vendor = values.getOrDefault("IMPLEMENTOR_VERSION", "unknown");
        }
        return Optional.of(new JavaRuntime(
                executable.toAbsolutePath().normalize(),
                version,
                majorVersion,
                vendor,
                architecture,
                JavaRuntimeParser.parseBits(null, architecture),
                Files.isRegularFile(home.resolve("bin").resolve(JAVAC_EXECUTABLE))
        ));
    }

    /** 取第 n 层父目录；不足 n 层时返回 null。 */
    private static Path parentOf(Path path, int levels) {
        Path current = path.toAbsolutePath().normalize();
        for (int i = 0; i < levels; i++) {
            current = current.getParent();
            if (current == null) {
                return null;
            }
        }
        return current;
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
        candidates.addAll(managedJavaExecutables(
                Path.of(System.getProperty("user.home"), ".caml", "java")));
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

    static List<Path> managedJavaExecutables(Path repositoryRoot) {
        if (!Files.isDirectory(repositoryRoot)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(repositoryRoot, 6)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(JAVA_EXECUTABLE))
                    .filter(path -> path.getParent() != null
                            && path.getParent().getFileName().toString().equals("bin"))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<Path> commonRoots() {
        String userHome = System.getProperty("user.home");
        List<Path> roots = new ArrayList<>(List.of(
                Path.of(userHome, ".jdks"),
                Path.of(userHome, ".gradle", "jdks"),
                Path.of(userHome, ".sdkman", "candidates", "java")
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