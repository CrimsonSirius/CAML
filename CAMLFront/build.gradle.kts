plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

dependencies {
    implementation(project(":CAMLBackend"))
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
