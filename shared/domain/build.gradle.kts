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

    testImplementation(projects.shared.data.modelTesting)

    androidTestImplementation(libs.bundles.integration.testing)
    // The live exchange-rate test builds the real HTTP client from shared:data:core
    androidTestImplementation(libs.bundles.ktor)
    androidTestImplementation(libs.mockk.android)
}
