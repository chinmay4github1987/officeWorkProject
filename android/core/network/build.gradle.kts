// The only module that knows the backend's URL and JSON shapes, and the SMTP server the app can send email through.
plugins {
    alias(libs.plugins.catalogue.android.library)
    alias(libs.plugins.catalogue.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Secrets for this machine live in android/.env (KEY=value lines). It is never committed: see .env.example.
 * Values are looked up as: -Pgradle.property, then an environment variable (e.g. a CI secret), then .env.
 */
val dotEnv: Map<String, String> = rootProject.file(".env").takeIf { it.isFile }?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
    ?.associate { line -> line.substringBefore('=').trim() to line.substringAfter('=').trim().removeSurrounding("\"") }
    .orEmpty()

fun setting(gradleProperty: String, envName: String, default: String = ""): String =
    providers.gradleProperty(gradleProperty).orNull
        ?: providers.environmentVariable(envName).orNull
        ?: dotEnv[envName]
        ?: default

/** A setting as a Java string literal for BuildConfig (quotes and backslashes escaped). */
fun stringSetting(gradleProperty: String, envName: String, default: String = ""): String =
    "\"" + setting(gradleProperty, envName, default).replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.chinmay.catalogue.core.network"
    buildFeatures { buildConfig = true }
    defaultConfig {
        // Set in gradle.properties or with -Pcatalogue.apiBaseUrl=... so no URL is hard-coded in source.
        val baseUrl = providers.gradleProperty("catalogue.apiBaseUrl").getOrElse("http://10.0.2.2:3000/")
        buildConfigField("String", "API_BASE_URL", "\"$baseUrl\"")

        // Product enquiries by email straight from the app (JavaMail). Off while SMTP_HOST is empty: enquiries then
        // go through the backend. Secrets go in android/.env (copy .env.example), never in a committed file.
        val port = setting("catalogue.smtp.port", "SMTP_PORT", "587")
        require(port.toIntOrNull() != null) { "SMTP_PORT must be a number, got '$port'" }
        buildConfigField("String", "SMTP_HOST", stringSetting("catalogue.smtp.host", "SMTP_HOST"))
        buildConfigField("int", "SMTP_PORT", port)
        buildConfigField("String", "SMTP_SECURITY", stringSetting("catalogue.smtp.security", "SMTP_SECURITY", "starttls"))
        buildConfigField("String", "SMTP_USERNAME", stringSetting("catalogue.smtp.username", "SMTP_USERNAME"))
        buildConfigField("String", "SMTP_PASSWORD", stringSetting("catalogue.smtp.password", "SMTP_PASSWORD"))
        buildConfigField("String", "SMTP_FROM", stringSetting("catalogue.smtp.from", "SMTP_FROM"))
        buildConfigField("String", "SALES_INBOX", stringSetting("catalogue.salesInbox", "SALES_INBOX"))
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
    implementation(libs.android.mail)
    implementation(libs.android.activation)
}
