package com.chinmay.catalogue.core.data.di

import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.data.repository.NetworkAuthRepository
import com.chinmay.catalogue.core.data.repository.NetworkCartRepository
import com.chinmay.catalogue.core.data.repository.NetworkCatalogueRepository
import com.chinmay.catalogue.core.data.repository.NetworkEnquiryRepository
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.data.session.SharedPreferencesSessionStore
import com.chinmay.catalogue.core.network.SessionTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    abstract fun bindEnquiryRepository(impl: NetworkEnquiryRepository): EnquiryRepository

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
