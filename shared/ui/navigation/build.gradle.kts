plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.navigation"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.model)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
}
