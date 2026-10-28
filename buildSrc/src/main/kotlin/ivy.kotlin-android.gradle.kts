plugins {
    // AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the kotlin-android plugin is gone.
    id("com.android.library")
}

android {
    // Kotlin's jvmTarget follows targetCompatibility under built-in Kotlin.
    val javaVersion = catalog.version("jvm-target")
    compileOptions {
        sourceCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
        targetCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
    }

    // Android
    compileSdk = catalog.version("compile-sdk").toInt()
    defaultConfig {
        minSdk = catalog.version("min-sdk").toInt()
    }
}

gradle.projectsEvaluated {
    // Increase tests Heap Size because of Kotest property-based tests
    tasks.withType<Test> {
        maxHeapSize = "2048m"
    }
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.kotlin)
    implementation(catalog.bundle("kotlin-android"))
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}
