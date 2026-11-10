plugins {
    `java-library`
}

dependencies {
    implementation(libs.gson)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Generates the lang JSON files from LanguageTranslationGenerator subclasses.
// Run with: gradlew :CAMLLanguage:generateTranslation
tasks.register<JavaExec>("generateTranslation") {
    group = "caml"
    description = "Generate language JSON files from LanguageTranslationGenerator subclasses."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.chs.camlauncher.language.TranslationGeneratorRunner")
    args(layout.projectDirectory.dir("src/main/resources/lang").asFile.absolutePath)
}
