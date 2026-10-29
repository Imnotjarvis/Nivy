enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

buildCache {
    local {
        // CI keeps the build cache outside Gradle's home so it can be saved per commit.
        System.getenv("GRADLE_BUILD_CACHE_DIR")?.let { directory = File(it) }
    }
}

rootProject.name = "IvyWallet"
include(":app")
include(":feature:accounts")
include(":feature:balance")
include(":feature:budgets")
include(":feature:categories")
include(":feature:edit-transaction")
include(":feature:exchange-rates")
include(":feature:features")
include(":feature:home")
include(":feature:import-data")
include(":feature:loans")
include(":feature:main")
include(":feature:onboarding")
include(":feature:piechart")
include(":feature:planned-payments")
include(":feature:reports")
include(":feature:search")
include(":feature:settings")
include(":feature:transactions")
include(":shared:base")
include(":shared:data:core")
include(":shared:data:model")
include(":shared:data:model-testing")
include(":shared:domain")
include(":shared:ui:core")
include(":shared:ui:navigation")
include(":shared:ui:testing")
include(":temp:legacy-code")
include(":temp:old-design")
include(":widget:add-transaction")
include(":widget:balance")
include(":widget:shared-base")