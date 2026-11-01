plugins {
    id("ivy.kotlin-android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.bundles.hilt)
    ksp(catalog.library("hilt-compiler"))
    // Generates the factories @HiltWorker classes need; without it WorkManager can't create them.
    ksp(catalog.library("hilt-work-compiler"))
}
