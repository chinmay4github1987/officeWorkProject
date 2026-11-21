import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: a library with Compose, Hilt and navigation, depending only on core modules.
 * Its unit tests can also render its screens (Compose UI tests on Robolectric), so UI tests need no emulator.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("catalogue.android.library")
            pluginManager.apply("catalogue.android.compose")
            pluginManager.apply("catalogue.android.hilt")
            // Navigation routes are @Serializable classes.
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            // Robolectric UI tests read the module's real strings, themes and fonts.
            extensions.getByType(LibraryExtension::class.java).testOptions.unitTests.isIncludeAndroidResources = true

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))

                add("implementation", libs.lib("androidx-activity-compose"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-navigation-compose"))
                add("implementation", libs.lib("hilt-navigation-compose"))
                add("implementation", libs.lib("kotlinx-serialization-json"))

                add("testImplementation", project(":core:testing"))
                // Compose UI tests on the JVM (Robolectric).
                add("testImplementation", platform(libs.lib("androidx-compose-bom")))
                add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
                add("testImplementation", libs.lib("robolectric"))
                // The empty activity createComposeRule() launches.
                add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
