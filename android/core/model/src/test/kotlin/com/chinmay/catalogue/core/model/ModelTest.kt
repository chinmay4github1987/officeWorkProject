package com.chinmay.catalogue.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain JUnit tests for the small amount of logic in the domain model. */
class ModelTest {

    @Test
    fun `full name joins first and last name`() {
        assertEquals("John Doe", user(first = "John", last = "Doe").fullName)
    }

    @Test
    fun `full name drops the gap when one part is missing`() {
        assertEquals("John", user(first = "John", last = "").fullName)
        assertEquals("Doe", user(first = "", last = "Doe").fullName)
    }

    @Test
    fun `full name falls back to the username when both parts are blank`() {
        assertEquals("johnd", user(first = "", last = "").fullName)
    }

    @Test
    fun `a cart with no items is empty`() {
        assertTrue(Cart(items = emptyList(), itemCount = 0, total = Money(0.0, "USD")).isEmpty)
    }

    @Test
    fun `a cart with an item is not empty`() {
        val cart = Cart(items = listOf(CartItem(PRODUCT, quantity = 2, lineTotal = Money(20.0, "USD"))), itemCount = 2, total = Money(20.0, "USD"))
        assertFalse(cart.isEmpty)
    }

    @Test
    fun `map transforms the data of a Success`() {
        val result: DataResult<Int> = DataResult.Success(21)
        assertEquals(DataResult.Success(42), result.map { it * 2 })
    }

    @Test
    fun `map passes a Failure through untouched and never calls the transform`() {
        val failure: DataResult<Int> = DataResult.Failure(DataError.NotFound)
        var called = false

        val mapped = failure.map { called = true; it * 2 }

        assertSame(failure, mapped)
        assertFalse(called)
    }

    private fun user(first: String, last: String) =
        User(id = "1", username = "johnd", email = "john@example.com", firstName = first, lastName = last, phone = null)

    private companion object {
        val PRODUCT = Product(
            id = "1",
            title = "Backpack",
            description = "d",
            price = Money(10.0, "USD"),
            category = Category("mens-clothing", "Men's clothing"),
            imageUrl = "https://img.example/1.png",
            rating = Rating(3.9, 120),
        )
    }
}
