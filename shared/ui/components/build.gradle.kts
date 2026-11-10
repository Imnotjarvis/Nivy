plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.ui.components"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)

    implementation(libs.bundles.activity)
    implementation(libs.androidx.work)
    implementation(libs.keval)
    implementation(libs.androidx.recyclerview)

    testImplementation(projects.shared.ui.testing)
}
