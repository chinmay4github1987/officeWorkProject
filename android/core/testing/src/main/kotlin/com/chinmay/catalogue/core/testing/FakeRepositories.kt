package com.chinmay.catalogue.core.testing

import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.data.repository.RecentSearchesRepository
import com.chinmay.catalogue.core.data.repository.ThemeMode
import com.chinmay.catalogue.core.data.repository.ThemeRepository
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    /** Category pages by slug; any other slug is NotFound. Tests can add or replace pages. */
    val pages: MutableMap<String, CategoryPage> = mutableMapOf(TestData.electronics.slug to TestData.electronicsPage)

    override suspend fun category(slug: String): DataResult<CategoryPage> {
        val page = pages[slug]
        if (page == null && failWith == null) return DataResult.Failure(DataError.NotFound)
        return respond { page!! }
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

/** Recent searches in memory, newest first, with the same rules as the real one (no repeats, at most eight). */
class FakeRecentSearchesRepository(initial: List<String> = emptyList()) : RecentSearchesRepository {
    private val _recent = MutableStateFlow(initial)
    override val recent: StateFlow<List<String>> = _recent

    override fun add(term: String) {
        val cleaned = term.trim()
        if (cleaned.isEmpty()) return
        _recent.value = (listOf(cleaned) + _recent.value.filterNot { it.equals(cleaned, ignoreCase = true) }).take(8)
    }

    override fun clear() {
        _recent.value = emptyList()
    }
}

/** The light/dark choice in memory. */
class FakeThemeRepository(initial: ThemeMode = ThemeMode.LIGHT) : ThemeRepository {
    private val _mode = MutableStateFlow(initial)
    override val mode: StateFlow<ThemeMode> = _mode
    override fun setMode(mode: ThemeMode) {
        _mode.value = mode
    }
}
