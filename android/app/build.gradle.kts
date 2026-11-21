// The only module that knows every feature. It wires navigation and owns the Hilt application graph.
plugins {
    alias(libs.plugins.catalogue.android.application)
    alias(libs.plugins.catalogue.android.compose)
    alias(libs.plugins.catalogue.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chinmay.catalogue"

    testOptions { unitTests.isIncludeAndroidResources = true }

    packaging {
        resources {
            // JavaMail (android-mail, android-activation) and jakarta.inject each ship these files under the same path.
            excludes += setOf("META-INF/NOTICE.md", "META-INF/LICENSE.md")
        }
    }

    defaultConfig {
        applicationId = "com.chinmay.catalogue"
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "android"
            keyAlias = "releaseKey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    implementation(project(":feature:auth"))
    implementation(project(":feature:cart"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)

    // Drawer and shell tests: Compose UI on the JVM (Robolectric) plus the hand-written fakes.
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
