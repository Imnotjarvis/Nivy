plugins {
    `kotlin-dsl`
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Like the app's modules (root build.gradle.kts): a compiler warning fails the build.
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// The Gradle plugins depend on older Bouncy Castle and Guava versions with known vulnerabilities;
// they run with the catalog's versions instead (Guava in the flavour asked for, -jre or -android).
val bouncyCastleVersion = libs.versions.bouncycastle.get()
val guavaVersion = libs.versions.guava.get().substringBeforeLast('-')
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle") {
            useVersion(bouncyCastleVersion)
            because("older Bouncy Castle versions have known vulnerabilities")
        } else if (requested.group == "com.google.guava" && requested.name == "guava") {
            useVersion("$guavaVersion-${requested.version.orEmpty().substringAfterLast('-', "jre")}")
            because("older Guava versions have known vulnerabilities")
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.plugin)
    implementation(libs.hilt.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kotlinx.serialization.plugin)
    implementation(libs.ksp.plugin)
    implementation(libs.room.plugin)
    implementation(libs.paparazzi.plugin)
    implementation(libs.module.graph.plugin)

    // Make version catalog available in precompiled scripts
    // https://github.com/gradle/gradle/issues/15383#issuecomment-1567461389
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
