package com.chinmay.catalogue.core.testing

import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.model.Product

/**
 * Hand-written fakes instead of mocks: they behave like the real thing, record what they were asked,
 * and tests read like plain Kotlin. Set [failWith] to simulate an error.
 */
class FakeCatalogueRepository : CatalogueRepository {
    var failWith: DataError? = null
    var home: CatalogueHome = TestData.home
    var products: List<Product> = TestData.products
    val searchTerms = mutableListOf<String>()
    var calls = 0
        private set

    private fun <T> respond(value: () -> T): DataResult<T> {
        calls++
        return failWith?.let { DataResult.Failure(it) } ?: DataResult.Success(value())
    }

    override suspend fun home() = respond { home }

    override suspend fun category(slug: String): DataResult<CategoryPage> {
        if (slug != TestData.electronics.slug && failWith == null) return DataResult.Failure(DataError.NotFound)
        return respond { TestData.electronicsPage }
    }

    override suspend fun search(term: String): DataResult<List<Product>> {
        searchTerms += term
        return respond { products.filter { it.title.contains(term.trim(), ignoreCase = true) } }
    }

    override suspend fun product(id: String): DataResult<Product> {
        val found = products.firstOrNull { it.id == id }
        if (found == null && failWith == null) return DataResult.Failure(DataError.NotFound)
        return respond { found!! }
    }
}

class FakeEnquiryRepository : EnquiryRepository {
    var failWith: DataError? = null
    val submitted = mutableListOf<EnquiryRequest>()

    override suspend fun submit(request: EnquiryRequest): DataResult<EnquiryReceipt> {
        submitted += request
        return failWith?.let { DataResult.Failure(it) }
            ?: DataResult.Success(EnquiryReceipt(reference = "ENQ-TEST0001", receivedAt = "2026-11-20T10:00:00.000Z"))
    }
}
