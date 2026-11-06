plugins {
    id("ivy.feature")
    id("ivy.integration.testing")
    id("ivy.room")
}

android {
    namespace = "com.ivy.domain"
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
