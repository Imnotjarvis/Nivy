plugins {
    id("nivy.feature")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        // Every native library comes prebuilt from a dependency; this project has no native code.
        // Stripping them needs the NDK, which nothing else here uses, so the build packaged them
        // unchanged and warned about it. Packaging them as they are is intended.
        jniLibs.keepDebugSymbols += "**/*.so"
        resources.pickFirsts.apply {
            add("win32-x86-64/attach_hotspot_windows.dll")
            add("win32-x86/attach_hotspot_windows.dll")
            add("META-INF/**")
            add("xsd/catalog.xml")
        }
    }
}

dependencies {
    androidTestImplementation(libs.bundles.integration.testing)
    // hilt-work asks for WorkManager 2.3.4, which crashes as it starts on Android 12 and newer (a
    // PendingIntent without FLAG_IMMUTABLE). The app gets a current WorkManager, but a module's
    // test APK got that one, and its process crashed before the first test: none ever ran.
    androidTestImplementation(libs.androidx.work)
}
