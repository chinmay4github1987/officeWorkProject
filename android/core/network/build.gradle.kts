// The only module that knows the URLs and JSON shapes the app talks to: Fake Store (data) and Formspree (enquiries).
plugins {
    alias(libs.plugins.catalogue.android.library)
    alias(libs.plugins.catalogue.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Settings for this machine can live in android/.env (KEY=value lines, never committed: see .env.example).
 * Values are looked up as: -Pgradle.property, then an environment variable (e.g. a CI secret), then .env.
 */
val dotEnv: Map<String, String> = rootProject.file(".env").takeIf { it.isFile }?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
    ?.associate { line -> line.substringBefore('=').trim() to line.substringAfter('=').trim().removeSurrounding("\"") }
    .orEmpty()

fun setting(gradleProperty: String, envName: String, default: String = ""): String =
    providers.gradleProperty(gradleProperty).orNull
        ?: providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        ?: dotEnv[envName]
        ?: default

android {
    namespace = "com.chinmay.catalogue.core.network"
    buildFeatures { buildConfig = true }
    defaultConfig {
        // Product, account and cart data come straight from Fake Store over HTTPS (no backend).
        // Override with -Pcatalogue.fakeStoreBaseUrl=... (e.g. a mirror); must be https and end with "/".
        val fakeStoreUrl = providers.gradleProperty("catalogue.fakeStoreBaseUrl").getOrElse("https://fakestoreapi.com/")
        require(fakeStoreUrl.startsWith("https://") && fakeStoreUrl.endsWith("/")) {
            "catalogue.fakeStoreBaseUrl must be an https:// URL ending with /, got '$fakeStoreUrl'"
        }
        buildConfigField("String", "FAKESTORE_BASE_URL", "\"$fakeStoreUrl\"")

        // Enquiries are posted to this Formspree form, which emails the sales inbox. The ID is public (it's in the
        // form's URL), not a secret; empty = enquiries show "not set up yet". Never put SMTP or API passwords here.
        val formId = setting("catalogue.formspreeFormId", "FORMSPREE_FORM_ID")
        require(formId.isEmpty() || formId.matches(Regex("[A-Za-z0-9]{4,40}"))) {
            "FORMSPREE_FORM_ID should look like 'xyzabcde' (from https://formspree.io/f/xyzabcde), got '$formId'"
        }
        buildConfigField("String", "FORMSPREE_FORM_ID", "\"$formId\"")

        // OPTIONAL demo email: with SMTP_USERNAME set (in android/.env only), enquiries are emailed straight from the
        // app through this mailbox (Gmail by default) instead of Formspree. The password is then INSIDE the APK, so use
        // a throwaway demo account, keep it out of git and CI, and never publish that APK.
        fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val smtpPort = setting("catalogue.smtp.port", "SMTP_PORT", "465")
        require(smtpPort.toIntOrNull() != null) { "SMTP_PORT must be a number, got '$smtpPort'" }
        buildConfigField("String", "SMTP_HOST", quoted(setting("catalogue.smtp.host", "SMTP_HOST", "smtp.gmail.com")))
        buildConfigField("int", "SMTP_PORT", smtpPort)
        buildConfigField("String", "SMTP_SECURITY", quoted(setting("catalogue.smtp.security", "SMTP_SECURITY", "ssl")))
        buildConfigField("String", "SMTP_USERNAME", quoted(setting("catalogue.smtp.username", "SMTP_USERNAME")))
        buildConfigField("String", "SMTP_PASSWORD", quoted(setting("catalogue.smtp.password", "SMTP_PASSWORD")))
        buildConfigField("String", "SALES_INBOX", quoted(setting("catalogue.salesInbox", "SALES_INBOX")))

        // Fake Store's prices are US dollars; the app shows them converted (rupees by default) at this fixed rate.
        val currency = setting("catalogue.priceCurrency", "PRICE_CURRENCY", "INR")
        val perUsd = setting("catalogue.pricePerUsd", "PRICE_PER_USD", "85.0")
        require(currency.matches(Regex("[A-Z]{3}"))) { "PRICE_CURRENCY must be an ISO code like INR, got '$currency'" }
        require((perUsd.toDoubleOrNull() ?: 0.0) > 0) { "PRICE_PER_USD must be a positive number, got '$perUsd'" }
        buildConfigField("String", "PRICE_CURRENCY", "\"$currency\"")
        buildConfigField("double", "PRICE_PER_USD", perUsd)
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
