pluginManagement {
    // Convention plugins live in an included build, so module build files stay a few lines long.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProductCatalogue"

include(":app")

include(":core:model")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

include(":feature:home")
include(":feature:category")
include(":feature:search")
include(":feature:product")
include(":feature:enquiry")
