import org.gradle.api.tasks.WriteProperties

plugins {
    java
}

val curseForgeApiKey = providers.environmentVariable("CURSEFORGE_API_KEY").orElse("")

val generateBuildProperties = tasks.register<WriteProperties>("generateBuildProperties") {
    destinationFile.set(layout.buildDirectory.file("generated/resources/caml.properties"))
    encoding = "UTF-8"
    // 不把 Key 原值放进 task inputs，避免被构建扫描记录；任务本身很轻，每次重新生成。
    outputs.upToDateWhen { false }
    property("caml.curseforge.apikey", curseForgeApiKey.get())
}

sourceSets {
    main {
        resources.srcDir(layout.buildDirectory.dir("generated/resources"))
    }
}

tasks.processResources {
    dependsOn(generateBuildProperties)
}

dependencies {
    implementation(project(":CAMLLanguage"))
    implementation(libs.gson)
    implementation(libs.log4j.api)
    implementation(libs.twelvemonkeys.imageio.webp)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
