// The only module that knows every feature. It wires navigation and owns the Hilt application graph.
plugins {
    alias(libs.plugins.catalogue.android.application)
    alias(libs.plugins.catalogue.android.compose)
    alias(libs.plugins.catalogue.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chinmay.catalogue"

    defaultConfig {
        applicationId = "com.chinmay.catalogue"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // PoC: release is signed with the debug key so it can be installed for demos. Use a real key before shipping.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":feature:home"))
    implementation(project(":feature:category"))
    implementation(project(":feature:search"))
    implementation(project(":feature:product"))
    implementation(project(":feature:enquiry"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
