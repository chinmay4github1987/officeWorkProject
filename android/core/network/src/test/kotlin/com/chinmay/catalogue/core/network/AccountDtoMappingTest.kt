package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.network.dto.CartDto
import com.chinmay.catalogue.core.network.dto.CartEnquiryRequestDto
import com.chinmay.catalogue.core.network.dto.EnquiryRequestDto
import com.chinmay.catalogue.core.network.dto.SessionDto
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.asNetworkModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** JSON <-> model mapping for the session, cart and enquiry DTOs, using the app's real Json settings. */
class AccountDtoMappingTest {
    private val json = NetworkFactory.json

    @Test
    fun `parses a session and maps the user, phone is optional`() {
        val body = """
            {"token":"abc","expiresAt":"2026-11-28T00:00:00.000Z",
             "user":{"id":"1","username":"johnd","email":"john@example.com","firstName":"John","lastName":"Doe"}}
        """.trimIndent()

        val session = json.decodeFromString<SessionDto>(body)
        val user = session.user.asExternalModel()

        assertEquals("abc", session.token)
        assertEquals("John Doe", user.fullName)
        assertNull(user.phone)
    }

    @Test
    fun `maps a cart, keeping the backend's totals instead of recalculating`() {
        val body = """
            {"itemCount":3,"total":{"amount":130.0,"currency":"USD"},
             "items":[{"quantity":3,"lineTotal":{"amount":130.0,"currency":"USD"},
               "product":{"id":"1","title":"Backpack","description":"d","price":{"amount":109.95,"currency":"USD"},
                 "category":{"slug":"mens-clothing","name":"Men's clothing"},"imageUrl":"https://img.example/1.png",
                 "rating":{"average":3.9,"count":120}}}]}
        """.trimIndent()

        val cart = json.decodeFromString<CartDto>(body).asExternalModel()

        assertFalse(cart.isEmpty)
        assertEquals(3, cart.itemCount)
        assertEquals(130.0, cart.total.amount, 0.0)
        val item = cart.items.single()
        assertEquals(3, item.quantity)
        assertEquals(130.0, item.lineTotal.amount, 0.0)
        assertEquals("Backpack", item.product.title)
    }

    @Test
    fun `optional enquiry fields are left out of the request JSON, not sent as null`() {
        val sent = json.encodeToString(EnquiryRequestDto.serializer(), EnquiryRequest("3", "Asha", "asha@example.com", null, null).asNetworkModel())

        assertEquals("""{"productId":"3","name":"Asha","email":"asha@example.com"}""", sent)
    }

    @Test
    fun `cart enquiry carries only the extras`() {
        val sent = json.encodeToString(CartEnquiryRequestDto.serializer(), CartEnquiryRequest(phone = "+44 1234", message = null).asNetworkModel())

        assertEquals("""{"phone":"+44 1234"}""", sent)
    }
}
