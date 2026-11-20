import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Plain Kotlin modules with no Android dependency (core:model). They compile and test fastest. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                add("testImplementation", libs.lib("junit"))
            }
        }
    }
}
