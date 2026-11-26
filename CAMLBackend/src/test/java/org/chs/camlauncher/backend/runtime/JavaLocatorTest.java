package org.chs.camlauncher.backend.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLocatorTest {
    @TempDir
    Path tempDirectory;

    /** Windows 的可执行文件带 .exe，JavaLocator 也只在对应平台匹配该文件名。 */
    private static String javaExecutableName() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    }

    @Test
    void inspectsCurrentJavaRuntime() {
        Path home = Path.of(System.getProperty("java.home"));
        Path executable = home.resolve("bin").resolve(javaExecutableName());
        assertTrue(Files.isRegularFile(executable));

        JavaRuntime runtime = new JavaLocator().inspect(executable).orElseThrow();

        assertEquals(Runtime.version().feature(), runtime.majorVersion());
        assertTrue(runtime.bits() == 32 || runtime.bits() == 64);
    }

    @Test
    void mergesManualAndScannedJavaWithoutDuplicates() {
        Path home = Path.of(System.getProperty("java.home"));
        Path executable = home.resolve("bin").resolve(javaExecutableName());

        List<JavaCandidate> candidates = new JavaLocator().discoverCandidates(
                List.of(executable, executable), executable);
        List<JavaCandidate> manualCandidates = candidates.stream()
                .filter(JavaCandidate::manual)
                .toList();

        assertEquals(1, manualCandidates.size());
        assertTrue(manualCandidates.getFirst().available());
    }

    @Test
    void scansNestedManagedJavaRepository() throws Exception {
        Path repository = tempDirectory.resolve("java");
        Path executable = repository.resolve("linux-x86_64/zulu-8-jdk/bin").resolve(javaExecutableName());
        Files.createDirectories(executable.getParent());
        Files.createFile(executable);

        List<Path> executables = JavaLocator.managedJavaExecutables(repository);

        assertEquals(List.of(executable), executables);
    }

    /**
     * 进程探测不可用时（无法执行、超时、被安全软件拦截）必须能从 JDK 的 release 文件读出信息。
     *
     * <p>CI 上曾经因为只能靠子进程探测而整体扫不到 Java，导致启动、加载器安装、候选列表
     * 相关用例连锁失败；这里用“不是可执行文件”的占位文件固定该兜底行为。
     */
    @Test
    void readsVersionFromReleaseFileWhenProbeCannotRun() throws Exception {
        Path home = tempDirectory.resolve("fake-jdk");
        Path bin = home.resolve("bin");
        Files.createDirectories(bin);
        Path executable = bin.resolve(javaExecutableName());
        Files.writeString(executable, "not a real java binary");
        Files.writeString(bin.resolve(
                javaExecutableName().equals("java.exe") ? "javac.exe" : "javac"), "not a real javac");
        Files.writeString(home.resolve("release"), """
                IMPLEMENTOR="Test Vendor"
                JAVA_VERSION="17.0.9"
                JAVA_VERSION_DATE="2024-01-16"
                OS_ARCH="x86_64"
                OS_NAME="Windows"
                IMAGE_TYPE="JDK"
                """);

        JavaRuntime runtime = new JavaLocator().inspect(executable).orElseThrow();

        assertEquals(17, runtime.majorVersion());
        assertEquals("17.0.9", runtime.version());
        assertEquals("x86_64", runtime.architecture());
        assertEquals(64, runtime.bits());
        assertEquals("Test Vendor", runtime.vendor());
        assertTrue(runtime.jdk());
    }

    @Test
    void ignoresDirectoryWithoutReleaseFile() throws Exception {
        Path bin = tempDirectory.resolve("bare/bin");
        Files.createDirectories(bin);
        Path executable = bin.resolve(javaExecutableName());
        Files.writeString(executable, "not a real java binary");

        assertTrue(new JavaLocator().inspect(executable).isEmpty());
    }
}