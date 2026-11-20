import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * One plugin = everything a feature module needs: Android library + Compose + Hilt + type-safe navigation,
 * and the core modules a screen may use. Features never depend on other features; :app connects them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("catalogue.android.library")
            pluginManager.apply("catalogue.android.compose")
            pluginManager.apply("catalogue.android.hilt")
            // Navigation routes are @Serializable classes.
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

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
            }
        }
    }
}
