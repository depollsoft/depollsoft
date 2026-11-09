plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:9.5.1")
    // buildSrc's classloader is the parent of every build script's, so the Kotlin Gradle plugin AGP
    // pulls in here would win over the root buildscript's. Keep this in step with kotlin_version.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
}
