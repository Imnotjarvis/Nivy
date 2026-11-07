plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.data.model.testing"
}

dependencies {
    implementation(projects.shared.data.model)

    implementation(libs.bundles.testing)
}
