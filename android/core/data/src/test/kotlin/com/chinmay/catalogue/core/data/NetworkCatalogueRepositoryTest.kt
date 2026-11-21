package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.NetworkCatalogueRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.network.NetworkFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * The catalogue endpoints' happy paths and the error cases that other tests don't cover:
 * which URL each call hits, how the JSON maps, and what broken responses turn into.
 * Real Retrofit + JSON against MockWebServer, so nothing about the network edge is faked.
 */
class NetworkCatalogueRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var catalogue: NetworkCatalogueRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()
        catalogue = NetworkCatalogueRepository(NetworkFactory.catalogueApi(server.url("/").toString(), client), NetworkFactory.json)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(code: Int, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    @Test
    fun `home GETs the home endpoint and maps featured products and categories`() = runTest {
        respond(200, """{"featured":[${product("1", "Backpack")},${product("5", "Bracelet")}],"categories":[$ELECTRONICS]}""")

        val home = (catalogue.home() as DataResult.Success).data

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/home", request.path)
        assertEquals(listOf("Backpack", "Bracelet"), home.featured.map { it.title })
        assertEquals("electronics", home.categories.single().slug)
    }

    @Test
    fun `category asks for the slug and keeps top rated and the rest apart`() = runTest {
        respond(200, """{"category":$ELECTRONICS,"topRated":[${product("9", "SSD")}],"more":[${product("10", "Monitor")},${product("11", "TV")}]}""")

        val page = (catalogue.category("electronics") as DataResult.Success).data

        assertEquals("/api/v1/categories/electronics", server.takeRequest().path)
        assertEquals("Electronics", page.category.name)
        assertEquals(listOf("9"), page.topRated.map { it.id })
        assertEquals(listOf("10", "11"), page.more.map { it.id })
    }

    @Test
    fun `product maps price, rating and image`() = runTest {
        respond(200, product("14", "Curved Monitor"))

        val product = (catalogue.product("14") as DataResult.Success).data

        assertEquals("/api/v1/products/14", server.takeRequest().path)
        assertEquals(999.99, product.price.amount, 0.0)
        assertEquals("USD", product.price.currencyCode)
        assertEquals(4.1, product.rating.average, 0.0)
        assertEquals(259, product.rating.count)
        assertEquals("https://img.example/14.png", product.imageUrl)
    }

    @Test
    fun `ids and search terms are URL-encoded, not spliced into the path`() = runTest {
        respond(200, product("x", "X"))
        respond(200, """{"products":[]}""")

        catalogue.product("../admin")
        catalogue.search("usb & ssd")

        assertEquals("/api/v1/products/..%2Fadmin", server.takeRequest().path)
        assertEquals("/api/v1/products?search=usb%20%26%20ssd", server.takeRequest().path)
    }

    @Test
    fun `an empty search result is Success with no products`() = runTest {
        respond(200, """{"query":"zzz","products":[]}""")
        assertEquals(DataResult.Success(emptyList<Any>()), catalogue.search("zzz"))
    }

    @Test
    fun `malformed JSON on a 200 becomes Server with no message`() = runTest {
        respond(200, """{"featured": [""")
        assertEquals(DataResult.Failure(DataError.Server(null)), catalogue.home())
    }

    @Test
    fun `a 200 missing a required field becomes Server with no message`() = runTest {
        respond(200, """{"id":"1","title":"No price"}""")
        assertEquals(DataResult.Failure(DataError.Server(null)), catalogue.product("1"))
    }

    @Test
    fun `a 500 with an HTML body (e g a proxy page) becomes Server with no message`() = runTest {
        respond(500, "<html><body>Bad gateway</body></html>")
        assertEquals(DataResult.Failure(DataError.Server(null)), catalogue.home())
    }

    @Test
    fun `a 400 without field errors keeps the server's message`() = runTest {
        respond(400, """{"error":{"code":"BAD_REQUEST","message":"Search term is too long."}}""")
        assertEquals(DataResult.Failure(DataError.Server("Search term is too long.")), catalogue.search("x"))
    }

    @Test
    fun `a 401 that isn't a login failure becomes Unauthorized`() = runTest {
        respond(401, """{"error":{"code":"UNAUTHORIZED","message":"Sign in again."}}""")
        assertEquals(DataResult.Failure(DataError.Unauthorized), catalogue.home())
    }

    @Test
    fun `a server that never answers times out as Network`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertEquals(DataResult.Failure(DataError.Network), catalogue.home())
    }

    private companion object {
        const val ELECTRONICS = """{"slug":"electronics","name":"Electronics"}"""

        fun product(id: String, title: String) =
            """{"id":"$id","title":"$title","description":"d","price":{"amount":999.99,"currency":"USD"},""" +
                """"category":$ELECTRONICS,"imageUrl":"https://img.example/$id.png","rating":{"average":4.1,"count":259}}"""
    }
}
