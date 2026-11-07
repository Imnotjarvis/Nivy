plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.data.core.testing"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
}
