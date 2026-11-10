plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

dependencies {
    implementation(project(":CAMLBackend"))
    implementation(project(":CAMLUI"))
    implementation(libs.gson)
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

// Generates the lang JSON files from LanguageTranslationGenerator subclasses.
// Run with: gradlew :CAMLFront:generateTranslation
tasks.register<JavaExec>("generateTranslation") {
    group = "caml"
    description = "Generate language JSON files from LanguageTranslationGenerator subclasses."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.chs.camlauncher.front.translation.TranslationGeneratorRunner")
    args(layout.projectDirectory.dir("src/main/resources/lang").asFile.absolutePath)
}