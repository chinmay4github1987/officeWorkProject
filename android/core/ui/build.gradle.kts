// Product-aware UI pieces shared by several features (tiles, cards, price formatting).
// Separate from :core:designsystem, which stays free of domain types.
plugins {
    alias(libs.plugins.catalogue.android.library)
    alias(libs.plugins.catalogue.android.compose)
}

android {
    namespace = "com.chinmay.catalogue.core.ui"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
