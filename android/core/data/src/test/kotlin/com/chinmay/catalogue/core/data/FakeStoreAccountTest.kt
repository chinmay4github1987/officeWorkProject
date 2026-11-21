package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.ENQUIRIES_NOT_SET_UP
import com.chinmay.catalogue.core.data.repository.FormspreeClient
import com.chinmay.catalogue.core.data.repository.FakeStoreAuthRepository
import com.chinmay.catalogue.core.data.repository.FakeStoreCatalogueRepository
import com.chinmay.catalogue.core.data.repository.LocalCartRepository
import com.chinmay.catalogue.core.data.session.CartLineStore
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.data.session.SharedPreferencesCartLineStore
import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.User
import com.chinmay.catalogue.core.network.NetworkFactory
import com.chinmay.catalogue.core.network.dto.Pricing
import com.chinmay.catalogue.core.network.FormspreeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Sign-in and the on-phone cart, through real Retrofit + JSON against a local server answering like Fake Store. */
class FakeStoreAccountTest {
    private class InMemorySessionStore : SessionStore {
        private var token: String? = null
        private val _user = MutableStateFlow<User?>(null)
        override val user: StateFlow<User?> = _user
        override fun token() = token
        override fun save(token: String, user: User) {
            this.token = token
            _user.value = user
        }
        override fun clear() {
            token = null
            _user.value = null
        }
    }

    private class InMemoryCartLineStore : CartLineStore {
        // Stored in the real text format, so the encoding is tested too.
        val raw = mutableMapOf<String, String>()
        override fun load(userId: String) = raw[userId]?.let { SharedPreferencesCartLineStore.decode(it) }
        override fun save(userId: String, lines: Map<String, Int>) {
            raw[userId] = SharedPreferencesCartLineStore.encode(lines)
        }
    }

    private val fakeStore = FakeStoreServer()
    private val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
    private val api = NetworkFactory.fakeStoreApi(fakeStore.baseUrl, client)
    private val session = InMemorySessionStore()
    private val lines = InMemoryCartLineStore()
    private val catalogue = FakeStoreCatalogueRepository(api, NetworkFactory.json, Pricing.USD) { 0L }
    private val auth = FakeStoreAuthRepository(api, NetworkFactory.json, session)

    private fun carts(formId: String = FORM) = LocalCartRepository(
        api, catalogue, session, lines,
        FormspreeClient(NetworkFactory.formspreeApi(fakeStore.baseUrl, client), FormspreeConfig(formId), NetworkFactory.json),
        Pricing.USD,
    ) { Instant.parse("2026-11-21T10:00:00.123456Z") }

    @After
    fun tearDown() = fakeStore.close()

    private fun <T> DataResult<T>.data(): T = (this as DataResult.Success).data

    @Test
    fun `login reads the user ID from Fake Store's token and loads only that user`() = runTest {
        val user = auth.login("  mor_2314 ", "83r5^_").data()

        assertEquals(User("2", "mor_2314", "morrison@gmail.com", "David", "Morrison", "1-570-236-7033"), user)
        assertEquals(user, session.user.value)
        assertEquals(listOf("POST /auth/login", "GET /users/2"), fakeStore.paths)
        val sent = fakeStore.server.takeRequest().body.readUtf8()
        assertEquals("""{"username":"mor_2314","password":"83r5^_"}""", sent)
    }

    @Test
    fun `wrong credentials are InvalidCredentials and nothing is saved`() = runTest {
        fakeStore.routes["POST /auth/login"] = MockResponse().setResponseCode(401).setBody("username or password is incorrect")

        assertEquals(DataResult.Failure(DataError.InvalidCredentials), auth.login("mor_2314", "nope"))
        assertNull(session.user.value)
    }

    @Test
    fun `Fake Store being down is not reported as a wrong password`() = runTest {
        fakeStore.routes["POST /auth/login"] = MockResponse().setResponseCode(503)
        assertEquals(DataResult.Failure(DataError.Server(null)), auth.login("mor_2314", "83r5^_"))
    }

    @Test
    fun `signed out, cart calls fail without any request`() = runTest {
        assertEquals(DataResult.Failure(DataError.Unauthorized), carts().refresh())
        assertTrue(fakeStore.paths.isEmpty())
    }

    @Test
    fun `the first cart is the user's newest Fake Store cart, priced from the live catalogue`() = runTest {
        auth.login("mor_2314", "83r5^_")

        val cart = carts().refresh().data()

        // Newest cart (March): product 1 twice (4 + 2 merged) and product 2 once.
        assertEquals(listOf("1" to 6, "2" to 1), cart.items.map { it.product.id to it.quantity })
        assertEquals(659.7, cart.items[0].lineTotal.amount, 0.0) // 6 × 109.95
        assertEquals(682.0, cart.total.amount, 0.0) // 659.70 + 22.30
        assertEquals(7, cart.itemCount)
        assertEquals("1:6,2:1", lines.raw["2"])
    }

    @Test
    fun `changes are kept on the phone, so Fake Store's cart is only read once`() = runTest {
        auth.login("mor_2314", "83r5^_")
        val repository = carts()
        repository.refresh()

        repository.add("3", 2)
        repository.setQuantity("1", 150) // capped at the maximum
        val cart = repository.remove("2").data()

        assertEquals(listOf("1" to Cart.MAX_QUANTITY, "3" to 2), cart.items.map { it.product.id to it.quantity })
        assertEquals(cart, repository.cart.value)

        // A new app session (same phone storage) carries on from here without asking Fake Store again.
        val later = carts().refresh().data()
        assertEquals(cart.items.map { it.product.id }, later.items.map { it.product.id })
        assertEquals(1, fakeStore.paths.count { it == "GET /carts/user/2" })
    }

    @Test
    fun `adding a product Fake Store doesn't have fails and leaves the cart alone`() = runTest {
        auth.login("mor_2314", "83r5^_")
        val repository = carts()
        repository.refresh()

        assertEquals(DataResult.Failure(DataError.NotFound), repository.add("999"))
        assertEquals("1:6,2:1", lines.raw["2"])
    }

    @Test
    fun `without a Formspree form a cart enquiry fails clearly and keeps the cart`() = runTest {
        auth.login("mor_2314", "83r5^_")
        val repository = carts(formId = "")
        repository.refresh()

        assertEquals(DataResult.Failure(DataError.Server(ENQUIRIES_NOT_SET_UP)), repository.submitEnquiry(CartEnquiryRequest(null, null)))
        assertEquals("1:6,2:1", lines.raw["2"])
    }

    @Test
    fun `a failed cart enquiry keeps the cart, so the customer can retry`() = runTest {
        fakeStore.routes["POST /f/$FORM"] = MockResponse().setResponseCode(500)
        auth.login("mor_2314", "83r5^_")
        val repository = carts()
        repository.refresh()

        assertEquals(DataResult.Failure(DataError.Server(null)), repository.submitEnquiry(CartEnquiryRequest(null, "Hi")))
        assertEquals("1:6,2:1", lines.raw["2"])
    }

    @Test
    fun `a cart enquiry sends every item and the total to Formspree, then empties the cart`() = runTest {
        auth.login("mor_2314", "83r5^_")
        val repository = carts()
        repository.refresh()

        val receipt = repository.submitEnquiry(CartEnquiryRequest(phone = null, message = "Can you deliver on Friday?")).data()

        assertTrue(receipt.reference.matches(Regex("ENQ-[0-9A-F]{8}")))
        assertEquals("2026-11-21T10:00:00.123Z", receipt.receivedAt)
        val sent = fakeStore.server.requestSequence().first { it.path == "/f/$FORM" }
        assertEquals("application/json", sent.getHeader("Accept"))
        val fields = NetworkFactory.json.parseToJsonElement(sent.body.readUtf8()).jsonObject.mapValues { it.value.jsonPrimitive.content }
        assertEquals("New Cart Enquiry – 7 items from David Morrison", fields["_subject"])
        assertEquals("morrison@gmail.com", fields["email"]) // Formspree's reply-to
        assertEquals(
            "6 × Fjallraven - Foldsack No. 1 Backpack (ID 1): USD 659.70\n1 × Mens Casual Premium Slim Fit T-Shirts (ID 2): USD 22.30",
            fields["items"],
        )
        assertEquals("USD 682.00", fields["total"])
        assertEquals("1-570-236-7033", fields["phone"]) // from the profile, as none was typed
        assertEquals("Can you deliver on Friday?", fields["message"])
        assertFalse(fields.values.any { "83r5^_" in it }) // the password never goes anywhere

        assertTrue(repository.cart.value!!.isEmpty)
        assertEquals("", lines.raw["2"])
    }

    /** Every request the server has received so far, in order. */
    private fun okhttp3.mockwebserver.MockWebServer.requestSequence() =
        generateSequence { takeRequest(0, TimeUnit.SECONDS) }

    private companion object {
        const val FORM = "testform1"
    }
}
