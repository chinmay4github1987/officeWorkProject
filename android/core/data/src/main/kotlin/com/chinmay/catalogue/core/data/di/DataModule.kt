package com.chinmay.catalogue.core.data.di

import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.data.repository.EnquirySender
import com.chinmay.catalogue.core.data.repository.FormspreeClient
import com.chinmay.catalogue.core.data.repository.SmtpEnquirySender
import com.chinmay.catalogue.core.data.repository.FakeStoreAuthRepository
import com.chinmay.catalogue.core.data.repository.FakeStoreCatalogueRepository
import com.chinmay.catalogue.core.data.repository.FormspreeEnquiryRepository
import com.chinmay.catalogue.core.data.repository.LocalCartRepository
import com.chinmay.catalogue.core.data.repository.RecentSearchesRepository
import com.chinmay.catalogue.core.data.repository.SharedPreferencesRecentSearchesRepository
import com.chinmay.catalogue.core.data.repository.SharedPreferencesThemeRepository
import com.chinmay.catalogue.core.data.repository.ThemeRepository
import com.chinmay.catalogue.core.data.session.CartLineStore
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.data.session.SharedPreferencesCartLineStore
import com.chinmay.catalogue.core.data.session.SharedPreferencesSessionStore
import com.chinmay.catalogue.core.network.mail.SmtpConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Binds each interface to its implementation. Data comes straight from https://fakestoreapi.com and enquiries go to
 * Formspree; there is no backend. Swapping to an offline-first catalogue (Room cache) would be a one-line change here.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCatalogueRepository(impl: FakeStoreCatalogueRepository): CatalogueRepository

    @Binds
    @Singleton
    abstract fun bindEnquiryRepository(impl: FormspreeEnquiryRepository): EnquiryRepository

    @Binds
    @Singleton
    abstract fun bindSessionStore(impl: SharedPreferencesSessionStore): SessionStore

    @Binds
    @Singleton
    abstract fun bindCartLineStore(impl: SharedPreferencesCartLineStore): CartLineStore

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeStoreAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: LocalCartRepository): CartRepository

    @Binds
    abstract fun bindThemeRepository(impl: SharedPreferencesThemeRepository): ThemeRepository

    @Binds
    abstract fun bindRecentSearchesRepository(impl: SharedPreferencesRecentSearchesRepository): RecentSearchesRepository
}

/**
 * How enquiries reach sales: the demo mailbox over SMTP when its settings are in android/.env (client demos),
 * otherwise Formspree (no password in the app; what CI builds and what you should ship).
 */
@Module
@InstallIn(SingletonComponent::class)
internal object EnquirySenderModule {
    @Provides
    @Singleton
    fun provideEnquirySender(
        smtp: SmtpConfig,
        byEmail: Provider<SmtpEnquirySender>,
        byFormspree: Provider<FormspreeClient>,
    ): EnquirySender = if (smtp.isConfigured) byEmail.get() else byFormspree.get()
}
