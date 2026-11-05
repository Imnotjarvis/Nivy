plugins {
    id("ivy.feature")
}

android {
    namespace = "com.ivy.data.core.testing"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
}
