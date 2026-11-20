// Test helpers shared by every feature's unit tests. Only ever a testImplementation dependency.
plugins {
    alias(libs.plugins.catalogue.android.library)
}

android {
    namespace = "com.chinmay.catalogue.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
