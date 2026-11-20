package com.chinmay.catalogue.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
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
    data class Success(val home: CatalogueHome, val isRefreshing: Boolean = false) : HomeUiState
}

/** Who is signed in and how many items are in their cart, for the header. Separate from the catalogue state. */
data class HomeAccount(val user: User? = null, val cartCount: Int = 0)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CatalogueRepository,
    private val auth: AuthRepository,
    private val carts: CartRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    fun signOut() = auth.logout()

    fun retry() {
        _uiState.value = HomeUiState.Loading
        load()
    }

    /** Pull to refresh: keeps showing the current products while it reloads, and keeps them if it fails. */
    fun refresh() {
        val current = _uiState.value as? HomeUiState.Success ?: return retry()
        _uiState.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            when (val result = repository.home()) {
                is DataResult.Success -> _uiState.value = HomeUiState.Success(result.data)
                is DataResult.Failure -> _uiState.update { (it as? HomeUiState.Success)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = when (val result = repository.home()) {
                is DataResult.Success -> HomeUiState.Success(result.data)
                is DataResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }
}
