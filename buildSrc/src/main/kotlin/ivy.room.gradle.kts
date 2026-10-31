plugins {
    id("ivy.module")
    id("androidx.room")
}

dependencies {
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    androidTestImplementation(libs.room.testing)
}

// Since Room 2.7 the plugin also adds the exported schemas to the instrumented tests' assets,
// where MigrationTestHelper reads them.
room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    // Keep generating Java like Room 2.6 did; Room 2.7+ defaults to Kotlin output.
    arg("room.generateKotlin", "false")
}
