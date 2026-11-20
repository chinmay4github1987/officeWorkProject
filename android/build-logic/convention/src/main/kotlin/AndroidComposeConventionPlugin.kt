import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Turns on Jetpack Compose for an application OR library module. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.getByType(CommonExtension::class.java).apply {
                buildFeatures.compose = true
            }
            dependencies {
                val bom = libs.lib("androidx-compose-bom")
                add("implementation", platform(bom))
                add("implementation", libs.lib("androidx-compose-ui"))
                add("implementation", libs.lib("androidx-compose-material3"))
                add("implementation", libs.lib("androidx-compose-material-icons-core"))
                add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
            }
        }
    }
}
