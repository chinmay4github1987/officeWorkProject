package com.chinmay.catalogue.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the API client. Used by Hilt in the app AND by tests (against MockWebServer),
 * so tests exercise exactly the same JSON settings as production.
 */
object NetworkFactory {
    val json = Json {
        // New fields from the backend must not crash older app versions.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun catalogueApi(baseUrl: String, client: OkHttpClient, json: Json = this.json): CatalogueApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CatalogueApi::class.java)
}
