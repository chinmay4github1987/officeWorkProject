import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** SDK levels and Java/Kotlin targets, defined ONCE for every Android module. */
object CatalogueSdk {
    const val COMPILE = 35
    const val TARGET = 35
    const val MIN = 26
}

internal fun Project.configureKotlinAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        compileSdk = CatalogueSdk.COMPILE
        defaultConfig {
            minSdk = CatalogueSdk.MIN
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
}
