import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File
import java.security.MessageDigest

plugins {
    base
}

allprojects {
    group = "org.chs.camlauncher"
    version = "1.0-SNAPSHOT"

    repositories {
        // 阿里云镜像（推荐，速度极快）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 保留原有的官方源作为备用
        mavenCentral()
        google()
    }
}

subprojects {
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(25)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/**
 * 把发布包汇总到根工程的 `build/packages`。
 *
 * 打包任务本身仍把产物写在自己的模块 `build` 目录（`CAMLFront/build/packages/...`）：
 * Gradle 的 `clean`、增量判断和 `--parallel` 都假定任务输出属于自己所在的工程。
 * 这里只做收集，让取包有一个统一入口，不改变各任务的输出位置。
 */
val collectPackages = tasks.register<Sync>("collectPackages") {
    group = "caml"
    description = "汇总 Linux / Windows 发布包到根目录 build/packages，并生成统一的 SHA256SUMS.txt。"

    // 两个任务都带平台判断，非本机平台会被跳过，因此可以无条件依赖。
    // EXE 需要 WiX，所以默认只保证「Linux 包 / Windows 免安装包」一定产出；
    // 需要 exe 时先单独运行 :CAMLFront:packageWindows，再运行本任务即可一起收集。
    dependsOn(":CAMLFront:packageLinux", ":CAMLFront:packageWindowsPortable")

    val frontLayout = project(":CAMLFront").layout
    val rootPackages = layout.buildDirectory.dir("packages")

    into(rootPackages)
    from(frontLayout.buildDirectory.dir("packages/linux")) {
        include("*.deb", "*.rpm")
    }
    from(frontLayout.buildDirectory.dir("packages/windows")) {
        include("*.exe", "*.zip")
    }

    doLast {
        val destination = rootPackages.get().asFile
        val artifacts = destination.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name != "SHA256SUMS.txt" }
            .sortedBy { it.name }
        if (artifacts.isEmpty()) {
            throw GradleException(
                "没有可汇总的发布包。请先运行 :CAMLFront:packageLinux 或 :CAMLFront:packageWindowsPortable。"
            )
        }
        File(destination, "SHA256SUMS.txt").writeText(
            artifacts.joinToString("\n") { "${sha256(it)}  ${it.name}" } + "\n"
        )
        logger.lifecycle("汇总 ${artifacts.size} 个发布包到 ${destination.absolutePath}")
    }
}