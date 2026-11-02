plugins {
    java
}

dependencies {
    implementation(libs.gson)
    implementation(libs.log4j.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
