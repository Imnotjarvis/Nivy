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

// The Android Gradle plugin depends on an older Bouncy Castle with known vulnerabilities; the
// plugins run with the catalog's version instead. Build tooling only, nothing ships in the app.
val bouncyCastleVersion = libs.versions.bouncycastle.get()
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle") {
            useVersion(bouncyCastleVersion)
            because("older Bouncy Castle versions have known vulnerabilities")
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
