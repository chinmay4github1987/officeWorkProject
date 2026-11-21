package com.chinmay.catalogue.feature.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductUiState {
    data object Loading : ProductUiState
    data class Error(val error: DataError) : ProductUiState
    data class Success(
        val product: Product,
        val isAddingToCart: Boolean = false,
        /** "Customers also viewed": up to [MAX_RELATED] other products from the same category. */
        val related: List<Product> = emptyList(),
    ) : ProductUiState {
        /** The description as short feature bullets (Fake Store has no separate feature list). */
        val features: List<String> get() = featuresOf(product.description)
    }

    companion object {
        const val MAX_RELATED = 6

        /**
         * Splits a description into sentence-sized bullets: on ". ", "! ", "? " and ";". A description that is one
         * long sentence stays one bullet. Empty pieces are dropped, at most 6 bullets.
         */
        fun featuresOf(description: String): List<String> = description
            .split(Regex("(?<=[.!?])\\s+|;\\s*|\\r?\\n+"))
            .map { it.trim().trimEnd('.', ';', '!').trim() }
            .filter { it.length > 2 }
            .take(6)
    }
}

/** One-off things that happen once (a snackbar, a navigation), not state the screen keeps showing. */
sealed interface ProductEvent {
    data object AddedToCart : ProductEvent
    data object SignInRequired : ProductEvent
    data class AddToCartFailed(val error: DataError) : ProductEvent
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
    private val auth: AuthRepository,
    private val carts: CartRepository,
) : ViewModel() {

    val productId: String = checkNotNull(savedStateHandle["productId"]) { "ProductDestination.productId is required" }

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Loading)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProductEvent>(Channel.BUFFERED)
    val events: Flow<ProductEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.value = ProductUiState.Loading
        load()
    }

    /** Signed out: asks the screen to open sign-in (the cart belongs to an account). */
    fun addToCart() {
        val state = _uiState.value as? ProductUiState.Success ?: return
        if (state.isAddingToCart) return
        if (auth.currentUser.value == null) {
            _events.trySend(ProductEvent.SignInRequired)
            return
        }
        _uiState.value = state.copy(isAddingToCart = true)
        viewModelScope.launch {
            val result = carts.add(productId)
            _uiState.update { (it as? ProductUiState.Success)?.copy(isAddingToCart = false) ?: it }
            _events.send(
                when {
                    result is DataResult.Success -> ProductEvent.AddedToCart
                    (result as DataResult.Failure).error == DataError.Unauthorized -> ProductEvent.SignInRequired
                    else -> ProductEvent.AddToCartFailed(result.error)
                },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            val product = when (val result = repository.product(productId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _uiState.value = ProductUiState.Error(result.error)
                    return@launch
                }
            }
            _uiState.value = ProductUiState.Success(product)
            // Related products come from the same category (cached catalogue); the page shows without waiting for them.
            val page = (repository.category(product.category.slug) as? DataResult.Success)?.data ?: return@launch
            val related = (page.topRated + page.more).filter { it.id != product.id }.distinctBy { it.id }.take(ProductUiState.MAX_RELATED)
            _uiState.update { (it as? ProductUiState.Success)?.copy(related = related) ?: it }
        }
    }
}
