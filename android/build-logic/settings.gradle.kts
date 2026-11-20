dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    // Reuse the app's version catalog so plugin versions are defined in exactly one place.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
