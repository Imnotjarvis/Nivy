plugins {
    id("nivy.feature")
    id("nivy.integration.testing")
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
    // The instrumented tests open a real database; the module's own code goes through the repositories.
    androidTestImplementation(libs.bundles.room)
    // The exchange-rate test reads a recorded response through Ktor's mock engine, not the network
    androidTestImplementation(libs.bundles.ktor)
    androidTestImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.mockk.android)
}
