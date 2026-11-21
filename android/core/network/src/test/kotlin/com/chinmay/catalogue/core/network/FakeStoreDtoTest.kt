package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.network.dto.FakeStoreUserDto
import com.chinmay.catalogue.core.network.dto.Pricing
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.fakeStoreCategory
import com.chinmay.catalogue.core.network.dto.fakeStoreUserIdFromToken
import com.chinmay.catalogue.core.network.dto.parseFakeStoreProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/** Fake Store's JSON and token, mapped with the app's real Json settings. */
class FakeStoreDtoTest {
    private val json = NetworkFactory.json

    @Test
    fun `category names become a URL-safe slug and a capitalised name`() {
        assertEquals(Category("mens-clothing", "Men's clothing"), fakeStoreCategory("men's clothing"))
        assertEquals(Category("electronics", "Electronics"), fakeStoreCategory(" electronics "))
        assertEquals(Category("cafe-deco", "Café & déco"), fakeStoreCategory("café & déco"))
    }

    @Test
    fun `the user ID comes from the token's sub claim, number or text`() {
        assertEquals("2", fakeStoreUserIdFromToken(jwt("""{"sub":2,"user":"mor_2314"}"""), json))
        assertEquals("7", fakeStoreUserIdFromToken(jwt("""{"sub":"7"}"""), json))
        assertNull(fakeStoreUserIdFromToken("not-a-token", json))
        assertNull(fakeStoreUserIdFromToken(jwt("""{"user":"x"}"""), json))
    }

    @Test
    fun `a user's password and address are never part of the parsed profile`() {
        val raw = """{"id":1,"email":"john@gmail.com","username":"johnd","password":"m38rmF${'$'}",
            "name":{"firstname":"john","lastname":"doe"},"address":{"city":"kilcoole"},"phone":"1-570-236-7033"}"""

        val user = json.decodeFromString(FakeStoreUserDto.serializer(), raw).asExternalModel()

        assertEquals("John Doe", user.fullName)
        assertEquals("1-570-236-7033", user.phone)
        assertEquals(false, user.toString().contains("m38rmF"))
    }

    @Test
    fun `an empty product body is "not found", a real one parses`() {
        assertNull(parseFakeStoreProduct("", json))
        assertNull(parseFakeStoreProduct("null", json))
        val product = parseFakeStoreProduct(
            """{"id":3,"title":"Mens Cotton Jacket","price":55.99,"description":"d","category":"men's clothing",
               "image":"https://fakestoreapi.com/img/3.jpg","rating":{"rate":4.7,"count":500}}""",
            json,
        )!!
        assertEquals("3", product.id)
        assertEquals(55.99, product.price.amount, 0.0)
        assertEquals("mens-clothing", product.category.slug)
    }

    @Test
    fun `dollar prices convert to rupees at the configured rate, rounded to the paisa`() {
        val inr = Pricing("INR", 85.0)
        assertEquals(Money(9345.75, "INR"), inr.fromUsd(109.95))
        assertEquals(Money(1.7, "INR"), inr.fromUsd(0.02))
        assertEquals(Money(109.95, "USD"), Pricing.USD.fromUsd(109.95))
    }

    private fun jwt(payload: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString("{}".toByteArray()) + "." + enc.encodeToString(payload.toByteArray()) + ".sig"
    }
}
