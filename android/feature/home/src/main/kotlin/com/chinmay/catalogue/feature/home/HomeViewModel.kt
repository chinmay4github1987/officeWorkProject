package com.chinmay.catalogue.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Each screen state is one of these; the UI renders exactly one, so impossible combinations can't happen. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val error: DataError) : HomeUiState
    data class Success(
        val home: CatalogueHome,
        /** "Top picks for you": the best-rated product of each category that isn't already in the hero. */
        val picks: List<Product> = emptyList(),
        /** Category slug -> a photo of its best product, for the category tiles. */
        val categoryImages: Map<String, String> = emptyMap(),
        val isRefreshing: Boolean = false,
    ) : HomeUiState
}

/** Who is signed in and how many items are in their cart, for the header. Separate from the catalogue state. */
data class HomeAccount(val user: User? = null, val cartCount: Int = 0)

/** A one-off message after tapping a card's cart button. */
sealed interface CartMessage {
    data class Added(val title: String) : CartMessage
    data class Failed(val error: DataError) : CartMessage
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CatalogueRepository,
    private val auth: AuthRepository,
    private val carts: CartRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _cartMessage = MutableStateFlow<CartMessage?>(null)
    val cartMessage: StateFlow<CartMessage?> = _cartMessage.asStateFlow()

    /** Signed out always shows 0, even if a cart from the last session is still cached. */
    val account: StateFlow<HomeAccount> = combine(auth.currentUser, carts.cart) { user, cart ->
        HomeAccount(user, if (user == null) 0 else cart?.itemCount ?: 0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeAccount(auth.currentUser.value))

    init {
        load()
        // Whenever someone signs in (or the app starts signed in), fetch their cart for the badge.
        viewModelScope.launch {
            auth.currentUser.collect { user -> if (user != null) carts.refresh() }
        }
    }

    fun retry() {
        _uiState.value = HomeUiState.Loading
        load()
    }

    /** Pull to refresh: keeps showing the current products while it reloads, and keeps them if it fails. */
    fun refresh() {
        val current = _uiState.value as? HomeUiState.Success ?: return retry()
        _uiState.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            when (val result = fetch()) {
                is DataResult.Success -> _uiState.value = result.data
                is DataResult.Failure -> _uiState.update { (it as? HomeUiState.Success)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    /** The card's cart button. Signed out, the screen sends the customer to sign in instead of calling this. */
    fun addToCart(product: Product) {
        viewModelScope.launch {
            _cartMessage.value = when (val result = carts.add(product.id)) {
                is DataResult.Success -> CartMessage.Added(product.title)
                is DataResult.Failure -> CartMessage.Failed(result.error)
            }
        }
    }

    fun cartMessageShown() {
        _cartMessage.value = null
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = when (val result = fetch()) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }

    /** Home, plus each category's best product (served from the repository's cached catalogue, so no extra requests). */
    private suspend fun fetch(): DataResult<HomeUiState.Success> {
        val home = when (val result = repository.home()) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return result
        }
        val featuredIds = home.featured.map { it.id }.toSet()
        val bestPerCategory = home.categories.mapNotNull { category ->
            (repository.category(category.slug) as? DataResult.Success)?.data?.topRated?.let { category.slug to it }
        }
        return DataResult.Success(
            HomeUiState.Success(
                home = home,
                picks = bestPerCategory.mapNotNull { (_, top) -> top.firstOrNull { it.id !in featuredIds } },
                categoryImages = bestPerCategory.mapNotNull { (slug, top) -> top.firstOrNull()?.let { slug to it.imageUrl } }.toMap(),
            ),
        )
    }
}
