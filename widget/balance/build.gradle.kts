plugins {
    id("nivy.widget")
    id("nivy.paparazzi")
}

android {
    namespace = "com.nivy.widget.balance"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)

    implementation(projects.shared.ui.components)
    implementation(libs.androidx.work)

    testImplementation(projects.shared.ui.testing)
}
