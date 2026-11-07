plugins {
    org.jetbrains.kotlin.plugin.compose
    id("nivy.module")
}

android {
    // Compose
    buildFeatures {
        compose = true
    }

    lint {
        disable += "MissingTranslation"
        abortOnError = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.tooling)

    lintChecks(libs.slack.lint.compose)
}
