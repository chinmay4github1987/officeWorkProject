package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.data.session.authenticated
import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.network.CatalogueApi
import com.chinmay.catalogue.core.network.dto.AddCartItemDto
import com.chinmay.catalogue.core.network.dto.CartDto
import com.chinmay.catalogue.core.network.dto.CartQuantityDto
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.asNetworkModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * The signed-in customer's cart, kept on our backend. Every call needs a session; signed out, calls fail with
 * DataError.Unauthorized. Every successful call updates [cart], so badges and screens stay in step.
 */
interface CartRepository {
    /** The last cart the backend returned, or null before the first load. Pair with AuthRepository.currentUser. */
    val cart: StateFlow<Cart?>

    suspend fun refresh(): DataResult<Cart>
    suspend fun add(productId: String, quantity: Int = 1): DataResult<Cart>

    /** 0 removes the product. */
    suspend fun setQuantity(productId: String, quantity: Int): DataResult<Cart>
    suspend fun remove(productId: String): DataResult<Cart>

    /** Emails the sales team about the whole cart; the backend empties the cart once it's sent. */
    suspend fun submitEnquiry(request: CartEnquiryRequest): DataResult<EnquiryReceipt>
}

internal class NetworkCartRepository @Inject constructor(
    private val api: CatalogueApi,
    private val json: Json,
    private val session: SessionStore,
) : CartRepository {

    private val _cart = MutableStateFlow<Cart?>(null)
    override val cart: StateFlow<Cart?> = _cart.asStateFlow()

    override suspend fun refresh(): DataResult<Cart> = update { api.cart() }

    override suspend fun add(productId: String, quantity: Int): DataResult<Cart> = update { api.addToCart(AddCartItemDto(productId, quantity)) }

    override suspend fun setQuantity(productId: String, quantity: Int): DataResult<Cart> =
        if (quantity <= 0) remove(productId) else update { api.setCartQuantity(productId, CartQuantityDto(quantity)) }

    override suspend fun remove(productId: String): DataResult<Cart> = update { api.removeFromCart(productId) }

    override suspend fun submitEnquiry(request: CartEnquiryRequest): DataResult<EnquiryReceipt> {
        val result = session.authenticated(json) { api.submitCartEnquiry(request.asNetworkModel()).asExternalModel() }
        if (result is DataResult.Success) {
            _cart.value = _cart.value?.let { Cart(items = emptyList(), itemCount = 0, total = it.total.copy(amount = 0.0)) }
        }
        return result
    }

    private suspend fun update(call: suspend () -> CartDto): DataResult<Cart> {
        val result = session.authenticated(json) { call().asExternalModel() }
        when (result) {
            is DataResult.Success -> _cart.value = result.data
            is DataResult.Failure -> if (session.user.value == null) _cart.value = null
        }
        return result
    }
}
