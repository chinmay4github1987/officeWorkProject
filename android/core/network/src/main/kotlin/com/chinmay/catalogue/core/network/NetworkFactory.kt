package com.chinmay.catalogue.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the Fake Store client. Used by Hilt in the app AND by tests (against MockWebServer),
 * so tests exercise exactly the same JSON settings as production.
 */
object NetworkFactory {
    val json = Json {
        // Fields we don't use (or that Fake Store adds later) are skipped, never a crash.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    const val FORMSPREE_BASE_URL = "https://formspree.io/"

    fun fakeStoreApi(baseUrl: String, client: OkHttpClient, json: Json = this.json): FakeStoreApi =
        retrofit(baseUrl, client, json).create(FakeStoreApi::class.java)

    fun formspreeApi(baseUrl: String, client: OkHttpClient, json: Json = this.json): FormspreeApi =
        retrofit(baseUrl, client, json).create(FormspreeApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
