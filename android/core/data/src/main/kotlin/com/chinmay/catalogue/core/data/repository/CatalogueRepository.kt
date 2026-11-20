package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product

/**
 * What screens can ask for. An interface, so ViewModel tests use a fake and a future
 * offline-first implementation (Room cache + network) can replace the default one without touching features.
 */
interface CatalogueRepository {
    suspend fun home(): DataResult<CatalogueHome>
    suspend fun category(slug: String): DataResult<CategoryPage>
    suspend fun search(term: String): DataResult<List<Product>>
    suspend fun product(id: String): DataResult<Product>
}
