plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.ui"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
}
