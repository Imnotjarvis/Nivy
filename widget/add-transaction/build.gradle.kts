plugins {
    id("nivy.widget")
}

android {
    namespace = "com.nivy.widget.transaction"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)
}
