package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.map
import com.chinmay.catalogue.core.network.FakeStoreApi
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.fakeStoreCategory
import com.chinmay.catalogue.core.network.dto.Pricing
import com.chinmay.catalogue.core.network.dto.parseFakeStoreProduct
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/** Merchandising choices: the business's, not Fake Store's. (They were backend settings; same defaults.) */
internal object Merchandising {
    /** The products shown as "Top rated" on Home, in this order. Unknown IDs are skipped. */
    val featuredProductIds = listOf("1", "5", "9", "14", "18")

    /** How many "top rated" products a category page shows. */
    const val CATEGORY_TOP_RATED_COUNT = 3
}

/**
 * The catalogue, straight from https://fakestoreapi.com, with the business rules applied on the phone.
 * Fake Store has only 20 products, so the whole list is loaded once and kept for [CACHE_MILLIS]: home, categories and
 * search then work from memory, and a single request serves the whole browsing session.
 */
internal class FakeStoreCatalogueRepository(
    private val api: FakeStoreApi,
    private val json: Json,
    private val pricing: Pricing,
    private val clock: () -> Long,
) : CatalogueRepository {

    @Inject
    constructor(api: FakeStoreApi, json: Json, pricing: Pricing) : this(api, json, pricing, System::currentTimeMillis)

    private class Snapshot(val products: List<Product>, val categories: List<Category>, val loadedAt: Long)

    private val mutex = Mutex()
    private var cached: Snapshot? = null

    override suspend fun home(): DataResult<CatalogueHome> = snapshot().map { data ->
        val byId = data.products.associateBy { it.id }
        CatalogueHome(featured = Merchandising.featuredProductIds.mapNotNull { byId[it] }, categories = data.categories)
    }

    /**
     * topRated = featured products in this category first, then the best customer-rated ones, up to the configured
     * count. more = every other product in the category, in Fake Store's order.
     */
    override suspend fun category(slug: String): DataResult<CategoryPage> = snapshot().flatMap { data ->
        val category = data.categories.firstOrNull { it.slug == slug } ?: return@flatMap DataResult.Failure(DataError.NotFound)
        val inCategory = data.products.filter { it.category.slug == slug }
        val featuredIds = Merchandising.featuredProductIds
        val curated = featuredIds.mapNotNull { id -> inCategory.firstOrNull { it.id == id } }
        val byRating = inCategory
            .filter { it.id !in featuredIds }
            .sortedWith(compareByDescending<Product> { it.rating.average }.thenByDescending { it.rating.count })
        val topRated = (curated + byRating).take(maxOf(Merchandising.CATEGORY_TOP_RATED_COUNT, curated.size))
        val topIds = topRated.map { it.id }.toSet()
        DataResult.Success(CategoryPage(category, topRated, inCategory.filter { it.id !in topIds }))
    }

    /** Case- and accent-insensitive; every word must appear in the title, description or category name. */
    override suspend fun search(term: String): DataResult<List<Product>> {
        val words = normalise(term).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return DataResult.Success(emptyList())
        return snapshot().map { data ->
            data.products.filter { product ->
                val haystack = normalise("${product.title} ${product.description} ${product.category.name}")
                words.all { it in haystack }
            }
        }
    }

    /**
     * From the loaded list (one request serves a product page and a whole cart). A product missing from that list
     * (e.g. added to Fake Store since it was loaded) is fetched on its own; Fake Store's "not found" is an empty 200.
     */
    override suspend fun product(id: String): DataResult<Product> = snapshot().flatMap { data ->
        data.products.firstOrNull { it.id == id }?.let { return@flatMap DataResult.Success(it) }
        when (val result = safeApiCall { parseFakeStoreProduct(api.product(id).string(), json, pricing) }) {
            is DataResult.Success -> result.data?.let { DataResult.Success(it) } ?: DataResult.Failure(DataError.NotFound)
            is DataResult.Failure -> result
        }
    }

    private fun fresh(): Snapshot? = cached?.takeIf { clock() - it.loadedAt < CACHE_MILLIS }

    /** One load at a time: screens opening together share it instead of each asking Fake Store. */
    private suspend fun snapshot(): DataResult<Snapshot> = mutex.withLock {
        fresh()?.let { return@withLock DataResult.Success(it) }
        val result = safeApiCall {
            coroutineScope {
                val products = async { api.products() }
                val categories = async { api.categories() }
                Snapshot(
                    products = products.await().map { it.asExternalModel(pricing) },
                    categories = categories.await().map(::fakeStoreCategory),
                    loadedAt = clock(),
                )
            }
        }
        if (result is DataResult.Success) cached = result.data // failures aren't cached: the next screen retries
        result
    }

    private fun normalise(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("['’]"), "")

    private inline fun <T, R> DataResult<T>.flatMap(transform: (T) -> DataResult<R>): DataResult<R> = when (this) {
        is DataResult.Success -> transform(data)
        is DataResult.Failure -> this
    }

    companion object {
        /** Fake Store's catalogue doesn't change while someone browses; 5 minutes keeps prices reasonably fresh. */
        const val CACHE_MILLIS = 5 * 60 * 1000L
    }
}
