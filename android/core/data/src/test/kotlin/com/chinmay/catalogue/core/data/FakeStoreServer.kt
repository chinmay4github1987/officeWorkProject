package com.chinmay.catalogue.core.data

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A local stand-in for https://fakestoreapi.com (MockWebServer), answering by path with Fake Store's real JSON shapes.
 * Tests change [routes] to simulate errors, and read [paths] to check which requests were made.
 */
internal class FakeStoreServer : AutoCloseable {
    val server = MockWebServer()
    val paths = CopyOnWriteArrayList<String>()

    /** Path (with method, e.g. "GET /products") -> response. Anything else is a 404. */
    val routes = mutableMapOf(
        "GET /products" to json(PRODUCTS),
        "GET /products/categories" to json("""["electronics","jewelery","men's clothing","women's clothing"]"""),
        "GET /products/7" to json(product(7, "White Gold Plated Princess", 9.99, "jewelery", 3.0, 400)),
        // Fake Store's "not found": 200 with an empty body.
        "GET /products/999" to MockResponse().setResponseCode(200),
        "POST /auth/login" to json("""{"token":"${token(userId = 2)}"}"""),
        "GET /users/2" to json(
            """{"id":2,"email":"morrison@gmail.com","username":"mor_2314","password":"83r5^_",
               "name":{"firstname":"david","lastname":"morrison"},"phone":"1-570-236-7033",
               "address":{"city":"kilcoole","street":"Lovers Ln","number":7267}}""",
        ),
        // Formspree (same local server in tests): the form accepts the submission.
        "POST /f/testform1" to json("""{"ok":true,"next":"https://formspree.io/thanks"}"""),
        "GET /carts/user/2" to json(
            """[{"id":3,"userId":2,"date":"2020-01-01T00:00:00.000Z","products":[{"productId":9,"quantity":1}]},
               {"id":4,"userId":2,"date":"2020-03-01T00:00:00.000Z","products":[{"productId":1,"quantity":4},{"productId":2,"quantity":1},{"productId":1,"quantity":2}]}]""",
        ),
    )

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val key = "${request.method} ${request.path}"
                paths += key
                return routes[key] ?: MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    val baseUrl: String get() = server.url("/").toString()

    override fun close() = server.shutdown()

    companion object {
        fun json(body: String) = MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)

        fun product(id: Int, title: String, price: Double, category: String, rate: Double, count: Int) =
            """{"id":$id,"title":"$title","price":$price,"description":"$title description","category":"$category",""" +
                """"image":"https://img.example/$id.png","rating":{"rate":$rate,"count":$count}}"""

        /** A JWT shaped like Fake Store's: {"sub": userId, "user": ..., "iat": ...}. The signature isn't checked. */
        fun token(userId: Int): String {
            val enc = Base64.getUrlEncoder().withoutPadding()
            val header = enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
            val payload = enc.encodeToString("""{"sub":$userId,"user":"mor_2314","iat":1700000000}""".toByteArray())
            return "$header.$payload.signature"
        }

        /** Ten products across four categories, with featured IDs 1, 5, 9, 14 and 18 partly present (18 is missing). */
        val PRODUCTS = listOf(
            product(1, "Fjallraven - Foldsack No. 1 Backpack", 109.95, "men's clothing", 3.9, 120),
            product(2, "Mens Casual Premium Slim Fit T-Shirts", 22.3, "men's clothing", 4.1, 259),
            product(3, "Mens Cotton Jacket", 55.99, "men's clothing", 4.7, 500),
            product(4, "Mens Casual Slim Fit", 15.99, "men's clothing", 2.1, 430),
            product(5, "John Hardy Women's Legends Naga Bracelet", 695.0, "jewelery", 4.6, 400),
            product(6, "Solid Gold Petite Micropave", 168.0, "jewelery", 3.9, 70),
            product(9, "WD 2TB Elements Portable Hard Drive", 64.0, "electronics", 3.3, 203),
            product(10, "SanDisk SSD PLUS 1TB", 109.0, "electronics", 2.9, 470),
            product(14, "Samsung 49-Inch Curved Gaming Monitor", 999.99, "electronics", 2.2, 140),
            product(20, "Café Racer Short Sleeve Moisture Tee", 12.99, "women's clothing", 3.6, 145),
        ).joinToString(prefix = "[", postfix = "]", separator = ",")
    }
}
