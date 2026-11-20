package com.chinmay.catalogue.core.network.di

import com.chinmay.catalogue.core.network.BuildConfig
import com.chinmay.catalogue.core.network.CatalogueApi
import com.chinmay.catalogue.core.network.AuthInterceptor
import com.chinmay.catalogue.core.network.NetworkFactory
import com.chinmay.catalogue.core.network.SessionTokenProvider
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
    fun provideOkHttpClient(tokens: SessionTokenProvider): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        // Adds the session token while signed in (the BASIC debug log below never prints headers).
        .addInterceptor(AuthInterceptor(tokens))
        .apply {
            // Request logging in debug builds only; release builds never log traffic (it contains customer details).
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideCatalogueApi(client: OkHttpClient, json: Json): CatalogueApi =
        NetworkFactory.catalogueApi(BuildConfig.API_BASE_URL, client, json)
}
