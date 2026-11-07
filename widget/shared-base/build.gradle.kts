plugins {
    id("nivy.widget")
}

android {
    namespace = "com.nivy.widget"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
}
