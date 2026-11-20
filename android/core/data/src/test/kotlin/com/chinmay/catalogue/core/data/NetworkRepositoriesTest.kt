package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.NetworkCatalogueRepository
import com.chinmay.catalogue.core.data.repository.NetworkEnquiryRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.network.NetworkFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Real Retrofit + real JSON parsing against a local fake server: covers the app's whole network edge. */
class NetworkRepositoriesTest {
    private lateinit var server: MockWebServer
    private lateinit var catalogue: NetworkCatalogueRepository
    private lateinit var enquiries: NetworkEnquiryRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
        val api = NetworkFactory.catalogueApi(server.url("/").toString(), client)
        catalogue = NetworkCatalogueRepository(api, NetworkFactory.json)
        enquiries = NetworkEnquiryRepository(api, NetworkFactory.json)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(code: Int, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    @Test
    fun `search sends the trimmed term and maps the products`() = runTest {
        respond(200, """{"query":"ssd","products":[$PRODUCT]}""")

        val result = catalogue.search("  ssd ")

        assertEquals("/api/v1/products?search=ssd", server.takeRequest().path)
        val products = (result as DataResult.Success).data
        assertEquals("SanDisk SSD", products.single().title)
    }

    @Test
    fun `404 becomes NotFound`() = runTest {
        respond(404, """{"error":{"code":"PRODUCT_NOT_FOUND","message":"That product could not be found."}}""")
        assertEquals(DataResult.Failure(DataError.NotFound), catalogue.product("999"))
    }

    @Test
    fun `400 with field errors becomes Validation with the server's messages`() = runTest {
        respond(400, """{"error":{"code":"VALIDATION_FAILED","message":"Some details need fixing.","fields":{"email":"Enter a valid email address"}}}""")

        val result = enquiries.submit(EnquiryRequest("3", "Asha", "bad", null, null))

        assertEquals(DataResult.Failure(DataError.Validation(mapOf("email" to "Enter a valid email address"))), result)
    }

    @Test
    fun `429 becomes RateLimited and 502 keeps the server message`() = runTest {
        respond(429, """{"error":{"code":"TOO_MANY_REQUESTS","message":"Too many enquiries."}}""")
        assertEquals(DataResult.Failure(DataError.RateLimited), enquiries.submit(EnquiryRequest("3", "A", "a@b.co", null, null)))

        respond(502, """{"error":{"code":"PRODUCT_SOURCE_UNAVAILABLE","message":"Products are unavailable right now."}}""")
        assertEquals(DataResult.Failure(DataError.Server("Products are unavailable right now.")), catalogue.home())
    }

    @Test
    fun `no connection becomes Network`() = runTest {
        server.shutdown()
        assertEquals(DataResult.Failure(DataError.Network), catalogue.home())
    }

    @Test
    fun `successful enquiry returns the receipt`() = runTest {
        respond(201, """{"reference":"ENQ-ABCD1234","receivedAt":"2026-11-20T10:00:00.000Z"}""")

        val result = enquiries.submit(EnquiryRequest("3", "Asha", "asha@example.com", null, "Hi"))

        assertEquals("ENQ-ABCD1234", (result as DataResult.Success).data.reference)
        val sent = server.takeRequest().body.readUtf8()
        assertTrue(sent.contains("\"email\":\"asha@example.com\""))
    }

    private companion object {
        const val PRODUCT = """{"id":"10","title":"SanDisk SSD","description":"Fast.","price":{"amount":109.0,"currency":"USD"},"category":{"slug":"electronics","name":"Electronics"},"imageUrl":"https://img.example/10.png","rating":{"average":2.9,"count":470}}"""
    }
}
