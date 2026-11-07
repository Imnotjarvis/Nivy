plugins {
    id("nivy.feature")
}

android {
    namespace = "com.nivy.data.model"
}

dependencies {
    api(projects.shared.base)
}
