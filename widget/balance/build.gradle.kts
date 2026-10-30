plugins {
    id("ivy.widget")
}

android {
    namespace = "com.ivy.widget.balance"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)

    implementation(projects.widget.sharedBase)
    implementation(projects.shared.ui.components)
}
