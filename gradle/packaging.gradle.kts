import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.imageio.ImageIO

project.evaluationDependsOn(":CAMLFront")

val frontProject = project(":CAMLFront")
val frontApplication = frontProject.extensions.getByType(JavaApplication::class.java)
val frontMainClass = frontApplication.mainClass.get()
val frontMainJarName = "CAMLFront-${frontProject.version}.jar"
val frontJavaToolchains = frontProject.extensions.getByType(JavaToolchainService::class.java)

val packageVersion = project.providers.gradleProperty("camlVersion").orElse("1.0.0")
val packageRelease = project.providers.gradleProperty("camlRelease").orElse("1")
val packagingDirectory = project.rootProject.layout.projectDirectory.dir("packaging").asFile
val packagesDirectory = project.layout.buildDirectory.dir("packages").get().asFile
val linuxPackageDirectory = packagesDirectory.resolve("linux")
val linuxInputDirectory = linuxPackageDirectory.resolve("input")
val linuxAppImageParent = linuxPackageDirectory.resolve("app-image")
val linuxAppImage = linuxAppImageParent.resolve("CAML")
val linuxRoot = linuxPackageDirectory.resolve("root")
val debRoot = linuxPackageDirectory.resolve("deb-root")
val linuxZuluNoticesDirectory = linuxPackageDirectory.resolve("zulu-notices")
val debOutput = linuxPackageDirectory.resolve("caml-launcher_${packageVersion.get()}-${packageRelease.get()}_amd64.deb")
val rpmTopDirectory = linuxPackageDirectory.resolve("rpmbuild")
val rpmOutput = linuxPackageDirectory.resolve("caml-launcher-${packageVersion.get()}-${packageRelease.get()}.x86_64.rpm")
val packageJdkHome = project.providers.gradleProperty("camlJdkHome")
val fallbackJdk = frontJavaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

/**
 * 打进裁剪运行时的模块集，Linux 与 Windows 共用。
 *
 * <p>少列一个模块时，安装包只会在用户点开时抛 {@code ClassNotFoundException}，
 * 因此由 {@code smokeTestPackagedApp} 校验运行时 release 文件是否覆盖这里。
 */
val packagedRuntimeModules = "java.base,java.desktop,java.logging,java.management,java.naming," +
    "java.net.http,java.prefs,java.scripting,java.sql,java.xml," +
    "jdk.management,jdk.unsupported,jdk.jfr"

fun isLinuxHost(): Boolean = System.getProperty("os.name").lowercase().contains("linux")

fun findExecutable(name: String): File? {
    val path = System.getenv("PATH") ?: return null
    val names = if (isWindowsHost() && !name.endsWith(".exe", ignoreCase = true)) {
        listOf(name, "$name.exe")
    } else {
        listOf(name)
    }
    return path.split(File.pathSeparator)
        .asSequence()
        .flatMap { directory -> names.asSequence().map { File(directory, it) } }
        .firstOrNull { it.isFile && it.canExecute() }
}

fun readJdkRelease(home: File): Pair<String, String>? {
    val release = File(home, "release")
    if (!release.isFile) return null
    val text = release.readText()
    val implementor = Regex("IMPLEMENTOR=\"([^\"]*)\"")
        .find(text)?.groupValues?.get(1).orEmpty()
    val version = Regex("JAVA_VERSION=\"([^\"]*)\"")
        .find(text)?.groupValues?.get(1).orEmpty()
    return implementor to version
}

fun describeJdk(home: File): String {
    val release = readJdkRelease(home) ?: return "无 release 文件"
    return "IMPLEMENTOR=\"${release.first}\", JAVA_VERSION=\"${release.second}\""
}

/** 打包只接受 Zulu 25：既要自带 jpackage，也要满足供应商和版本要求。 */
fun isPackageJdk(home: File, forWindows: Boolean): Boolean {
    val jpackage = if (forWindows) File(home, "bin/jpackage.exe") else File(home, "bin/jpackage")
    if (!jpackage.isFile) return false
    val release = readJdkRelease(home) ?: return false
    return release.first.contains("Azul", ignoreCase = true) && release.second.startsWith("25")
}

/** 在候选根目录下按最多两层查找 JDK（形如 <root>/<vendor>/<jdk> 或 <root>/<jdk>）。 */
fun jdkCandidatesUnder(root: File): List<File> {
    if (!root.isDirectory) return emptyList()
    val result = mutableListOf<File>()
    root.listFiles().orEmpty().sortedBy { it.name }.forEach { child ->
        if (!child.isDirectory) return@forEach
        result += child
        child.listFiles().orEmpty().sortedBy { it.name }.forEach { grandChild ->
            if (grandChild.isDirectory) result += grandChild
        }
    }
    return result
}

/** 常见 JDK 安装位置；用于在 IDE 里直接点任务、拿不到 -PcamlJdkHome 时自动兜底。 */
fun jdkSearchRoots(): List<File> {
    val roots = mutableListOf<File>()
    System.getenv("ZULU_HOME")?.trim()?.takeIf { it.isNotEmpty() }?.let { roots += File(it) }
    val userHome = File(System.getProperty("user.home"))
    roots += File(userHome, ".jdks")
    roots += File(userHome, ".sdkman/candidates/java")
    roots += File(userHome, ".gradle/jdks")
    if (isWindowsHost()) {
        listOf("ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA").forEach { name ->
            System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { base ->
                listOf("Zulu", "Azul", "Programs/Zulu", "Eclipse Adoptium", "Microsoft").forEach {
                    roots += File(base, it)
                }
            }
        }
    } else {
        roots += File("/usr/lib/jvm")
        roots += File("/usr/lib64/jvm")
        roots += File("/opt")
    }
    return roots
}

fun discoverPackageJdks(forWindows: Boolean): List<File> = jdkSearchRoots()
    .flatMap { jdkCandidatesUnder(it) }
    .distinctBy { it.absolutePath }
    .filter { isPackageJdk(it, forWindows) }

/**
 * 隐式候选：显式没有配置 camlJdkHome 时依次尝试。
 *
 * <p>在 IDE 里直接点打包任务时无法附加 -PcamlJdkHome，且 Gradle daemon 的 JAVA_HOME
 * 往往是 IDE 自身的 JDK（可能不是 Zulu 25），因此这里把 PATH 上的 jpackage、
 * Gradle 工具链和常见安装目录都当作候选，逐个用 isPackageJdk 校验。
 */
fun implicitPackageJdkCandidates(forWindows: Boolean): List<Pair<File, String>> {
    val candidates = mutableListOf<Pair<File, String>>()

    findExecutable(if (forWindows) "jpackage.exe" else "jpackage")?.let { jpackage ->
        jpackage.parentFile?.parentFile?.let { candidates += it to "PATH 中的 jpackage" }
    }
    System.getenv("JAVA_HOME")?.trim()?.takeIf { it.isNotEmpty() }?.let {
        candidates += File(it) to "JAVA_HOME"
    }
    runCatching { fallbackJdk.get().metadata.installationPath.asFile }.getOrNull()?.let {
        candidates += it to "Gradle 工具链"
    }
    discoverPackageJdks(forWindows).forEach { candidates += it to "自动发现" }

    return candidates.distinctBy { it.first.absolutePath }
}

/**
 * 解析打包用的 JDK。
 *
 * <p>显式 `-PcamlJdkHome` 必须可靠：指向非 Zulu 25 的 JDK 时直接失败，避免打出版权文本
 * 与运行时不一致的包。没有显式配置时按 implicitPackageJdkCandidates 的顺序挑第一个合格的，
 * 因此在 IDE 里直接点任务、JAVA_HOME 指向别的 JDK 也能自动找到本机 Zulu 25。
 */
fun resolvePackageJdkHome(forWindows: Boolean): File {
    val platformLabel = if (forWindows) "Windows" else "Linux"
    val expectedJdk = "Zulu JDK 25（Azul Systems）"
    val configured = packageJdkHome.orNull?.trim()?.takeIf { it.isNotEmpty() }

    if (configured != null) {
        val home = File(configured)
        if (isPackageJdk(home, forWindows)) return home
        throw GradleException(
            "$platformLabel 发布包必须使用 $expectedJdk 构建，" +
                "避免把不可再分发的 JDK 打进安装包，并保证 packaging/licenses 中的许可文本与实际运行时一致。" +
                " camlJdkHome 当前指向：${home.absolutePath}（${describeJdk(home)}）。" +
                " 请改为 -PcamlJdkHome=<Zulu 25 路径>，或在 ~/.gradle/gradle.properties 中设置 camlJdkHome。"
        )
    }

    val rejected = mutableListOf<String>()
    for ((home, origin) in implicitPackageJdkCandidates(forWindows)) {
        if (isPackageJdk(home, forWindows)) {
            project.logger.lifecycle("打包使用 $origin 的 JDK：${home.absolutePath}")
            return home
        }
        if (home.isDirectory) {
            rejected += "  - $origin: ${home.absolutePath}（${describeJdk(home)}）"
        }
    }

    throw GradleException(
        buildString {
            appendLine("$platformLabel 发布包必须使用 $expectedJdk 构建，但没有找到可用的 JDK。")
            if (rejected.isNotEmpty()) {
                appendLine("已检查：")
                rejected.forEach { appendLine(it) }
            }
            appendLine("解决办法（任选一种）：")
            appendLine("  - 命令行：-PcamlJdkHome=<Zulu 25 路径>")
            appendLine("  - IDE 里点任务：在 ~/.gradle/gradle.properties 写入 camlJdkHome=<Zulu 25 路径>")
            appendLine("  - 或设置环境变量 JAVA_HOME / ZULU_HOME 指向 Zulu 25")
        }.trimEnd()
    )
}

fun validatePackageVersion(version: String) {
    if (!version.matches(Regex("[0-9]+(\\.[0-9]+)*"))) {
        throw GradleException("camlVersion 必须是数字和点组成的版本号，例如 1.0.0；当前值：$version")
    }
}

fun validateLinuxArchitecture() {
    val architecture = System.getProperty("os.arch").lowercase()
    if (architecture != "amd64" && architecture != "x86_64") {
        throw GradleException("当前 Linux 打包仅支持 x86_64/amd64；当前架构：$architecture")
    }
}

fun installedSizeKb(root: File, excluded: File? = null): Long {
    var bytes = 0L
    root.walkTopDown()
        .filter { it.isFile }
        .filter { excluded == null || !it.toPath().startsWith(excluded.toPath()) }
        .forEach { bytes += it.length() }
    return (bytes + 1023) / 1024
}

fun copyExecutable(source: File, target: File) {
    target.parentFile.mkdirs()
    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    target.setExecutable(true, false)
    target.setReadable(true, false)
}

/** 删除只读文件：jpackage 产出的 exe/msi 带只读属性，Windows 上直接删除会 AccessDenied。 */
fun deleteReadOnly(file: File) {
    if (!file.exists()) return
    file.setWritable(true)
    if (file.isFile && !file.delete()) {
        throw GradleException("无法删除旧产物：${file.absolutePath}")
    }
}

fun copyTree(source: File, target: File) {
    source.walkTopDown().forEach { file ->
        val relative = file.relativeTo(source).path
        val destination = File(target, relative)
        if (file.isDirectory) {
            destination.mkdirs()
        } else {
            destination.parentFile.mkdirs()
            Files.copy(
                file.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
        }
    }
}

fun digest(file: File, algorithm: String): String {
    val messageDigest = MessageDigest.getInstance(algorithm)
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            messageDigest.update(buffer, 0, read)
        }
    }
    return messageDigest.digest().joinToString("") { "%02x".format(it) }
}

fun writeMd5sums(root: File, debianDirectory: File) {
    val lines = mutableListOf<String>()
    Files.walk(root.toPath()).use { paths ->
        paths.filter { Files.isRegularFile(it) }
            .filter { !it.startsWith(debianDirectory.toPath()) }
            .forEach { path ->
                val relative = root.toPath().relativize(path).toString()
                    .replace(File.separatorChar, '/')
                lines += "${digest(path.toFile(), "MD5")}  $relative"
            }
    }
    lines.sort()
    File(debianDirectory, "md5sums").writeText(lines.joinToString("\n") + "\n")
}

val cleanLinuxPackages = project.tasks.register<Delete>("cleanLinuxPackages") {
    group = "caml"
    description = "删除 Linux 打包中间文件和产物。"
    delete(linuxPackageDirectory)
}

val stageLinuxInput = project.tasks.register<Sync>("stageLinuxInput") {
    group = "caml"
    description = "收集 CAMLFront 运行时 jar，作为 jpackage 的输入。"
    dependsOn(":CAMLFront:installDist")
    onlyIf { isLinuxHost() }
    into(linuxInputDirectory)
    doFirst {
        project.delete(linuxInputDirectory)
    }
    from(frontProject.layout.buildDirectory.dir("install/CAMLFront/lib")) {
        include("*.jar")
    }
}

val packageLinuxAppImage = project.tasks.register<Exec>("packageLinuxAppImage") {
    group = "caml"
    description = "生成包含 Zulu JDK 25 和 JavaFX 的 Linux app-image。"
    dependsOn(stageLinuxInput)
    onlyIf { isLinuxHost() }

    doFirst {
        val version = packageVersion.get()
        validatePackageVersion(version)
        validateLinuxArchitecture()
        val jdkHome = resolvePackageJdkHome(false)
        project.delete(linuxAppImageParent)
        linuxAppImageParent.mkdirs()

        executable = File(jdkHome, "bin/jpackage").absolutePath
        args = listOf(
            "--type", "app-image",
            "--name", "CAML",
            "--app-version", version,
            "--vendor", "CAML Contributors",
            "--description", "CAML Minecraft Launcher",
            "--copyright", "Copyright (C) 2026 CAML Contributors",
            "--icon", File(packagingDirectory, "icons/caml-512.png").absolutePath,
            "--input", linuxInputDirectory.absolutePath,
            "--dest", linuxAppImageParent.absolutePath,
            "--main-jar", frontMainJarName,
            "--main-class", frontMainClass,
            "--java-options", "--module-path=\$APPDIR",
            "--java-options", "--add-modules=javafx.controls,javafx.fxml",
            "--java-options", "--enable-native-access=ALL-UNNAMED,javafx.graphics",
            "--add-modules",
            packagedRuntimeModules,
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files",
            "--verbose"
        )
    }
}

val stageLinuxZuluNotices = project.tasks.register("stageLinuxZuluNotices") {
    group = "caml"
    description = "收集 Zulu JDK 的 DISCLAIMER 和 readme.txt。"
    onlyIf { isLinuxHost() }

    doLast {
        val jdkHome = resolvePackageJdkHome(false)
        linuxZuluNoticesDirectory.deleteRecursively()
        linuxZuluNoticesDirectory.mkdirs()
        listOf("DISCLAIMER", "readme.txt").forEach { name ->
            val source = File(jdkHome, name)
            if (!source.isFile) {
                throw GradleException("Zulu JDK 缺少许可证文件：${source.absolutePath}")
            }
            Files.copy(
                source.toPath(),
                File(linuxZuluNoticesDirectory, name).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}

val stageLinuxRoot = project.tasks.register<Sync>("stageLinuxRoot") {
    group = "caml"
    description = "生成 deb 和 rpm 共用的 Linux 根文件系统。"
    dependsOn(packageLinuxAppImage, stageLinuxZuluNotices)
    onlyIf { isLinuxHost() }
    into(linuxRoot)
    doFirst {
        project.delete(linuxRoot)
    }

    from(linuxAppImage) {
        into("opt/caml-launcher")
    }
    from(File(packagingDirectory, "linux/caml")) {
        into("usr/bin")
        filePermissions {
            unix("0755")
        }
    }
    from(File(packagingDirectory, "linux/caml.desktop")) {
        into("usr/share/applications")
    }
    from(File(packagingDirectory, "icons/caml.png")) {
        into("usr/share/icons/hicolor/256x256/apps")
    }
    from(File(packagingDirectory, "icons/caml-512.png")) {
        into("usr/share/icons/hicolor/512x512/apps")
        rename { "caml.png" }
    }
    from(File(packagingDirectory, "linux/copyright")) {
        into("usr/share/doc/caml-launcher")
    }
    from(project.rootProject.file("LICENSE")) {
        into("usr/share/doc/caml-launcher/licenses")
        rename { "CAML-GPL-3.0-only.txt" }
    }
    from(File(packagingDirectory, "licenses")) {
        into("usr/share/doc/caml-launcher/licenses")
    }
    from(linuxZuluNoticesDirectory) {
        into("usr/share/doc/caml-launcher/licenses/Zulu")
    }
    from(File(linuxAppImage, "lib/runtime/legal")) {
        into("usr/share/doc/caml-launcher/licenses/JDK")
    }
    from(File(linuxAppImage, "lib/runtime/legal")) {
        into("usr/share/licenses/caml-launcher/JDK")
    }
    from(project.rootProject.file("LICENSE")) {
        into("usr/share/licenses/caml-launcher")
        rename { "CAML-GPL-3.0-only.txt" }
    }
    from(File(packagingDirectory, "licenses")) {
        into("usr/share/licenses/caml-launcher")
    }
    from(linuxZuluNoticesDirectory) {
        into("usr/share/licenses/caml-launcher/Zulu")
    }
}

val stageDebRoot = project.tasks.register<Sync>("stageDebRoot") {
    group = "caml"
    description = "生成 Debian 安装根目录。"
    dependsOn(stageLinuxRoot)
    onlyIf { isLinuxHost() }
    into(debRoot)
    doFirst {
        project.delete(debRoot)
    }
    from(linuxRoot)
}

val packageDeb = project.tasks.register<Exec>("packageDeb") {
    group = "caml"
    description = "生成 caml-launcher Debian 安装包。"
    dependsOn(stageDebRoot)
    onlyIf { isLinuxHost() }

    doFirst {
        val version = packageVersion.get()
        val release = packageRelease.get()
        validatePackageVersion(version)
        val debianDirectory = debRoot.resolve("DEBIAN")
        val installedSize = installedSizeKb(debRoot, debianDirectory)
        debianDirectory.mkdirs()

        File(debianDirectory, "control").writeText(
            """
            Package: caml-launcher
            Version: $version-$release
            Section: games
            Priority: optional
            Architecture: amd64
            Maintainer: CrimsonSirius <227993016@qq.com>
            Installed-Size: $installedSize
            Depends: libc6, libgtk-3-0, libx11-6, libxext6, libxrender1, libxtst6, libxi6, libxxf86vm1, libgl1, fontconfig, libfreetype6, libasound2
            Homepage: https://github.com/CrimsonSirius/CAML
            Description: CAML Minecraft Launcher
             CAML is a JavaFX-based Minecraft launcher.
            """.trimIndent() + "\n"
        )
        copyExecutable(File(packagingDirectory, "linux/postinst"), File(debianDirectory, "postinst"))
        copyExecutable(File(packagingDirectory, "linux/postrm"), File(debianDirectory, "postrm"))
        writeMd5sums(debRoot, debianDirectory)

        if (findExecutable("dpkg-deb") == null) {
            throw GradleException("未找到 dpkg-deb。Arch 上请安装 dpkg：sudo pacman -S dpkg")
        }
        project.delete(debOutput)
        executable = "dpkg-deb"
        args = listOf(
            "--build",
            "--root-owner-group",
            debRoot.absolutePath,
            debOutput.absolutePath
        )
    }
}

val packageRpm = project.tasks.register<Exec>("packageRpm") {
    group = "caml"
    description = "生成 caml-launcher RPM 安装包。"
    dependsOn(stageLinuxRoot)
    onlyIf { isLinuxHost() }

    doFirst {
        val version = packageVersion.get()
        val release = packageRelease.get()
        validatePackageVersion(version)
        project.delete(rpmTopDirectory)
        listOf("BUILD", "BUILDROOT", "RPMS", "SOURCES", "SPECS", "SRPMS").forEach {
            File(rpmTopDirectory, it).mkdirs()
        }
        File(rpmTopDirectory, "rpmdb").mkdirs()

        copyTree(linuxRoot, File(rpmTopDirectory, "SOURCES/root"))
        val sourceSpec = File(packagingDirectory, "rpm/caml-launcher.spec").readText()
            .replace("@VERSION@", version)
            .replace("@RELEASE@", release)
        val spec = File(rpmTopDirectory, "SPECS/caml-launcher.spec")
        spec.writeText(sourceSpec)

        if (findExecutable("rpmbuild") == null) {
            throw GradleException("未找到 rpmbuild。Arch 上请安装 rpm-tools：sudo pacman -S rpm-tools")
        }
        executable = "rpmbuild"
        args = listOf(
            "-bb",
            "--define", "_topdir ${rpmTopDirectory.absolutePath}",
            "--define", "_dbpath ${File(rpmTopDirectory, "rpmdb").absolutePath}",
            spec.absolutePath
        )
    }

    doLast {
        val version = packageVersion.get()
        val release = packageRelease.get()
        val builtRpm = File(
            rpmTopDirectory,
            "RPMS/x86_64/caml-launcher-$version-$release.x86_64.rpm"
        )
        if (!builtRpm.isFile) {
            throw GradleException("rpmbuild 未生成预期文件：${builtRpm.absolutePath}")
        }
        Files.copy(builtRpm.toPath(), rpmOutput.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

val packageLinux = project.tasks.register("packageLinux") {
    group = "caml"
    description = "生成 Linux deb、rpm 和 SHA256SUMS.txt。"
    dependsOn(packageDeb, packageRpm)
    onlyIf { isLinuxHost() }

    doLast {
        val artifacts = listOf(debOutput, rpmOutput).filter { it.isFile }
        if (artifacts.size != 2) {
            throw GradleException("Linux 打包未生成完整的 deb/rpm 产物。")
        }
        File(linuxPackageDirectory, "SHA256SUMS.txt").writeText(
            artifacts.joinToString("\n") { "${digest(it, "SHA-256")}  ${it.name}" } + "\n"
        )
    }
}

fun isWindowsHost(): Boolean = System.getProperty("os.name").lowercase().contains("win")

fun validateWindowsArchitecture() {
    val architecture = System.getProperty("os.arch").lowercase()
    if (architecture != "amd64" && architecture != "x86_64") {
        throw GradleException("当前 Windows 打包仅支持 x64/amd64；当前架构：$architecture")
    }
}

/** EXE/MSI 的 ProductVersion 只接受 major.minor.build，且每段 0-255。 */
fun validateWindowsVersion(version: String) {
    val parts = version.split('.')
    if (parts.size < 1 || parts.size > 3) {
        throw GradleException(
            "Windows 安装包的版本号必须是 major.minor.build 形式（每段 0-255）；当前值：$version"
        )
    }
    for (part in parts) {
        val number = part.toIntOrNull()
        if (number == null || number !in 0..255) {
            throw GradleException(
                "Windows 安装包的版本号必须是 major.minor.build 形式（每段 0-255）；当前值：$version"
            )
        }
    }
}

/** jpackage 生成 exe/msi 时依赖 WiX Toolset，提前给出可读的错误提示。 */
fun requireWix() {
    if (findExecutable("wix") == null &&
        findExecutable("light") == null &&
        findExecutable("candle") == null) {
        throw GradleException(
            "未找到 WiX Toolset。请安装 WiX 3.14 或 WiX 4.0.4+ 并把 bin 目录加入 PATH：" +
                "https://wixtoolset.org/docs/download/ 。" +
                " 只想要免安装包时改用 :packageWindowsPortable（不需要 WiX）。"
        )
    }
}

val windowsPackageDirectory = packagesDirectory.resolve("windows")
val windowsInputDirectory = windowsPackageDirectory.resolve("input")
val windowsZuluNoticesDirectory = windowsPackageDirectory.resolve("zulu-notices")
val windowsLicensesDirectory = windowsPackageDirectory.resolve("licenses")
val windowsAppImageParent = windowsPackageDirectory.resolve("app-image")
val windowsAppImage = windowsAppImageParent.resolve("CAML")
val windowsIcon = windowsPackageDirectory.resolve("caml.ico")
val windowsExeOutput = windowsPackageDirectory.resolve(
    "caml-launcher-${packageVersion.get()}-${packageRelease.get()}-windows-x64.exe"
)
val windowsPortableOutput = windowsPackageDirectory.resolve(
    "caml-launcher-${packageVersion.get()}-${packageRelease.get()}-windows-x64-portable.zip"
)

/**
 * 固定的升级 UUID：jpackage 用它识别“同一产品的不同版本”，
 * 一旦发布就不能再改，否则新版本会与旧版本并存而不是升级。
 */
val windowsUpgradeUuid = "6f1c2b4a-8f2d-4e57-9d3a-1b7c5e9a2f60"

fun scaleIcon(source: BufferedImage, size: Int): BufferedImage {
    val target = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = target.createGraphics()
    try {
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(source, 0, 0, size, size, null)
    } finally {
        graphics.dispose()
    }
    return target
}

fun littleEndian(value: Int): ByteArray = byteArrayOf(
    (value and 0xFF).toByte(),
    ((value shr 8) and 0xFF).toByte(),
    ((value shr 16) and 0xFF).toByte(),
    ((value shr 24) and 0xFF).toByte()
)

/** 把一张 ARGB 位图编码成 ICO 中的 32bpp DIB 数据（含 AND 掩码）。 */
fun icoImageData(image: BufferedImage): ByteArray {
    val width = image.width
    val height = image.height
    val out = ByteArrayOutputStream()
    val data = DataOutputStream(out)

    data.write(littleEndian(40))                 // BITMAPINFOHEADER 大小
    data.write(littleEndian(width))
    data.write(littleEndian(height * 2))         // 高度包含 AND 掩码
    data.write(byteArrayOf(1, 0))                // planes
    data.write(byteArrayOf(32, 0))               // 每像素位数
    data.write(littleEndian(0))                  // BI_RGB
    data.write(littleEndian(width * height * 4))
    repeat(4) { data.write(littleEndian(0)) }    // 分辨率与调色板信息

    for (y in height - 1 downTo 0) {
        for (x in 0 until width) {
            val argb = image.getRGB(x, y)
            data.write(argb and 0xFF)                // B
            data.write((argb shr 8) and 0xFF)        // G
            data.write((argb shr 16) and 0xFF)       // R
            data.write((argb ushr 24) and 0xFF)      // A
        }
    }

    // AND 掩码全 0（透明度由 alpha 通道表达），每行按 4 字节对齐。
    val maskRow = ByteArray(((width + 31) / 32) * 4)
    repeat(height) { data.write(maskRow) }

    data.flush()
    return out.toByteArray()
}

fun writeIco(target: File, source: BufferedImage, sizes: List<Int>) {
    val images = sizes.map { icoImageData(scaleIcon(source, it)) }
    val out = ByteArrayOutputStream()
    val data = DataOutputStream(out)

    data.write(byteArrayOf(0, 0, 1, 0))          // reserved, type = icon
    data.write(byteArrayOf(sizes.size.toByte(), 0))

    var offset = 6 + 16 * sizes.size
    sizes.forEachIndexed { index, size ->
        val dimension = if (size >= 256) 0 else size
        data.write(dimension)
        data.write(dimension)
        data.write(0)                            // 调色板颜色数
        data.write(0)                            // 保留
        data.write(byteArrayOf(1, 0))            // planes
        data.write(byteArrayOf(32, 0))           // 每像素位数
        data.write(littleEndian(images[index].size))
        data.write(littleEndian(offset))
        offset += images[index].size
    }
    images.forEach { data.write(it) }

    data.flush()
    target.parentFile.mkdirs()
    target.writeBytes(out.toByteArray())
}

val cleanWindowsPackages = project.tasks.register<Delete>("cleanWindowsPackages") {
    group = "caml"
    description = "删除 Windows 打包中间文件和产物。"
    delete(windowsPackageDirectory)
}

val generateWindowsIcon = project.tasks.register("generateWindowsIcon") {
    group = "caml"
    description = "由 caml-512.png 生成 Windows 安装包所需的多尺寸 caml.ico。"
    val source = File(packagingDirectory, "icons/caml-512.png")
    inputs.file(source)
    outputs.file(windowsIcon)

    doLast {
        val image = ImageIO.read(source)
            ?: throw GradleException("无法读取图标源文件：${source.absolutePath}")
        if (image.width < 256 || image.height < 256) {
            throw GradleException("图标源文件至少需要 256x256：${source.absolutePath}")
        }
        writeIco(windowsIcon, image, listOf(16, 24, 32, 48, 64, 128, 256))
    }
}

val stageWindowsZuluNotices = project.tasks.register("stageWindowsZuluNotices") {
    group = "caml"
    description = "收集 Zulu JDK 的 DISCLAIMER 和 readme.txt。"
    onlyIf { isWindowsHost() }

    doLast {
        val jdkHome = resolvePackageJdkHome(true)
        windowsZuluNoticesDirectory.deleteRecursively()
        windowsZuluNoticesDirectory.mkdirs()
        listOf("DISCLAIMER", "readme.txt").forEach { name ->
            val source = File(jdkHome, name)
            if (!source.isFile) {
                throw GradleException("Zulu JDK 缺少许可证文件：${source.absolutePath}")
            }
            Files.copy(
                source.toPath(),
                File(windowsZuluNoticesDirectory, name).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}

val stageWindowsLicenses = project.tasks.register<Sync>("stageWindowsLicenses") {
    group = "caml"
    description = "生成 Windows 安装包使用的许可目录，并加入 Zulu 通知。"
    dependsOn(stageWindowsZuluNotices)
    onlyIf { isWindowsHost() }
    into(windowsLicensesDirectory)
    doFirst {
        project.delete(windowsLicensesDirectory)
    }
    from(File(packagingDirectory, "licenses"))
    from(windowsZuluNoticesDirectory) {
        into("Zulu")
    }
}

val stageWindowsInput = project.tasks.register<Sync>("stageWindowsInput") {
    group = "caml"
    description = "收集 CAMLFront 运行时 jar，作为 Windows jpackage 的输入。"
    dependsOn(":CAMLFront:installDist")
    onlyIf { isWindowsHost() }
    into(windowsInputDirectory)
    doFirst {
        project.delete(windowsInputDirectory)
    }
    from(frontProject.layout.buildDirectory.dir("install/CAMLFront/lib")) {
        include("*.jar")
    }
}

val packageWindowsAppImage = project.tasks.register<Exec>("packageWindowsAppImage") {
    group = "caml"
    description = "生成包含 Zulu JDK 25 和 JavaFX 的 Windows app-image。"
    dependsOn(stageWindowsInput, generateWindowsIcon, stageWindowsLicenses)
    onlyIf { isWindowsHost() }

    doFirst {
        val version = packageVersion.get()
        validatePackageVersion(version)
        validateWindowsVersion(version)
        validateWindowsArchitecture()
        val jdkHome = resolvePackageJdkHome(true)
        project.delete(windowsAppImageParent)
        windowsAppImageParent.mkdirs()

        executable = File(jdkHome, "bin/jpackage.exe").absolutePath
        args = listOf(
            "--type", "app-image",
            "--name", "CAML",
            "--app-version", version,
            "--vendor", "CAML Contributors",
            "--description", "CAML Minecraft Launcher",
            "--copyright", "Copyright (C) 2026 CAML Contributors",
            "--icon", windowsIcon.absolutePath,
            "--input", windowsInputDirectory.absolutePath,
            "--dest", windowsAppImageParent.absolutePath,
            "--main-jar", frontMainJarName,
            "--main-class", frontMainClass,
            "--java-options", "--module-path=\$APPDIR",
            "--java-options", "--add-modules=javafx.controls,javafx.fxml",
            "--java-options", "--enable-native-access=ALL-UNNAMED,javafx.graphics",
            "--add-modules",
            packagedRuntimeModules,
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files",
            "--app-content", windowsLicensesDirectory.absolutePath,
            "--app-content", project.rootProject.file("LICENSE").absolutePath,
            "--verbose"
        )
    }
}

/**
 * 免安装便携包：直接打包 app-image，不需要 WiX。
 *
 * <p>没有安装 WiX、或只想就地试用时用它；安装式发布仍以 EXE 为准。
 */
val packageWindowsPortable = project.tasks.register<Zip>("packageWindowsPortable") {
    group = "caml"
    description = "生成 Windows 免安装便携包（zip，不需要 WiX）。"
    dependsOn(packageWindowsAppImage)
    onlyIf { isWindowsHost() }

    archiveFileName.set(windowsPortableOutput.name)
    destinationDirectory.set(windowsPackageDirectory)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    from(windowsAppImage) {
        into("CAML")
    }
    from(project.rootProject.file("LICENSE")) {
        into("CAML")
        rename { "CAML-GPL-3.0-only.txt" }
    }
}

val packageWindowsExe = project.tasks.register<Exec>("packageWindowsExe") {
    group = "caml"
    description = "生成 caml-launcher Windows EXE 安装包（per-machine，含桌面快捷方式选项）。"
    dependsOn(stageWindowsInput, generateWindowsIcon, stageWindowsLicenses)
    onlyIf { isWindowsHost() }

    doFirst {
        val version = packageVersion.get()
        validatePackageVersion(version)
        validateWindowsVersion(version)
        validateWindowsArchitecture()
        requireWix()
        val jdkHome = resolvePackageJdkHome(true)
        windowsPackageDirectory.mkdirs()
        // 旧产物带只读属性，jpackage 无法覆盖，先清掉本次要重新生成的 exe。
        deleteReadOnly(File(windowsPackageDirectory, "CAML-$version.exe"))
        deleteReadOnly(windowsExeOutput)

        executable = File(jdkHome, "bin/jpackage.exe").absolutePath
        args = listOf(
            "--type", "exe",
            "--name", "CAML",
            "--app-version", version,
            "--vendor", "CAML Contributors",
            "--description", "CAML Minecraft Launcher",
            "--copyright", "Copyright (C) 2026 CAML Contributors",
            "--icon", windowsIcon.absolutePath,
            "--input", windowsInputDirectory.absolutePath,
            "--dest", windowsPackageDirectory.absolutePath,
            "--main-jar", frontMainJarName,
            "--main-class", frontMainClass,
            "--java-options", "--module-path=\$APPDIR",
            "--java-options", "--add-modules=javafx.controls,javafx.fxml",
            "--java-options", "--enable-native-access=ALL-UNNAMED,javafx.graphics",
            "--add-modules", packagedRuntimeModules,
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files",
            "--app-content", windowsLicensesDirectory.absolutePath,
            "--app-content", project.rootProject.file("LICENSE").absolutePath,
            "--license-file", project.rootProject.file("LICENSE").absolutePath,
            "--win-menu",
            "--win-menu-group", "CAML",
            "--win-shortcut",
            // 安装时询问是否创建快捷方式；复选框默认勾选，用户可取消。
            "--win-shortcut-prompt",
            "--win-dir-chooser",
            "--win-upgrade-uuid", windowsUpgradeUuid,
            "--win-help-url", "https://github.com/CrimsonSirius/CAML",
            "--verbose"
        )
    }

    doLast {
        val builtExe = File(windowsPackageDirectory, "CAML-${packageVersion.get()}.exe")
        if (!builtExe.isFile) {
            throw GradleException("jpackage 未生成预期文件：${builtExe.absolutePath}")
        }
        // jpackage 产出的 exe 带只读属性，Windows 上不能直接覆盖，先删掉旧产物再移动。
        deleteReadOnly(windowsExeOutput)
        Files.move(
            builtExe.toPath(),
            windowsExeOutput.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

val packageWindows = project.tasks.register("packageWindows") {
    group = "caml"
    description = "生成 Windows EXE 安装包、免安装便携包和 SHA256SUMS.txt。"
    dependsOn(packageWindowsExe, packageWindowsPortable)
    onlyIf { isWindowsHost() }

    doLast {
        val artifacts = listOf(windowsExeOutput, windowsPortableOutput).filter { it.isFile }
        if (artifacts.size != 2) {
            throw GradleException("Windows 打包未生成完整的 exe/zip 产物。")
        }
        File(windowsPackageDirectory, "SHA256SUMS.txt").writeText(
            artifacts.joinToString("\n") { "${digest(it, "SHA-256")}  ${it.name}" } + "\n"
        )
    }
}

val collectPackages = project.tasks.register("collectPackages") {
    group = "caml"
    description = "生成当前平台发布包，并在 build/packages 生成统一的 SHA256SUMS.txt。"
    dependsOn(packageLinux, packageWindowsPortable)

    doLast {
        val artifacts = buildList {
            addAll(linuxPackageDirectory.listFiles().orEmpty().filter {
                it.isFile && it.extension in setOf("deb", "rpm")
            })
            addAll(windowsPackageDirectory.listFiles().orEmpty().filter {
                it.isFile && it.extension in setOf("exe", "zip")
            })
        }.sortedBy { it.relativeTo(packagesDirectory).invariantSeparatorsPath }
        if (artifacts.isEmpty()) {
            throw GradleException(
                "没有可汇总的发布包。请先运行 packageLinux 或 packageWindowsPortable。"
            )
        }
        // 条目带上 linux/、windows/ 前缀，才能在 build/packages 下直接 sha256sum -c。
        File(packagesDirectory, "SHA256SUMS.txt").writeText(
            artifacts.joinToString("\n") {
                val relative = it.relativeTo(packagesDirectory).invariantSeparatorsPath
                "${digest(it, "SHA-256")}  $relative"
            } + "\n"
        )
        project.logger.lifecycle("汇总 ${artifacts.size} 个发布包到 ${packagesDirectory.absolutePath}")
    }
}

val printPackagingJdk = project.tasks.register("printPackagingJdk") {
    group = "caml"
    description = "打印打包会使用的 JDK 和候选清单，用于排查供应商/版本不匹配。"

    doLast {
        val forWindows = isWindowsHost()
        logger.lifecycle("平台：${System.getProperty("os.name")}（${System.getProperty("os.arch")}）")
        logger.lifecycle("显式配置 camlJdkHome：${packageJdkHome.orNull ?: "（未设置）"}")
        logger.lifecycle("候选清单：")
        implicitPackageJdkCandidates(forWindows).forEach { (home, origin) ->
            val status = if (isPackageJdk(home, forWindows)) "可用" else "跳过"
            logger.lifecycle("  [$status] $origin -> ${home.absolutePath}（${describeJdk(home)}）")
        }
        try {
            logger.lifecycle("打包将使用：${resolvePackageJdkHome(forWindows).absolutePath}")
        } catch (exception: GradleException) {
            logger.lifecycle(exception.message.orEmpty())
        }
    }
}

/** 运行外部进程并继承标准输入输出；非 0 退出码直接失败。 */
fun runPackagingProcess(command: List<String>, workingDirectory: File, label: String) {
    val exitCode = ProcessBuilder(command)
        .directory(workingDirectory)
        .inheritIO()
        .start()
        .waitFor()
    if (exitCode != 0) {
        throw GradleException("$label 失败（退出码 $exitCode）：${command.joinToString(" ")}")
    }
}

/**
 * 发布包冒烟校验：用 app-image 自身的 `*.cfg` 启动后端。
 *
 * <p>为什么不直接用固定的 jvm 参数：安装包的启动参数由 jpackage 写进 cfg，
 * 冒烟校验必须复用同一份参数，否则改了 `--java-options` 时校验会悄悄失效。
 *
 * <p>为什么还要检查运行时：`--add-modules` 少列一个模块时后端在本机 JDK 上照常运行，
 * 只有裁剪运行时里没有该模块才会在用户机器上抛 ClassNotFoundException。
 */
val smokeTestPackagedApp = project.tasks.register("smokeTestPackagedApp") {
    group = "caml"
    description = "用 app-image 自身的启动参数启动后端，校验模块布局与裁剪运行时。"

    doLast {
        val forWindows = isWindowsHost()
        val appImage = if (forWindows) windowsAppImage else linuxAppImage
        if (!appImage.isDirectory) {
            throw GradleException(
                "未找到 app-image：${appImage.absolutePath}。请先运行 " +
                    (if (forWindows) "packageWindowsAppImage" else "packageLinuxAppImage") + "。"
            )
        }
        val cfgFile = appImage.walkTopDown().firstOrNull { it.isFile && it.extension == "cfg" }
            ?: throw GradleException("app-image 中缺少启动配置（*.cfg）：${appImage.absolutePath}")
        val appDirectory = cfgFile.parentFile
        logger.lifecycle("冒烟校验 app-image：${appImage.absolutePath}")
        logger.lifecycle("启动配置：${cfgFile.absolutePath}")

        fun expandAppDir(value: String): String =
            value.replace("\$APPDIR", appDirectory.absolutePath).replace('\\', File.separatorChar)

        val javaOptions = mutableListOf<String>()
        val classpathEntries = mutableListOf<String>()
        cfgFile.readLines().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("java-options=") ->
                    javaOptions += expandAppDir(line.removePrefix("java-options="))

                line.startsWith("app.classpath=") ->
                    classpathEntries += expandAppDir(line.removePrefix("app.classpath="))
            }
        }
        if (classpathEntries.isEmpty()) {
            throw GradleException("启动配置中没有 app.classpath：${cfgFile.absolutePath}")
        }
        val modulePathOption = "--module-path=${appDirectory.absolutePath}"
        if (javaOptions.none { it == modulePathOption }) {
            throw GradleException(
                "启动配置没有把 app 目录放进 --module-path，本次校验无法复现安装包的模块布局：" +
                    "${cfgFile.absolutePath}"
            )
        }

        val jdkHome = runCatching { resolvePackageJdkHome(forWindows) }.getOrElse {
            logger.lifecycle("没有找到 Zulu 25，改用当前 Gradle JVM 执行冒烟校验")
            File(System.getProperty("java.home"))
        }
        val javac = File(jdkHome, if (forWindows) "bin/javac.exe" else "bin/javac")
        val javaExecutable = File(jdkHome, if (forWindows) "bin/java.exe" else "bin/java")
        if (!javac.isFile || !javaExecutable.isFile) {
            throw GradleException("冒烟校验需要完整 JDK（含 javac）：${jdkHome.absolutePath}")
        }

        val smokeDirectory = project.layout.buildDirectory.dir("smoke").get().asFile
        val classesDirectory = File(smokeDirectory, "classes")
        project.delete(classesDirectory)
        classesDirectory.mkdirs()
        val appJars = File(appDirectory, "*").absolutePath
        runPackagingProcess(
            listOf(
                javac.absolutePath,
                "-encoding", "UTF-8",
                "-d", classesDirectory.absolutePath,
                "-cp", appJars,
                File(packagingDirectory, "smoke/BackendSmoke.java").absolutePath
            ),
            project.projectDir,
            "编译冒烟校验程序"
        )

        val dataDirectory = File(smokeDirectory, "data")
        val gameDirectory = File(smokeDirectory, "game")
        project.delete(dataDirectory, gameDirectory)
        runPackagingProcess(
            listOf(javaExecutable.absolutePath) + javaOptions + listOf(
                "-cp", classesDirectory.absolutePath + File.pathSeparator + appJars,
                "BackendSmoke",
                dataDirectory.absolutePath,
                gameDirectory.absolutePath
            ),
            project.projectDir,
            "启动打包后的后端"
        )

        val runtimeDirectory = appImage.walkTopDown()
            .firstOrNull { it.isDirectory && it.name == "runtime" }
            ?: throw GradleException("app-image 中缺少运行时目录：${appImage.absolutePath}")
        val releaseFile = File(runtimeDirectory, "release")
        if (!releaseFile.isFile) {
            throw GradleException("裁剪运行时缺少 release 文件：${releaseFile.absolutePath}")
        }
        val modules = Regex("MODULES=\"([^\"]*)\"")
            .find(releaseFile.readText())
            ?.groupValues?.get(1)
            ?.split(' ')
            ?.filter { it.isNotBlank() }
            ?: throw GradleException("无法解析裁剪运行时的 MODULES：${releaseFile.absolutePath}")
        val missingModules = packagedRuntimeModules.split(',').filterNot { it in modules }
        if (missingModules.isNotEmpty()) {
            throw GradleException(
                "裁剪运行时缺少模块：${missingModules.joinToString(", ")}。" +
                    "这种安装包只会在用户点开时抛出 ClassNotFoundException，请同步 --add-modules。"
            )
        }
        val nativeLauncher = File(runtimeDirectory, if (forWindows) "bin/java.exe" else "bin/java")
        if (nativeLauncher.isFile) {
            throw GradleException(
                "运行时仍带有 Java 启动器（${nativeLauncher.absolutePath}），" +
                    "--strip-native-commands 没有生效。"
            )
        }
        logger.lifecycle(
            "冒烟校验通过：模块 ${modules.size} 个，app jar ${classpathEntries.size} 个"
        )
    }
}
