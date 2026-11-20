package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.network.CatalogueApi
import com.chinmay.catalogue.core.network.dto.asExternalModel
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Online-only for the PoC: every call goes to the backend, which caches product data. */
internal class NetworkCatalogueRepository @Inject constructor(
    private val api: CatalogueApi,
    private val json: Json,
) : CatalogueRepository {

    override suspend fun home(): DataResult<CatalogueHome> =
        safeApiCall(json) { api.home().asExternalModel() }

    override suspend fun category(slug: String): DataResult<CategoryPage> =
        safeApiCall(json) { api.category(slug).asExternalModel() }

    override suspend fun search(term: String): DataResult<List<Product>> =
        safeApiCall(json) { api.search(term.trim()).products.map { it.asExternalModel() } }

    override suspend fun product(id: String): DataResult<Product> =
        safeApiCall(json) { api.product(id).asExternalModel() }
}
