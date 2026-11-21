package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.FakeStoreCatalogueRepository
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.network.NetworkFactory
import com.chinmay.catalogue.core.network.dto.Pricing
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** The catalogue rules on top of real Retrofit + JSON, against a local server answering like fakestoreapi.com. */
class FakeStoreCatalogueRepositoryTest {
    private val fakeStore = FakeStoreServer()
    private var now = 0L
    private val repository = FakeStoreCatalogueRepository(
        api = NetworkFactory.fakeStoreApi(fakeStore.baseUrl, OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()),
        json = NetworkFactory.json,
        pricing = Pricing.USD,
        clock = { now },
    )

    @After
    fun tearDown() = fakeStore.close()

    private fun <T> DataResult<T>.data(): T = (this as DataResult.Success).data

    @Test
    fun `home shows the featured products in the chosen order, skipping IDs Fake Store doesn't have`() = runTest {
        val home = repository.home().data()

        assertEquals(listOf("1", "5", "9", "14"), home.featured.map { it.id }) // 18 isn't in the catalogue
        assertEquals(
            listOf(
                Category("electronics", "Electronics"),
                Category("jewelery", "Jewelery"),
                Category("mens-clothing", "Men's clothing"),
                Category("womens-clothing", "Women's clothing"),
            ),
            home.categories,
        )
    }

    @Test
    fun `maps Fake Store's product JSON to the app's product`() = runTest {
        val backpack = repository.home().data().featured.first()

        assertEquals("Fjallraven - Foldsack No. 1 Backpack", backpack.title)
        assertEquals(109.95, backpack.price.amount, 0.0)
        assertEquals("USD", backpack.price.currencyCode)
        assertEquals("mens-clothing", backpack.category.slug)
        assertEquals("https://img.example/1.png", backpack.imageUrl)
        assertEquals(3.9, backpack.rating.average, 0.0)
        assertEquals(120, backpack.rating.count)
    }

    @Test
    fun `one load serves home, categories, search and product pages until the cache expires`() = runTest {
        repository.home()
        repository.category("jewelery")
        repository.search("ssd")
        repository.product("3")
        assertEquals(listOf("GET /products", "GET /products/categories"), fakeStore.paths.sorted())

        now += FakeStoreCatalogueRepository.CACHE_MILLIS
        repository.home()
        assertEquals(4, fakeStore.paths.size)
    }

    @Test
    fun `category top rated puts featured products first, then the best rated, three in all`() = runTest {
        val page = repository.category("mens-clothing").data()

        // Product 1 is featured; then by rating: 3 (4.7) and 2 (4.1). 4 (2.1) is in "more".
        assertEquals(listOf("1", "3", "2"), page.topRated.map { it.id })
        assertEquals(listOf("4"), page.more.map { it.id })
        assertEquals("Men's clothing", page.category.name)
    }

    @Test
    fun `an unknown category is NotFound`() = runTest {
        assertEquals(DataResult.Failure(DataError.NotFound), repository.category("toys"))
    }

    @Test
    fun `search needs every word, ignoring case, accents and apostrophes, across title, description and category`() = runTest {
        assertEquals(listOf("3"), repository.search("mens JACKET").data().map { it.id }) // "men's clothing" + "Jacket"
        assertEquals(listOf("20"), repository.search("cafe").data().map { it.id }) // "Café"
        assertEquals(listOf("9", "10", "14"), repository.search("electronics").data().map { it.id })
        assertTrue(repository.search("unicorn").data().isEmpty())
    }

    @Test
    fun `a blank search returns nothing without a request`() = runTest {
        assertTrue(repository.search("   ").data().isEmpty())
        assertTrue(fakeStore.paths.isEmpty())
    }

    @Test
    fun `a product page uses the loaded list, so a whole cart costs one request`() = runTest {
        listOf("1", "2", "3", "5").forEach { assertEquals(it, repository.product(it).data().id) }
        assertEquals(listOf("GET /products", "GET /products/categories"), fakeStore.paths.sorted())
    }

    @Test
    fun `a product missing from the list is fetched on its own, and Fake Store's empty answer is NotFound`() = runTest {
        assertEquals("White Gold Plated Princess", repository.product("7").data().title)
        assertTrue("GET /products/7" in fakeStore.paths)

        assertEquals(DataResult.Failure(DataError.NotFound), repository.product("999"))
    }

    @Test
    fun `server errors, broken JSON and no connection become typed errors, and failures aren't cached`() = runTest {
        fakeStore.routes["GET /products"] = MockResponse().setResponseCode(500)
        assertEquals(DataResult.Failure(DataError.Server(null)), repository.home())

        fakeStore.routes["GET /products"] = FakeStoreServer.json("""[{"id":1,"title":""")
        assertEquals(DataResult.Failure(DataError.Server(null)), repository.home())

        fakeStore.routes["GET /products"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        assertEquals(DataResult.Failure(DataError.Network), repository.home())

        fakeStore.routes["GET /products"] = FakeStoreServer.json(FakeStoreServer.PRODUCTS)
        assertEquals(4, repository.home().data().featured.size) // recovers on the next try
    }

    @Test
    fun `with rupee pricing every product price is converted from Fake Store's dollars`() = runTest {
        val rupees = FakeStoreCatalogueRepository(
            api = NetworkFactory.fakeStoreApi(fakeStore.baseUrl, OkHttpClient()),
            json = NetworkFactory.json,
            pricing = Pricing("INR", 85.0),
            clock = { now },
        )

        val backpack = rupees.product("1").data()

        assertEquals("INR", backpack.price.currencyCode)
        assertEquals(9345.75, backpack.price.amount, 0.0) // 109.95 × 85
    }
}
