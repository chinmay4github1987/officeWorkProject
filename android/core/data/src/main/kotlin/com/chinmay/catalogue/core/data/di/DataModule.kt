package com.chinmay.catalogue.core.data.di

import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.data.repository.NetworkAuthRepository
import com.chinmay.catalogue.core.data.repository.NetworkCartRepository
import com.chinmay.catalogue.core.data.repository.NetworkCatalogueRepository
import com.chinmay.catalogue.core.data.repository.NetworkEnquiryRepository
import com.chinmay.catalogue.core.data.repository.SmtpEnquiryRepository
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.data.session.SharedPreferencesSessionStore
import com.chinmay.catalogue.core.network.SessionTokenProvider
import com.chinmay.catalogue.core.network.mail.SmtpConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Binds each interface to its implementation. Swapping to an offline-first repository is a one-line change here. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCatalogueRepository(impl: NetworkCatalogueRepository): CatalogueRepository

    @Binds
    @Singleton
    abstract fun bindSessionStore(impl: SharedPreferencesSessionStore): SessionStore

    /** :core:network reads the token through this, without knowing where sessions are kept. */
    @Binds
    abstract fun bindSessionTokenProvider(impl: SessionStore): SessionTokenProvider

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: NetworkAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: NetworkCartRepository): CartRepository
}

/**
 * Product enquiries go by email straight from the app (JavaMail) when the build has SMTP settings,
 * otherwise through the backend's /api/v1/enquiries. Cart enquiries always use the backend: they need the session.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object EnquiryModule {
    @Provides
    @Singleton
    fun provideEnquiryRepository(
        smtp: SmtpConfig,
        byEmail: Provider<SmtpEnquiryRepository>,
        byBackend: Provider<NetworkEnquiryRepository>,
    ): EnquiryRepository = if (smtp.isConfigured) byEmail.get() else byBackend.get()
}
