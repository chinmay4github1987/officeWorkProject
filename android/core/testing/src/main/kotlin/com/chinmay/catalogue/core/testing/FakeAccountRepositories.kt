package com.chinmay.catalogue.core.testing

import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.CartItem
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Accepts [TestData.user] with [TestData.PASSWORD]. Use [signIn] to start a test signed in. */
class FakeAuthRepository : AuthRepository {
    var failWith: DataError? = null
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun signIn(user: User = TestData.user) {
        _currentUser.value = user
    }

    override suspend fun login(username: String, password: String): DataResult<User> {
        failWith?.let { return DataResult.Failure(it) }
        if (username.trim() != TestData.user.username || password != TestData.PASSWORD) {
            return DataResult.Failure(DataError.InvalidCredentials)
        }
        signIn()
        return DataResult.Success(TestData.user)
    }

    override fun logout() {
        _currentUser.value = null
    }
}

/**
 * A cart over [TestData.products] that behaves like the backend: prices from the product list,
 * quantities 1–99, and Unauthorized when [auth] has nobody signed in.
 */
class FakeCartRepository(private val auth: FakeAuthRepository) : CartRepository {
    var failWith: DataError? = null
    val enquiries = mutableListOf<CartEnquiryRequest>()
    private val quantities = linkedMapOf<String, Int>()
    private val _cart = MutableStateFlow<Cart?>(null)
    override val cart: StateFlow<Cart?> = _cart.asStateFlow()

    override suspend fun refresh() = respond {}

    override suspend fun add(productId: String, quantity: Int) = respond {
        quantities[productId] = ((quantities[productId] ?: 0) + quantity).coerceAtMost(Cart.MAX_QUANTITY)
    }

    override suspend fun setQuantity(productId: String, quantity: Int) = respond {
        if (quantity <= 0) {
            quantities.remove(productId)
        } else {
            quantities[productId] = quantity.coerceAtMost(Cart.MAX_QUANTITY)
        }
    }

    override suspend fun remove(productId: String) = respond { quantities.remove(productId) }

    override suspend fun submitEnquiry(request: CartEnquiryRequest): DataResult<EnquiryReceipt> {
        failWith?.let { return DataResult.Failure(it) }
        if (auth.currentUser.value == null) return DataResult.Failure(DataError.Unauthorized)
        if (quantities.isEmpty()) return DataResult.Failure(DataError.Server("Your cart is empty. Add a product first."))
        enquiries += request
        quantities.clear()
        _cart.value = build()
        return DataResult.Success(EnquiryReceipt(reference = "ENQ-CART0001", receivedAt = "2026-11-21T10:00:00.000Z"))
    }

    private fun respond(change: () -> Unit): DataResult<Cart> {
        failWith?.let { return DataResult.Failure(it) }
        if (auth.currentUser.value == null) return DataResult.Failure(DataError.Unauthorized)
        change()
        return DataResult.Success(build().also { _cart.value = it })
    }

    private fun build(): Cart {
        val items = quantities.mapNotNull { (id, quantity) ->
            TestData.products.firstOrNull { it.id == id }?.let {
                CartItem(it, quantity, Money(it.price.amount * quantity, it.price.currencyCode))
            }
        }
        return Cart(items, items.sumOf { it.quantity }, Money(items.sumOf { it.lineTotal.amount }, "USD"))
    }
}
