plugins {
    id("nivy.feature")
    id("nivy.room")
    id("nivy.integration.testing")
}

android {
    namespace = "com.nivy.data"
}

dependencies {
    implementation(projects.shared.base)
    api(projects.shared.data.model)

    api(libs.datastore)
    implementation(libs.bundles.ktor)

    testImplementation(projects.shared.data.coreTesting)
    testImplementation(projects.shared.data.modelTesting)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.bundles.integration.testing)
    androidTestImplementation(projects.shared.data.coreTesting)
}
