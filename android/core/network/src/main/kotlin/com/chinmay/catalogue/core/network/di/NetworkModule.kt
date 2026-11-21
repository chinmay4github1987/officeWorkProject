package com.chinmay.catalogue.core.network.di

import com.chinmay.catalogue.core.network.BuildConfig
import com.chinmay.catalogue.core.network.FakeStoreApi
import com.chinmay.catalogue.core.network.FormspreeApi
import com.chinmay.catalogue.core.network.FormspreeConfig
import com.chinmay.catalogue.core.network.NetworkFactory
import com.chinmay.catalogue.core.network.dto.Pricing
import com.chinmay.catalogue.core.network.mail.SmtpConfig
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = NetworkFactory.json

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .apply {
            // Request logging in debug builds only; release builds never log traffic (it contains customer details).
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideFakeStoreApi(client: OkHttpClient, json: Json): FakeStoreApi =
        NetworkFactory.fakeStoreApi(BuildConfig.FAKESTORE_BASE_URL, client, json)

    @Provides
    @Singleton
    fun provideFormspreeApi(client: OkHttpClient, json: Json): FormspreeApi =
        NetworkFactory.formspreeApi(NetworkFactory.FORMSPREE_BASE_URL, client, json)

    /** The optional demo mailbox (Gmail by default). Unused unless SMTP_USERNAME and SMTP_PASSWORD are set in .env. */
    @Provides
    @Singleton
    fun provideSmtpConfig(): SmtpConfig = SmtpConfig(
        host = BuildConfig.SMTP_HOST,
        port = BuildConfig.SMTP_PORT,
        security = SmtpConfig.security(BuildConfig.SMTP_SECURITY),
        username = BuildConfig.SMTP_USERNAME,
        password = BuildConfig.SMTP_PASSWORD,
        salesInbox = BuildConfig.SALES_INBOX,
    )

    @Provides
    @Singleton
    fun provideSmtpMailer(config: SmtpConfig): SmtpMailer = SmtpMailer(config)

    /** How Fake Store's dollar prices are shown: rupees at a fixed rate by default (PRICE_CURRENCY, PRICE_PER_USD). */
    @Provides
    @Singleton
    fun providePricing(): Pricing = Pricing(currencyCode = BuildConfig.PRICE_CURRENCY, perUsd = BuildConfig.PRICE_PER_USD)

    /** Blank unless the build was given FORMSPREE_FORM_ID (android/.env or a CI secret, see .env.example). */
    @Provides
    @Singleton
    fun provideFormspreeConfig(): FormspreeConfig = FormspreeConfig(formId = BuildConfig.FORMSPREE_FORM_ID)
}
