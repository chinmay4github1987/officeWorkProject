package com.chinmay.catalogue.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CartUiState {
    data object Loading : CartUiState
    data object SignedOut : CartUiState
    data class Error(val error: DataError) : CartUiState
    data class Ready(
        val cart: Cart,
        /** Products with a quantity change in flight: their steppers are disabled so taps can't race. */
        val updating: Set<String> = emptySet(),
        val message: String = "",
        val isSending: Boolean = false,
        /** A failed update or send. Shown as a banner; the cart stays usable. */
        val actionError: DataError? = null,
    ) : CartUiState

    data class Sent(val receipt: EnquiryReceipt, val email: String) : CartUiState
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val carts: CartRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        // Reacts to sign-in (coming back from Login) and to sign-out (including an expired session).
        viewModelScope.launch {
            auth.currentUser.collect { user ->
                if (user == null) _uiState.value = CartUiState.SignedOut else if (_uiState.value !is CartUiState.Sent) load()
            }
        }
    }

    fun retry() = load()

    fun onQuantityChange(productId: String, quantity: Int) {
        val ready = _uiState.value as? CartUiState.Ready ?: return
        if (productId in ready.updating) return
        _uiState.value = ready.copy(updating = ready.updating + productId, actionError = null)
        viewModelScope.launch {
            val result = carts.setQuantity(productId, quantity.coerceIn(0, Cart.MAX_QUANTITY))
            updateReady { current ->
                when (result) {
                    is DataResult.Success -> current.copy(cart = result.data, updating = current.updating - productId)
                    is DataResult.Failure -> current.copy(updating = current.updating - productId, actionError = result.error)
                }
            }
        }
    }

    fun onRemove(productId: String) = onQuantityChange(productId, 0)

    fun onMessageChange(message: String) = updateReady { it.copy(message = message.take(MESSAGE_MAX)) }

    fun dismissError() = updateReady { it.copy(actionError = null) }

    fun onSendEnquiry() {
        val ready = _uiState.value as? CartUiState.Ready ?: return
        if (ready.isSending || ready.cart.isEmpty || ready.updating.isNotEmpty()) return
        _uiState.value = ready.copy(isSending = true, actionError = null)
        viewModelScope.launch {
            val request = CartEnquiryRequest(phone = null, message = ready.message.trim().ifEmpty { null })
            when (val result = carts.submitEnquiry(request)) {
                is DataResult.Success -> _uiState.value = CartUiState.Sent(result.data, auth.currentUser.value?.email.orEmpty())
                is DataResult.Failure -> updateReady { it.copy(isSending = false, actionError = result.error) }
            }
        }
    }

    private fun load() {
        if (auth.currentUser.value == null) {
            _uiState.value = CartUiState.SignedOut
            return
        }
        _uiState.value = CartUiState.Loading
        viewModelScope.launch {
            when (val result = carts.refresh()) {
                is DataResult.Success -> _uiState.value = CartUiState.Ready(result.data)
                is DataResult.Failure -> _uiState.value =
                    if (result.error == DataError.Unauthorized) CartUiState.SignedOut else CartUiState.Error(result.error)
            }
        }
    }

    private inline fun updateReady(crossinline transform: (CartUiState.Ready) -> CartUiState.Ready) =
        _uiState.update { (it as? CartUiState.Ready)?.let(transform) ?: it }

    companion object {
        const val MESSAGE_MAX = 2000
    }
}
