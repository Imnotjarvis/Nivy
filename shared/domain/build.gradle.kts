plugins {
    id("nivy.feature")
    id("nivy.integration.testing")
    id("nivy.room")
}

android {
    namespace = "com.nivy.domain"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)

    implementation(libs.datastore)
    implementation(libs.bundles.opencsv)

    testImplementation(projects.shared.data.coreTesting)
    testImplementation(projects.shared.data.modelTesting)

    androidTestImplementation(libs.bundles.integration.testing)
    // The exchange-rate test reads a recorded response through Ktor's mock engine, not the network
    androidTestImplementation(libs.bundles.ktor)
    androidTestImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.mockk.android)
}
