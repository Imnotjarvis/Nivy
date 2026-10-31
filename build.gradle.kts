// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Run with:
    // ./gradlew detekt // Simple report in the console
    // ./gradlew detektFormat // To check with enabled auto-correction
    id("ivy.detekt")
    id("com.jraska.module.graph.assertion")
}

// Kotlin warnings fail the build. Each one carries its diagnostic name, e.g. [DEPRECATION], so CI
// can group them.
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
        compilerOptions.freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    }
}

// The build tools (lint, the SDK libraries, test runners) ask for older Bouncy Castle and Guava
// versions with known vulnerabilities; every project resolves the catalog's versions instead (Guava
// in the flavour asked for, -jre or -android). buildSrc does the same for the plugins.
val bouncyCastleVersion = libs.versions.bouncycastle.get()
val guavaVersion = libs.versions.guava.get().substringBeforeLast('-')
allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.bouncycastle") {
                useVersion(bouncyCastleVersion)
                because("older Bouncy Castle versions have known vulnerabilities")
            } else if (requested.group == "com.google.guava" && requested.name == "guava") {
                useVersion("$guavaVersion-${requested.version.orEmpty().substringAfterLast('-', "jre")}")
                because("older Guava versions have known vulnerabilities")
            }
        }
    }
}

// Module dependency rules, checked in CI by ./gradlew :assertModuleGraph.
// The rules are regexes over "consumer -X> dependency" (api and implementation only).
moduleGraphAssert {
    // app -> feature -> widget -> ui:components -> navigation -> ui:core -> domain -> data
    //     -> data:model -> base
    maxHeight = 9
    restricted = arrayOf(
        // Lower layers never depend on higher ones.
        ":shared:.* -X> :(app|feature|widget):.*",
        ":shared:(?!ui:components)[a-z:-]+ -X> :shared:ui:components",
        ":widget:.* -X> :(app|feature):.*",
        ".* -X> :app",
        // Features don't depend on each other; main and import-data keep their existing links.
        ":feature:(?!(?:main|import-data) )[a-z-]+ -X> :feature:.*",
        ":feature:main -X> :feature:(?!(?:accounts|home)$).*",
        ":feature:import-data -X> :feature:(?!onboarding$).*",
    )
}
