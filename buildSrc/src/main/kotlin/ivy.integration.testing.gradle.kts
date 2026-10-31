plugins {
    id("ivy.feature")
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
}
