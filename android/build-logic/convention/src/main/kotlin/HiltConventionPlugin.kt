import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Dependency injection with Hilt. KSP (not kapt) generates the code, which keeps builds fast. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies {
                add("implementation", libs.lib("hilt-android"))
                add("ksp", libs.lib("hilt-compiler"))
            }
        }
    }
}
