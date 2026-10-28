plugins {
    id("ivy.module")
    id("androidx.room")
}

dependencies {
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    androidTestImplementation(libs.room.testing)
}

android {
    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    // Keep generating Java like Room 2.6 did; Room 2.7+ defaults to Kotlin output.
    arg("room.generateKotlin", "false")
}
