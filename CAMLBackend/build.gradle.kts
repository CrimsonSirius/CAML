plugins {
    java
}

dependencies {
    implementation(project(":CAMLLanguage"))
    implementation(libs.gson)
    implementation(libs.log4j.api)
    implementation(libs.twelvemonkeys.imageio.webp)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
