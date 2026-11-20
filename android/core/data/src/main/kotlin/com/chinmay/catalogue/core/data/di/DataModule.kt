package com.chinmay.catalogue.core.data.di

import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.data.repository.NetworkCatalogueRepository
import com.chinmay.catalogue.core.data.repository.NetworkEnquiryRepository
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
}
