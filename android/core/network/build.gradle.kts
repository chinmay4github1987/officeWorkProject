// The only module that knows the backend's URL and JSON shapes.
plugins {
    alias(libs.plugins.catalogue.android.library)
    alias(libs.plugins.catalogue.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chinmay.catalogue.core.network"
    buildFeatures { buildConfig = true }
    defaultConfig {
        // Set in gradle.properties or with -Pcatalogue.apiBaseUrl=... so no URL is hard-coded in source.
        val baseUrl = providers.gradleProperty("catalogue.apiBaseUrl").getOrElse("http://10.0.2.2:3000/")
        buildConfigField("String", "API_BASE_URL", "\"$baseUrl\"")
    }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}
