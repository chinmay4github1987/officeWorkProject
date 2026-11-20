// Repositories: the single entry point features use for data. They hide HTTP and return DataResult.
plugins {
    alias(libs.plugins.catalogue.android.library)
    alias(libs.plugins.catalogue.android.hilt)
}

android {
    namespace = "com.chinmay.catalogue.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:network"))
    implementation(libs.retrofit.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.core)
}
