plugins {
    id("ivy.kotlin-android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.bundles.hilt)
    ksp(catalog.library("hilt-compiler"))
}
