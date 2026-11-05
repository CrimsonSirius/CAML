plugins {
    `java-library`
    id("org.openjfx.javafxplugin") version "0.1.0"
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

javafx {
    version = libs.versions.javafx.get()
    modules("javafx.controls")
}
