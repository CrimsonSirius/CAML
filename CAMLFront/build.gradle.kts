import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

dependencies {
    implementation(project(":CAMLBackend"))
    implementation(project(":CAMLUI"))
    implementation(project(":CAMLLanguage"))
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

javafx {
    version = libs.versions.javafx.get()
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.chs.camlauncher.front.CAMLApplication")
}

val packageVersion = providers.gradleProperty("camlVersion").orElse("1.0.0")
val packageRelease = providers.gradleProperty("camlRelease").orElse("1")
val packagingDirectory = rootProject.layout.projectDirectory.dir("packaging").asFile
val linuxPackageDirectory = layout.buildDirectory.dir("packages/linux").get().asFile
val linuxInputDirectory = linuxPackageDirectory.resolve("input")
val linuxAppImageParent = linuxPackageDirectory.resolve("app-image")
val linuxAppImage = linuxAppImageParent.resolve("CAML")
val linuxRoot = linuxPackageDirectory.resolve("root")
val debRoot = linuxPackageDirectory.resolve("deb-root")
val debOutput = linuxPackageDirectory.resolve("caml-launcher_${packageVersion.get()}-${packageRelease.get()}_amd64.deb")
val rpmTopDirectory = linuxPackageDirectory.resolve("rpmbuild")
val rpmOutput = linuxPackageDirectory.resolve("caml-launcher-${packageVersion.get()}-${packageRelease.get()}.x86_64.rpm")
val packageJdkHome = providers.gradleProperty("camlJdkHome")
    .orElse(providers.environmentVariable("JAVA_HOME"))
val fallbackJdk = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

fun isLinuxHost(): Boolean = System.getProperty("os.name").lowercase().contains("linux")

fun findExecutable(name: String): File? {
    val path = System.getenv("PATH") ?: return null
    return path.split(File.pathSeparator)
        .asSequence()
        .map { File(it, name) }
        .firstOrNull { it.isFile && it.canExecute() }
}

fun resolvePackageJdkHome(): File {
    val configured = packageJdkHome.orNull?.trim()?.takeIf { it.isNotEmpty() }
    val home = if (configured != null) {
        File(configured)
    } else {
        fallbackJdk.get().metadata.installationPath.asFile
    }

    if (!File(home, "bin/jpackage").isFile) {
        throw GradleException(
            "未找到 jpackage：${home}/bin/jpackage。请把 camlJdkHome 或 JAVA_HOME 指向 Temurin JDK 25。"
        )
    }

    val releaseText = File(home, "release").takeIf { it.isFile }?.readText().orEmpty()
    if (!releaseText.contains("IMPLEMENTOR=\"Eclipse Adoptium\"") &&
        !releaseText.contains("IMPLEMENTOR=\"Temurin\"")) {
        throw GradleException(
            "Linux 发布包必须使用 Temurin JDK 25 构建，避免把不可再分发的 JDK 打进安装包。" +
                " 当前 JDK：${home}。请设置 -PcamlJdkHome=<Temurin 25 路径> 或 JAVA_HOME。"
        )
    }

    return home
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

val cleanLinuxPackages = tasks.register<Delete>("cleanLinuxPackages") {
    group = "caml"
    description = "删除 Linux 打包中间文件和产物。"
    delete(linuxPackageDirectory)
}

val stageLinuxInput = tasks.register<Sync>("stageLinuxInput") {
    group = "caml"
    description = "收集 CAMLFront 运行时 jar，作为 jpackage 的输入。"
    dependsOn(tasks.named("installDist"))
    onlyIf { isLinuxHost() }
    into(linuxInputDirectory)
    doFirst {
        project.delete(linuxInputDirectory)
    }
    from(layout.buildDirectory.dir("install/CAMLFront/lib")) {
        include("*.jar")
    }
}

val packageLinuxAppImage = tasks.register<Exec>("packageLinuxAppImage") {
    group = "caml"
    description = "生成包含 JDK 25 和 JavaFX 的 Linux app-image。"
    dependsOn(stageLinuxInput)
    onlyIf { isLinuxHost() }

    doFirst {
        val version = packageVersion.get()
        validatePackageVersion(version)
        validateLinuxArchitecture()
        val jdkHome = resolvePackageJdkHome()
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
            "--main-jar", "CAMLFront-${project.version}.jar",
            "--main-class", "org.chs.camlauncher.front.CAMLApplication",
            "--java-options", "--module-path=\$APPDIR",
            "--java-options", "--add-modules=javafx.controls,javafx.fxml",
            "--java-options", "--enable-native-access=ALL-UNNAMED,javafx.graphics",
            "--add-modules",
            "java.base,java.desktop,java.logging,java.management,java.naming," +
                "java.net.http,java.prefs,java.scripting,java.sql,java.xml," +
                "jdk.management,jdk.unsupported,jdk.jfr",
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files",
            "--verbose"
        )
    }
}

val stageLinuxRoot = tasks.register<Sync>("stageLinuxRoot") {
    group = "caml"
    description = "生成 deb 和 rpm 共用的 Linux 根文件系统。"
    dependsOn(packageLinuxAppImage)
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
    from(rootProject.file("LICENSE")) {
        into("usr/share/doc/caml-launcher/licenses")
        rename { "CAML-GPL-3.0-only.txt" }
    }
    from(File(packagingDirectory, "licenses")) {
        into("usr/share/doc/caml-launcher/licenses")
    }
    from(File(linuxAppImage, "lib/runtime/legal")) {
        into("usr/share/doc/caml-launcher/licenses/JDK")
    }
    from(File(linuxAppImage, "lib/runtime/legal")) {
        into("usr/share/licenses/caml-launcher/JDK")
    }
    from(rootProject.file("LICENSE")) {
        into("usr/share/licenses/caml-launcher")
        rename { "CAML-GPL-3.0-only.txt" }
    }
    from(File(packagingDirectory, "licenses")) {
        into("usr/share/licenses/caml-launcher")
    }
}

val stageDebRoot = tasks.register<Sync>("stageDebRoot") {
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

val packageDeb = tasks.register<Exec>("packageDeb") {
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

val packageRpm = tasks.register<Exec>("packageRpm") {
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

val packageLinux = tasks.register("packageLinux") {
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
