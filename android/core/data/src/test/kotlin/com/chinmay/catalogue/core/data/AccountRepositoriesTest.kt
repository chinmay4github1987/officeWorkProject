package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.NetworkAuthRepository
import com.chinmay.catalogue.core.data.repository.NetworkCartRepository
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.User
import com.chinmay.catalogue.core.network.AuthInterceptor
import com.chinmay.catalogue.core.network.NetworkFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Login, the bearer token and session expiry, through real Retrofit + OkHttp against a local server. */
class AccountRepositoriesTest {
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

    private lateinit var server: MockWebServer
    private val session = InMemorySessionStore()
    private lateinit var auth: NetworkAuthRepository
    private lateinit var carts: NetworkCartRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        // The same interceptor the app installs, fed by the session store.
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).addInterceptor(AuthInterceptor(session)).build()
        val api = NetworkFactory.catalogueApi(server.url("/").toString(), client)
        auth = NetworkAuthRepository(api, NetworkFactory.json, session)
        carts = NetworkCartRepository(api, NetworkFactory.json, session)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(code: Int, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    @Test
    fun `login stores the session and later calls send the bearer token`() = runTest {
        respond(200, """{"token":"abc.def","expiresAt":"2026-11-28T10:00:00.000Z","user":$USER}""")
        respond(200, """{"items":[{"product":$PRODUCT,"quantity":2,"lineTotal":{"amount":218.0,"currency":"USD"}}],"itemCount":2,"total":{"amount":218.0,"currency":"USD"}}""")

        val user = (auth.login(" asha ", "secret-pass") as DataResult.Success).data
        val cart = (carts.refresh() as DataResult.Success).data

        assertEquals("Asha Rao", user.fullName)
        val login = server.takeRequest()
        assertEquals("""{"username":"asha","password":"secret-pass"}""", login.body.readUtf8())
        assertNull(login.getHeader("Authorization"))
        assertEquals("Bearer abc.def", server.takeRequest().getHeader("Authorization"))
        assertEquals(2, cart.itemCount)
        assertEquals(cart, carts.cart.value)
    }

    @Test
    fun `wrong credentials become InvalidCredentials and nothing is stored`() = runTest {
        respond(401, """{"error":{"code":"INVALID_CREDENTIALS","message":"That username and password don't match."}}""")

        assertEquals(DataResult.Failure(DataError.InvalidCredentials), auth.login("asha", "nope"))
        assertNull(auth.currentUser.value)
    }

    @Test
    fun `an expired session signs out and the cart is forgotten`() = runTest {
        session.save("old.token", USER_MODEL)
        respond(401, """{"error":{"code":"UNAUTHORIZED","message":"Please sign in to continue."}}""")

        assertEquals(DataResult.Failure(DataError.Unauthorized), carts.refresh())
        assertNull(auth.currentUser.value)
        assertNull(carts.cart.value)
    }

    @Test
    fun `signed out, cart calls fail without touching the network`() = runTest {
        assertEquals(DataResult.Failure(DataError.Unauthorized), carts.add("3"))
        assertEquals(0, server.requestCount)
    }

    private companion object {
        const val USER = """{"id":"1","username":"asha","email":"asha@example.com","firstName":"Asha","lastName":"Rao","phone":null}"""
        val USER_MODEL = User("1", "asha", "asha@example.com", "Asha", "Rao", null)
        const val PRODUCT = """{"id":"10","title":"SanDisk SSD","description":"Fast.","price":{"amount":109.0,"currency":"USD"},"category":{"slug":"electronics","name":"Electronics"},"imageUrl":"https://img.example/10.png","rating":{"average":2.9,"count":470}}"""
    }
}
