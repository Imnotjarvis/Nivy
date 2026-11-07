plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.settings"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)
    implementation(projects.shared.ui.components)
    implementation(projects.widget.balance)

    testImplementation(projects.shared.ui.testing)
}
