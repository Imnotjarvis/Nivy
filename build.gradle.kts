// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Run with:
    // ./gradlew detekt // Simple report in the console
    // ./gradlew detektFormat // To check with enabled auto-correction
    id("ivy.detekt")
    id("com.jraska.module.graph.assertion")
}

// Each compiler warning carries its diagnostic name, e.g. [DEPRECATION], so CI can group them.
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    }
}

// Module dependency rules, checked in CI by ./gradlew :assertModuleGraph.
// The rules are regexes over "consumer -X> dependency" (api and implementation only).
moduleGraphAssert {
    // app -> feature -> widget -> legacy -> navigation -> ui -> domain -> data -> base
    maxHeight = 8
    restricted = arrayOf(
        // Lower layers never depend on higher ones.
        ":shared:.* -X> :(app|feature|widget|temp):.*",
        ":widget:.* -X> :(app|feature):.*",
        ":temp:.* -X> :(app|feature|widget):.*",
        ".* -X> :app",
        // Features don't depend on each other; main and import-data keep their existing links.
        ":feature:(?!(?:main|import-data) )[a-z-]+ -X> :feature:.*",
        ":feature:main -X> :feature:(?!(?:accounts|home)$).*",
        ":feature:import-data -X> :feature:(?!onboarding$).*",
        // No new users of the temp modules: only the features and widget that use them today.
        ":feature:(?!(?:accounts|balance|budgets|categories|edit-transaction|exchange-rates|home|" +
            "import-data|loans|main|onboarding|piechart|planned-payments|reports|search|settings|" +
            "transactions) )[a-z-]+ -X> :temp:.*",
        ":widget:(?!balance )[a-z-]+ -X> :temp:.*",
    )
}
