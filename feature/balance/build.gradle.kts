plugins {
    id("ivy.feature")
}

android {
    namespace = "com.ivy.balance"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)
    implementation(projects.shared.ui.components)

    testImplementation(projects.shared.ui.testing)
}
