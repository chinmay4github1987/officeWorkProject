package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.ENQUIRIES_NOT_SET_UP
import com.chinmay.catalogue.core.data.repository.FakeStoreCatalogueRepository
import com.chinmay.catalogue.core.data.repository.FormspreeClient
import com.chinmay.catalogue.core.data.repository.FormspreeEnquiryRepository
import com.chinmay.catalogue.core.data.repository.SharedPreferencesRecentSearchesRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.network.FormspreeConfig
import com.chinmay.catalogue.core.network.NetworkFactory
import com.chinmay.catalogue.core.network.dto.Pricing
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Product enquiries through real Retrofit + JSON to a local server answering like Formspree (and Fake Store). */
class FormspreeEnquiryRepositoryTest {
    private val server = FakeStoreServer()
    private val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()
    private val catalogue = FakeStoreCatalogueRepository(NetworkFactory.fakeStoreApi(server.baseUrl, client), NetworkFactory.json, Pricing.USD) { 0L }

    private fun repository(formId: String = "testform1") = FormspreeEnquiryRepository(
        FormspreeClient(NetworkFactory.formspreeApi(server.baseUrl, client), FormspreeConfig(formId), NetworkFactory.json),
        catalogue,
    ) { Instant.parse("2026-11-21T10:00:00.123456Z") }

    @After
    fun tearDown() = server.close()

    private val request = EnquiryRequest(
        productId = "3",
        name = "Asha Patel",
        email = "asha@example.com",
        phone = "+44 7700 900123",
        message = "Is this available in large?",
        company = "ABC Technologies",
    )

    private fun sentFields(): Map<String, String> {
        val sent = generateSequence { server.server.takeRequest(0, TimeUnit.SECONDS) }.first { it.path == "/f/testform1" }
        assertEquals("POST", sent.method)
        assertEquals("application/json", sent.getHeader("Accept"))
        return NetworkFactory.json.parseToJsonElement(sent.body.readUtf8()).jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    @Test
    fun `sends the enquiry with the product attached and the spec's subject`() = runTest {
        val receipt = (repository().submit(request) as DataResult.Success).data

        assertTrue(receipt.reference.matches(Regex("ENQ-[0-9A-F]{8}")))
        assertEquals("2026-11-21T10:00:00.123Z", receipt.receivedAt)
        val fields = sentFields()
        assertEquals("New Product Enquiry – Mens Cotton Jacket", fields["_subject"])
        assertEquals("3", fields["productId"])
        assertEquals("Mens Cotton Jacket", fields["productName"])
        assertEquals("Men's clothing", fields["productCategory"])
        assertEquals("https://fakestoreapi.com/products/3", fields["productUrl"])
        assertEquals("Asha Patel", fields["name"])
        assertEquals("asha@example.com", fields["email"])
        assertEquals("+44 7700 900123", fields["phone"])
        assertEquals("ABC Technologies", fields["company"])
        assertEquals("Is this available in large?", fields["message"])
        assertEquals("2026-11-21T10:00:00.123Z", fields["submittedAt"])
        assertEquals(receipt.reference, fields["reference"])
    }

    @Test
    fun `success needs Formspree's confirmation`() = runTest {
        server.routes["POST /f/testform1"] = FakeStoreServer.json("""{"ok":false}""")
        assertEquals(DataResult.Failure(DataError.Server(null)), repository().submit(request))
    }

    @Test
    fun `Formspree's field errors come back per field, so the form can show them`() = runTest {
        server.routes["POST /f/testform1"] = MockResponse().setResponseCode(422)
            .setBody("""{"error":"Validation errors","errors":[{"code":"TYPE_EMAIL","field":"email","message":"should be an email"}]}""")

        assertEquals(DataResult.Failure(DataError.Validation(mapOf("email" to "should be an email"))), repository().submit(request))
    }

    @Test
    fun `a form-level rejection keeps Formspree's message`() = runTest {
        server.routes["POST /f/testform1"] = MockResponse().setResponseCode(422)
            .setBody("""{"error":"Form not found","errors":[{"code":"FORM_NOT_FOUND","message":"Form not found"}]}""")

        assertEquals(DataResult.Failure(DataError.Server("Form not found")), repository().submit(request))
    }

    @Test
    fun `server errors, too many requests and no connection become typed errors`() = runTest {
        server.routes["POST /f/testform1"] = MockResponse().setResponseCode(503)
        assertEquals(DataResult.Failure(DataError.Server(null)), repository().submit(request))

        server.routes["POST /f/testform1"] = MockResponse().setResponseCode(429)
        assertEquals(DataResult.Failure(DataError.RateLimited), repository().submit(request))

        server.routes["POST /f/testform1"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        assertEquals(DataResult.Failure(DataError.Network), repository().submit(request))
    }

    @Test
    fun `without a form ID nothing is sent and the customer gets a clear message`() = runTest {
        assertEquals(DataResult.Failure(DataError.Server(ENQUIRIES_NOT_SET_UP)), repository(formId = "").submit(request))
        assertTrue(server.paths.none { it.startsWith("POST /f/") })
    }

    @Test
    fun `recent searches put the newest first, drop repeats in any case and keep eight`() {
        var list = emptyList<String>()
        listOf("ssd", "jacket", "  SSD ", "", "ring").forEach { list = SharedPreferencesRecentSearchesRepository.withSearch(list, it) }
        assertEquals(listOf("ring", "SSD", "jacket"), list)

        (1..10).forEach { list = SharedPreferencesRecentSearchesRepository.withSearch(list, "term $it") }
        assertEquals(8, list.size)
        assertEquals("term 10", list.first())
    }
}
