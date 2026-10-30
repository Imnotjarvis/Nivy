plugins {
    id("ivy.feature")
}

android {
    namespace = "com.ivy.data.model"
}

dependencies {
    api(projects.shared.base)
}
